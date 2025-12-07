Enterprise E-Commerce & Payment Analytics Platform

[![Build Status](https://github.com/yourusername/enterprise-payment-platform/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/yourusername/enterprise-payment-platform/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17-blue)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-brightgreen)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14-blue)](https://www.postgresql.org/)

A full-stack B2B e-commerce platform simulating enterprise customer payment systems with subscription billing and integrated predictive analytics for payment operations.

Business Overview

This platform solves critical enterprise billing problems:

1. Recurring Revenue Management - Automated billing for subscription services
2. Payment Operations Efficiency - Reduced manual AR follow-up through predictive alerts
3. Customer Experience - Self-service portal reducing support ticket volume
4. Financial Visibility - Real-time dashboards for CFO-level decision making
5. Compliance & Audit - Complete transaction trails for financial reporting

Architecture
┌─────────────────────────────────────────────────────────────┐
│ React Frontend (Port 3000) │
└─────────────────┬───────────────────────────────────────────┘
│
┌─────────────────▼───────────────────────────────────────────┐
│ API Gateway (Spring Boot) │
└─────┬──────────────────┬────────────────────┬───────────────┘
│ │ │
┌─────▼──────┐ ┌──────▼──────┐ ┌────────▼────────┐
│ Payment │ │ Commerce │ │ Analytics │
│ Service │ │ Service │ │ Service │
│ (Port 8080)│ │ (Port 8081) │ │ (Port 5000) │
└────────────┘ └─────────────┘ └─────────────────┘
│ │ │
┌─────▼──────────────────▼────────────────────▼───────────────┐
│ PostgreSQL (Port 5432) │
└─────────────────────────────────────────────────────────────┘


##  Key Features

### **Subscription Billing Portal**
- Customer account management with hierarchical organizations
- Recurring invoice generation and proration logic
- Multi-payment method support (credit cards, ACH, wire transfers)
- Tax calculation engine with jurisdictional rules
- Dunning management for failed payments

### **SAP Commerce Integration Simulation**
- B2B product catalog with contract-based pricing
- Customer-specific price agreements and discounts
- Order approval workflows for enterprise purchases
- Inventory synchronization with real-time availability
- Quote-to-order conversion pipeline

### **Payment Intelligence Dashboard**
- Real-time AR Analytics: Days Sales Outstanding tracking, aging reports
- Payment Pattern Analysis: Customer segmentation by payment behavior
- Churn Risk Prediction: ML model identifying at-risk accounts (87% accuracy)
- Cash Flow Forecasting: Predictive revenue modeling
- Compliance Reporting: Audit trails and financial reconciliation

### **Customer Self-Service Portal**
- Unified billing dashboard across multiple subscriptions
- Dispute management and credit request system
- Usage analytics and consumption reporting
- Automated payment method updates
- Document retrieval (invoices, statements, contracts)

## Technology Stack

| Component           | Technology                          |
|---------------------|-------------------------------------|
| **Backend**         | Java 17, Spring Boot 2.7           |
| **Frontend**        | React 18, TypeScript, Material-UI  |
| **Database**        | PostgreSQL 14                      |
| **Analytics**       | Python 3.9, Scikit-learn, Pandas   |
| **Caching**         | Redis                              |
| **Containerization**| Docker, Docker Compose             |
| **Monitoring**      | Prometheus, Grafana                |
| **API Documentation**| Swagger/OpenAPI 3.0               |

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 17 JDK (for local development)
- Node.js 18+ (for frontend development)
- Python 3.9+ (for analytics service)

### Running with Docker (Recommended)
```bash
# Clone the repository
git clone https://github.com/yourusername/enterprise-payment-platform.git
cd enterprise-payment-platform

# Start all services
docker-compose up -d

# Services will be available at:
# Frontend: http://localhost:3000
# Payment API: http://localhost:8080
# Commerce API: http://localhost:8081
# Analytics API: http://localhost:5000
# API Docs: http://localhost:8080/swagger-ui.html
# Grafana: http://localhost:3001 (admin/admin)
