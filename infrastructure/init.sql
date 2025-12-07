-- Create database and user
CREATE DATABASE payment_db;
CREATE USER payment_user WITH ENCRYPTED PASSWORD 'securepass123';
GRANT ALL PRIVILEGES ON DATABASE payment_db TO payment_user;

-- Connect to payment_db and create tables
\c payment_db;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    company_name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50),
    duns_number VARCHAR(50),
    credit_limit DECIMAL(15,2) DEFAULT 0,
    payment_terms INT DEFAULT 30,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    risk_score DECIMAL(5,2) DEFAULT 0.5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_name)
);

-- Contacts table
CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(100),
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products table (for commerce simulation)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sku VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    unit_price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2),
    tax_rate DECIMAL(5,2) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Price agreements (customer-specific pricing)
CREATE TABLE price_agreements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    agreed_price DECIMAL(10,2) NOT NULL,
    minimum_quantity INT DEFAULT 1,
    valid_from DATE NOT NULL,
    valid_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(customer_id, product_id)
);

-- Subscriptions table
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    subscription_number VARCHAR(100) UNIQUE NOT NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    plan_code VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    billing_cycle VARCHAR(50) NOT NULL, -- MONTHLY, QUARTERLY, ANNUAL
    billing_day INT, -- Day of month for billing
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    start_date DATE NOT NULL,
    end_date DATE,
    auto_renew BOOLEAN DEFAULT true,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Invoices table
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    subscription_id UUID REFERENCES subscriptions(id),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    tax DECIMAL(15,2) DEFAULT 0,
    total DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'ISSUED',
    payment_terms INT DEFAULT 30,
    notes TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Invoice line items
CREATE TABLE invoice_line_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_id UUID REFERENCES invoices(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id),
    description VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    tax_rate DECIMAL(5,2) DEFAULT 0,
    amount DECIMAL(15,2) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payments table
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    payment_reference VARCHAR(100) UNIQUE NOT NULL,
    invoice_id UUID REFERENCES invoices(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    gateway_response JSONB,
    failure_reason TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment methods
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- CREDIT_CARD, ACH, WIRE
    token VARCHAR(255), -- Gateway token
    last_four VARCHAR(4),
    expiry_date VARCHAR(7), -- MM/YYYY
    bank_name VARCHAR(100),
    is_default BOOLEAN DEFAULT false,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Dunning (collection) actions
CREATE TABLE dunning_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES invoices(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL, -- EMAIL, CALL, LETTER
    action_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    notes TEXT,
    result VARCHAR(255),
    next_action_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Credit memos
CREATE TABLE credit_memos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    memo_number VARCHAR(100) UNIQUE NOT NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES invoices(id),
    amount DECIMAL(15,2) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ISSUED',
    applied_to_invoice UUID REFERENCES invoices(id),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Analytics tables
CREATE TABLE analytics_mrr (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    period DATE NOT NULL,
    mrr_amount DECIMAL(15,2) NOT NULL,
    new_mrr DECIMAL(15,2) DEFAULT 0,
    expansion_mrr DECIMAL(15,2) DEFAULT 0,
    churned_mrr DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(period)
);

CREATE TABLE analytics_churn (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    period DATE NOT NULL,
    churned_customers INT DEFAULT 0,
    total_customers INT NOT NULL,
    churn_rate DECIMAL(5,2) NOT NULL,
    revenue_churn DECIMAL(15,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(period)
);

-- Create indexes for performance
CREATE INDEX idx_invoices_customer_status ON invoices(customer_id, status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_payments_customer_date ON payments(customer_id, payment_date);
CREATE INDEX idx_subscriptions_customer_status ON subscriptions(customer_id, status);
CREATE INDEX idx_customers_risk_score ON customers(risk_score DESC);

-- Create views for reporting
CREATE VIEW vw_ar_aging AS
SELECT 
    c.id as customer_id,
    c.company_name,
    SUM(CASE 
        WHEN i.due_date >= CURRENT_DATE THEN i.total
        ELSE 0 
    END) as current,
    SUM(CASE 
        WHEN i.due_date BETWEEN CURRENT_DATE - INTERVAL '30 days' AND CURRENT_DATE - INTERVAL '1 day' THEN i.total
        ELSE 0 
    END) as days_1_30,
    SUM(CASE 
        WHEN i.due_date BETWEEN CURRENT_DATE - INTERVAL '60 days' AND CURRENT_DATE - INTERVAL '31 days' THEN i.total
        ELSE 0 
    END) as days_31_60,
    SUM(CASE 
        WHEN i.due_date BETWEEN CURRENT_DATE - INTERVAL '90 days' AND CURRENT_DATE - INTERVAL '61 days' THEN i.total
        ELSE 0 
    END) as days_61_90,
    SUM(CASE 
        WHEN i.due_date < CURRENT_DATE - INTERVAL '90 days' THEN i.total
        ELSE 0 
    END) as over_90
FROM invoices i
JOIN customers c ON i.customer_id = c.id
WHERE i.status IN ('ISSUED', 'PARTIALLY_PAID')
GROUP BY c.id, c.company_name;

CREATE VIEW vw_customer_payment_trends AS
SELECT 
    c.id as customer_id,
    c.company_name,
    COUNT(p.id) as total_payments,
    AVG(p.amount) as avg_payment_amount,
    AVG(EXTRACT(EPOCH FROM (p.payment_date::date - i.due_date))/86400) as avg_days_to_pay,
    STDDEV(EXTRACT(EPOCH FROM (p.payment_date::date - i.due_date))/86400) as payment_timing_variability,
    SUM(CASE WHEN p.payment_date::date <= i.due_date THEN 1 ELSE 0 END) * 100.0 / COUNT(p.id) as on_time_percentage
FROM customers c
LEFT JOIN payments p ON c.id = p.customer_id AND p.status = 'COMPLETED'
LEFT JOIN invoices i ON p.invoice_id = i.id
GROUP BY c.id, c.company_name;

-- Insert sample data
INSERT INTO customers (id, company_name, tax_id, credit_limit, payment_terms, risk_score) VALUES
(uuid_generate_v4(), 'Acme Corporation', '12-3456789', 100000.00, 30, 0.3),
(uuid_generate_v4(), 'Globex Corporation', '98-7654321', 50000.00, 15, 0.7),
(uuid_generate_v4(), 'Stark Industries', '45-6789012', 250000.00, 45, 0.2),
(uuid_generate_v4(), 'Wayne Enterprises', '23-4567890', 150000.00, 30, 0.4);

INSERT INTO products (id, sku, name, description, unit_price, category) VALUES
(uuid_generate_v4(), 'PROD-001', 'Enterprise License', 'Annual enterprise software license', 25000.00, 'Software'),
(uuid_generate_v4(), 'PROD-002', 'Cloud Storage 1TB', 'Monthly cloud storage subscription', 500.00, 'Cloud'),
(uuid_generate_v4(), 'PROD-003', 'API Calls Package', 'Package of 1M API calls', 1000.00, 'API'),
(uuid_generate_v4(), 'PROD-004', 'Premium Support', '24/7 premium technical support', 2000.00, 'Services');
