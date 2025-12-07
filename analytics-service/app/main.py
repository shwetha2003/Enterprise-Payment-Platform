from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from typing import List, Dict, Optional
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler
import psycopg2
import redis
import json

from app.models import PaymentPredictionRequest, CustomerRiskScore
from app.services import PaymentPredictor, RiskAnalyzer

app = FastAPI(title="Payment Analytics Service", version="1.0.0")

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Security
security = HTTPBearer()

# Database connection
def get_db_connection():
    return psycopg2.connect(
        host="postgres",
        database="payment_db",
        user="admin",
        password="securepass123"
    )

# Redis connection
redis_client = redis.Redis(host='redis', port=6379, decode_responses=True)

# Initialize ML models
payment_predictor = PaymentPredictor()
risk_analyzer = RiskAnalyzer()

@app.get("/health")
async def health_check():
    return {"status": "healthy", "timestamp": datetime.now().isoformat()}

@app.post("/predict/payment-delay")
async def predict_payment_delay(request: PaymentPredictionRequest):
    """
    Predict likelihood of payment delay for a customer
    """
    try:
        # Get customer payment history from database
        conn = get_db_connection()
        query = """
        SELECT amount, payment_date, due_date, status 
        FROM payments 
        WHERE customer_id = %s
        ORDER BY payment_date DESC
        LIMIT 100
        """
        
        df = pd.read_sql_query(query, conn, params=(request.customer_id,))
        conn.close()
        
        if df.empty:
            return {"customer_id": request.customer_id, "risk_score": 0.5, "confidence": 0.0}
        
        # Calculate features
        features = calculate_payment_features(df)
        
        # Predict using ML model
        prediction = payment_predictor.predict(features)
        
        # Cache result
        cache_key = f"payment_risk:{request.customer_id}"
        redis_client.setex(
            cache_key,
            3600,  # 1 hour TTL
            json.dumps({
                "risk_score": float(prediction['risk_score']),
                "confidence": float(prediction['confidence']),
                "features": features
            })
        )
        
        return prediction
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/analytics/churn-risk")
async def get_churn_risk(customer_id: str):
    """
    Calculate churn risk score for a customer
    """
    cache_key = f"churn_risk:{customer_id}"
    cached = redis_client.get(cache_key)
    
    if cached:
        return json.loads(cached)
    
    try:
        conn = get_db_connection()
        
        # Get customer data
        customer_query = """
        SELECT c.*, 
               COUNT(p.id) as total_payments,
               AVG(EXTRACT(EPOCH FROM (p.payment_date - i.due_date))/86400) as avg_days_late,
               MAX(p.payment_date) as last_payment_date
        FROM customers c
        LEFT JOIN payments p ON c.id = p.customer_id
        LEFT JOIN invoices i ON p.invoice_id = i.id
        WHERE c.id = %s
        GROUP BY c.id
        """
        
        df = pd.read_sql_query(customer_query, conn, params=(customer_id,))
        conn.close()
        
        if df.empty:
            raise HTTPException(status_code=404, detail="Customer not found")
        
        # Calculate churn risk
        risk_score = risk_analyzer.calculate_churn_risk(df.iloc[0])
        
        result = {
            "customer_id": customer_id,
            "churn_risk_score": risk_score,
            "risk_level": "HIGH" if risk_score > 0.7 else "MEDIUM" if risk_score > 0.4 else "LOW",
            "factors": risk_analyzer.get_risk_factors(df.iloc[0])
        }
        
        # Cache result
        redis_client.setex(cache_key, 1800, json.dumps(result))
        
        return result
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/analytics/customer-segmentation")
async def get_customer_segmentation():
    """
    Segment customers based on payment behavior
    """
    try:
        conn = get_db_connection()
        
        query = """
        SELECT 
            c.id as customer_id,
            c.company_name,
            COUNT(p.id) as payment_count,
            AVG(p.amount) as avg_payment_amount,
            AVG(EXTRACT(EPOCH FROM (p.payment_date - i.due_date))/86400) as avg_days_to_pay,
            STDDEV(EXTRACT(EPOCH FROM (p.payment_date - i.due_date))/86400) as payment_timing_variability
        FROM customers c
        LEFT JOIN payments p ON c.id = p.customer_id
        LEFT JOIN invoices i ON p.invoice_id = i.id
        WHERE p.status = 'COMPLETED'
        GROUP BY c.id, c.company_name
        """
        
        df = pd.read_sql_query(query, conn)
        conn.close()
        
        # Perform clustering
        segments = perform_customer_segmentation(df)
        
        return {
            "segments": segments,
            "total_customers": len(df),
            "analysis_date": datetime.now().isoformat()
        }
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/analytics/cash-flow-forecast")
async def get_cash_flow_forecast(days: int = 30):
    """
    Forecast cash flow for next N days
    """
    try:
        conn = get_db_connection()
        
        # Get upcoming invoices
        query = """
        SELECT 
            i.due_date,
            SUM(i.total - COALESCE(SUM(p.amount), 0)) as expected_amount
        FROM invoices i
        LEFT JOIN payments p ON i.id = p.invoice_id AND p.status = 'COMPLETED'
        WHERE i.status IN ('ISSUED', 'PARTIALLY_PAID')
          AND i.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '%s days'
        GROUP BY i.due_date
        ORDER BY i.due_date
        """
        
        df = pd.read_sql_query(query, conn, params=(days,))
        conn.close()
        
        # Generate forecast
        forecast = generate_cash_flow_forecast(df, days)
        
        return forecast
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

def calculate_payment_features(df: pd.DataFrame) -> Dict:
    """Calculate features from payment history"""
    if df.empty:
        return {}
    
    features = {
        "payment_count": len(df),
        "avg_amount": float(df['amount'].mean()),
        "payment_frequency": calculate_payment_frequency(df),
        "late_payment_ratio": calculate_late_payment_ratio(df),
        "payment_amount_variability": float(df['amount'].std()),
        "recent_payment_trend": calculate_recent_trend(df)
    }
    
    return features

def calculate_payment_frequency(df: pd.DataFrame) -> float:
    """Calculate average days between payments"""
    if len(df) < 2:
        return 0
    
    df['payment_date'] = pd.to_datetime(df['payment_date'])
    df = df.sort_values('payment_date')
    intervals = (df['payment_date'].diff().dt.days).dropna()
    
    return float(intervals.mean()) if not intervals.empty else 0

def calculate_late_payment_ratio(df: pd.DataFrame) -> float:
    """Calculate ratio of late payments"""
    if df.empty:
        return 0
    
    # Assuming we have due_date and payment_date
    late_payments = df[df['payment_date'] > df['due_date']]
    
    return len(late_payments) / len(df)

def calculate_recent_trend(df: pd.DataFrame) -> float:
    """Calculate trend in payment timing (improving or worsening)"""
    if len(df) < 3:
        return 0
    
    df = df.sort_values('payment_date')
    df['days_late'] = (df['payment_date'] - df['due_date']).dt.days
    
    # Linear regression slope
    x = np.arange(len(df))
    y = df['days_late'].values
    
    if len(y) > 1:
        slope = np.polyfit(x, y, 1)[0]
        return float(slope)
    
    return 0

def perform_customer_segmentation(df: pd.DataFrame) -> List[Dict]:
    """Segment customers using K-means clustering"""
    from sklearn.cluster import KMeans
    from sklearn.preprocessing import StandardScaler
    
    # Prepare features
    feature_cols = ['payment_count', 'avg_payment_amount', 'avg_days_to_pay', 'payment_timing_variability']
    X = df[feature_cols].fillna(0)
    
    # Scale features
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # Apply K-means
    kmeans = KMeans(n_clusters=4, random_state=42)
    df['segment'] = kmeans.fit_predict(X_scaled)
    
    # Map segments to meaningful names
    segment_names = {
        0: "High-Value Punctual",
        1: "Medium-Value Variable",
        2: "Low-Value Late",
        3: "New/Infrequent"
    }
    
    df['segment_name'] = df['segment'].map(segment_names)
    
    # Return segment statistics
    segments = []
    for segment_id in df['segment'].unique():
        segment_data = df[df['segment'] == segment_id]
        segments.append({
            "segment_id": int(segment_id),
            "segment_name": segment_names.get(segment_id, "Unknown"),
            "customer_count": int(len(segment_data)),
            "avg_payment_amount": float(segment_data['avg_payment_amount'].mean()),
            "avg_days_to_pay": float(segment_data['avg_days_to_pay'].mean()),
            "characteristics": describe_segment_characteristics(segment_data)
        })
    
    return segments

def describe_segment_characteristics(segment_df: pd.DataFrame) -> Dict:
    """Generate human-readable description of segment characteristics"""
    return {
        "payment_behavior": "Punctual" if segment_df['avg_days_to_pay'].mean() < 0 else "Usually Late",
        "value_tier": "High" if segment_df['avg_payment_amount'].mean() > 10000 else 
                     "Medium" if segment_df['avg_payment_amount'].mean() > 1000 else "Low",
        "consistency": "Consistent" if segment_df['payment_timing_variability'].mean() < 5 else "Variable"
    }

def generate_cash_flow_forecast(df: pd.DataFrame, days: int) -> Dict:
    """Generate cash flow forecast"""
    if df.empty:
        return {"forecast": [], "total_expected": 0, "confidence_interval": {}}
    
    # Create daily forecast
    forecast_dates = pd.date_range(start=datetime.now().date(), periods=days, freq='D')
    forecast = []
    
    total_expected = 0
    for date in forecast_dates:
        day_amount = df[df['due_date'] == date.date()]['expected_amount'].sum()
        total_expected += float(day_amount) if not pd.isna(day_amount) else 0
        
        forecast.append({
            "date": date.isoformat(),
            "expected_amount": float(day_amount) if not pd.isna(day_amount) else 0,
            "cumulative_amount": total_expected
        })
    
    # Calculate confidence interval (simplified)
    avg_daily = total_expected / days if days > 0 else 0
    std_dev = df['expected_amount'].std() if len(df) > 1 else 0
    
    return {
        "forecast": forecast,
        "total_expected": total_expected,
        "confidence_interval": {
            "lower_bound": total_expected - (1.96 * std_dev),
            "upper_bound": total_expected + (1.96 * std_dev),
            "confidence_level": 0.95
        },
        "key_dates": get_key_forecast_dates(df)
    }

def get_key_forecast_dates(df: pd.DataFrame) -> List[Dict]:
    """Identify key dates in forecast with large payments"""
    key_dates = []
    
    for _, row in df.nlargest(5, 'expected_amount').iterrows():
        key_dates.append({
            "date": row['due_date'].isoformat() if hasattr(row['due_date'], 'isoformat') else str(row['due_date']),
            "amount": float(row['expected_amount']),
            "significance": "Major" if row['expected_amount'] > 10000 else "Moderate"
        })
    
    return key_dates

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
