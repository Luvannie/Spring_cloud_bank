# Modern Microservices Banking System - Architecture Specification

**Version**: 1.0.0  
**Last Updated**: 2026-04-08  
**Domain**: Fintech - Financial Transaction Processing  
**Availability Target**: 99.99% (Four Nines)  
**Consistency Model**: ACID with Saga Pattern for distributed transactions

---

## Table of Contents

1. [System Architecture Map](#1-system-architecture-map)
2. [Service Decomposition](#2-service-decomposition)
3. [Database Schema Design](#3-database-schema-design)
4. [Integration Logic: PayOS Payment Gateway](#4-integration-logic-payos-payment-gateway)
5. [Resiliency Strategy: Resilience4j](#5-resiliency-strategy-resilience4j)
6. [Sample Code Structure](#6-sample-code-structure)

---

## 1. System Architecture Map

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              EXTERNAL SYSTEMS                                    │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐      │
│  │   PayOS     │    │   Keycloak │    │    ELK      │    │  Prometheus │      │
│  │  Payment    │    │     IAM     │    │   Stack     │    │  + Grafana  │      │
│  │   Gateway   │    │             │    │             │    │             │      │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘      │
└─────────┼──────────────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │                  │
          ▼                  ▼                  ▼                  ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY (Spring Cloud Gateway)                  │
│                    ┌─────────────────────────────────────────┐                   │
│                    │ • JWT Validation                        │                   │
│                    │ • Rate Limiting (1000 req/min)          │                   │
│                    │ • Request/Response Logging              │                   │
│                    │ • RBAC Enforcement                       │                   │
│                    └─────────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           SERVICE MESH (Kafka Event Bus)                         │
│  ┌─────────────────────────────────────────────────────────────────────────┐     │
│  │                           KAFKA CLUSTER                                   │     │
│  │  Topics: account.events | transaction.events | payment.events | notify   │     │
│  └─────────────────────────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│ Auth Service│ │Account Svc  │ │Transaction  │ │  Payment    │ │Notification │
│             │ │             │ │   Service   │ │   Service   │ │   Service   │
│ • Login     │ │ • Balance   │ │ • Transfer  │ │ • PayOS     │ │ • Email     │
│ • OAuth2    │ │ • Freeze    │ │ • Saga      │ │ • Webhooks  │ │ • SMS       │
│ • JWT Mint  │ │ • Statements│ │ • History   │ │ • QR Codes  │ │ • Push      │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
     │               │               │               │               │
     ▼               ▼               ▼               ▼               ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
│    Redis    │ │ PostgreSQL  │ │ PostgreSQL  │ │ PostgreSQL  │ │   Redis     │
│   (Sessions)│ │  (Accounts) │ │(Transactions)│ │  (Payments) │ │   (Queue)   │
└─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘
```

### 1.2 Money Transfer Transaction Flow (Bank A → Bank B)

#### Flow Diagram: Inter-Bank Transfer using Saga Pattern (Orchestration)

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         SAGA ORCHESTRATOR: TransferSaga                            │
│                    (Implemented in Transaction Service)                             │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ Step 1: Initiate Transfer
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ CLIENT APPLICATION                                                                  │
│ POST /api/v1/transfers                                                              │
│ {                                                                                    │
│   "sourceAccountId": "ACC-001",                                                     │
│   "targetAccountId": "ACC-002",  // External bank account                           │
│   "amount": 1000000,  // VND                                                        │
│   "currency": "VND",                                                                │
│   "description": "Payment for order #12345"                                         │
│ }                                                                                    │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ STEP 1: TRANSACTION SERVICE - Create Pending Transaction                            │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │ KAFKA Event: transaction.initiated                                              │ │
│ │ {                                                                                │ │
│ │   "transactionId": "TXN-UUID",                                                  │ │
│ │   "sourceAccountId": "ACC-001",                                                │ │
│ │   "targetAccountId": "ACC-002",                                                │ │
│ │   "amount": 1000000,                                                           │ │
│ │   "status": "PENDING",                                                         │ │
│ │   "sagaId": "SAGA-UUID",                                                       │ │
│ │   "timestamp": "2026-04-08T10:30:00Z"                                          │ │
│ │ }                                                                                │ │
│ └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
┌────────────────────────────────────┐  ┌────────────────────────────────────┐
│   STEP 2a: ACCOUNT SERVICE         │  │   STEP 2b: PAYMENT SERVICE        │
│   (Debit Source Account)           │  │   (Generate PayOS Payment Link)    │
│                                    │  │                                    │
│ • Validate source account exists   │  │ • Create payment link via PayOS    │
│ • Check sufficient balance          │  │ • Return QR code / payment URL     │
│ • Reserve amount (ACID lock)        │  │ • Wait for payment confirmation    │
│                                    │  │                                    │
│ Compensation: CREDIT back           │  │ Compensation: Cancel payment link  │
└────────────────────────────────────┘  └────────────────────────────────────┘
                    │                               │
                    └───────────────┬───────────────┘
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ STEP 3: PAYMENT SERVICE - Process PayOS Payment                                    │
│                                                                                      │
│ • PayOS generates QR code / payment URL                                             │
│ • Customer completes payment within 15 minutes                                      │
│ • PayOS sends webhook to /api/v1/payments/webhook                                  │
│                                                                                      │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │ KAFKA Event: payment.completed                                                   │ │
│ │ {                                                                                │ │
│ │   "paymentId": "PAY-UUID",                                                      │ │
│ │   "transactionId": "TXN-UUID",                                                  │ │
│ │   "status": "SUCCESS",                                                          │ │
│ │   "payosTransactionId": "PS-123456",                                           │ │
│ │   "paidAt": "2026-04-08T10:32:00Z"                                              │ │
│ │ }                                                                                │ │
│ └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ STEP 4: TRANSACTION SERVICE - Confirm & Complete Transfer                          │
│                                                                                      │
│ • Update transaction status to COMPLETED                                             │
│ • Finalize debit from source account                                                │
│ • Trigger settlement event for target account (external bank)                        │
│                                                                                      │
│ Compensation: Refund to source account if final step fails                          │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ STEP 5: NOTIFICATION SERVICE - Send Notifications                                   │
│                                                                                      │
│ • Email/SMS to source account holder: "You transferred 1,000,000 VND to ACC-002"   │
│ • Email/SMS to target account holder: "You received 1,000,000 VND from ACC-001"     │
│                                                                                      │
│ ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│ │ KAFKA Event: notification.requested                                              │ │
│ │ {                                                                                │ │
│ │   "notificationId": "NOTIF-UUID",                                               │ │
│ │   "type": ["EMAIL", "SMS"],                                                     │ │
│ │   "recipients": ["sender@example.com", "receiver@example.com"],                │ │
│ │   "template": "transfer_confirmation",                                           │ │
│ │   "data": { ... }                                                               │ │
│ │ }                                                                                │ │
│ └─────────────────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ SAGA COMPLETED SUCCESSFULLY                                                         │
│                                                                                      │
│ Final State:                                                                          │
│ • Transaction: COMPLETED                                                            │
│ • Source Account: Debited 1,000,000 VND                                             │
│ • Target Account: Credited (via external settlement)                                │
│ • Payment: Confirmed via PayOS                                                      │
│ • Notifications: Sent                                                               │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.3 Saga Compensation Flow (Failure Scenario)

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                    SAGA COMPENSATION: Handling Payment Failure                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ PayOS Webhook: PAYMENT_FAILED
                                    │ OR Timeout after 15 minutes
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ COMPENSATING ACTION 1: PAYMENT SERVICE                                              │
│ • Mark payment as FAILED                                                            │
│ • Release any PayOS resources (cancel QR if not paid)                               │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ COMPENSATING ACTION 2: ACCOUNT SERVICE                                              │
│ • CREDIT back reserved amount to source account                                     │
│ • Release any balance locks                                                         │
│ • Log compensation action with sagaId                                               │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ SAGA ROLLED BACK                                                                     │
│                                                                                      │
│ Final State:                                                                          │
│ • Transaction: FAILED                                                              │
│ • Source Account: UNCHANGED (reserved amount released)                             │
│ • Target Account: UNCHANGED                                                        │
│ • Payment: CANCELLED                                                               │
│ • Notification: "Your payment link has expired"                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Service Decomposition

### 2.1 Core Services Overview

| Service | Responsibility | Data Owner | Tech Stack |
|---------|---------------|------------|------------|
| **Auth Service** | Identity, JWT issuance, OAuth2 | Users, Roles, Sessions | Spring Security, Keycloak Client |
| **Account Service** | Account CRUD, Balance, Freeze | Accounts, Statements | Spring Boot, JPA, PostgreSQL |
| **Transaction Service** | Transfers, Saga Orchestration | Transactions, Ledger | Spring Boot, Kafka |
| **Payment Service** | PayOS Integration, QR Codes | Payments, PayOS Refs | Spring Boot, WebClient |
| **Notification Service** | Email, SMS, Push Notifications | Notification Log | Spring Boot, Mail, SMS Client |

### 2.2 Auth Service

**Port**: 8081  
**Base Path**: `/api/v1/auth`

```yaml
Responsibilities:
  - User authentication (username/password, OAuth2)
  - JWT token issuance and validation
  - Session management (Redis-backed)
  - Role-Based Access Control (RBAC)
  - Keycloak integration for enterprise SSO

APIs:
  POST   /api/v1/auth/login              - Authenticate user, return JWT
  POST   /api/v1/auth/refresh            - Refresh expired JWT
  POST   /api/v1/auth/logout             - Invalidate session
  GET    /api/v1/auth/me                 - Get current user info
  POST   /api/v1/auth/register           - Register new user
  PUT    /api/v1/auth/password           - Change password

Data Model:
  - User: id, username, email, password_hash, status, created_at
  - Role: id, name, description
  - UserRole: user_id, role_id
  - Session: session_id, user_id, jwt_token, expires_at, created_at

Security:
  - JWT expiry: 15 minutes (access), 7 days (refresh)
  - Rate limit: 5 failed attempts / 15 minutes → lock account
  - Password: BCrypt with cost 12
```

### 2.3 Account Service

**Port**: 8082  
**Base Path**: `/api/v1/accounts`

```yaml
Responsibilities:
  - Account creation, closure
  - Balance inquiries and updates
  - Account freeze/unfreeze
  - Account statements and history
  - Balance reservations (for Saga)

APIs:
  POST   /api/v1/accounts                 - Create new account
  GET    /api/v1/accounts/{id}           - Get account details
  GET    /api/v1/accounts/{id}/balance   - Get current balance
  GET    /api/v1/accounts/{id}/statements- Get account statements
  PUT    /api/v1/accounts/{id}/freeze    - Freeze account
  PUT    /api/v1/accounts/{id}/unfreeze  - Unfreeze account
  POST   /api/v1/accounts/{id}/reserve   - Reserve balance (Saga)
  POST   /api/v1/accounts/{id}/commit     - Commit reserved amount (Saga)
  POST   /api/v1/accounts/{id}/rollback  - Rollback reserved amount (Saga)

Data Model:
  - Account: id, user_id, account_number, account_type, balance, 
             reserved_balance, status, currency, created_at, updated_at
  - Statement: id, account_id, transaction_id, type, amount, 
               balance_after, description, created_at

Business Rules:
  - Balance >= 0 always (no overdraft)
  - Reserved balance <= available balance
  - Account status: ACTIVE, FROZEN, CLOSED
  - Account types: CHECKING, SAVINGS, BUSINESS
```

### 2.4 Transaction Service

**Port**: 8083  
**Base Path**: `/api/v1/transactions`

```yaml
Responsibilities:
  - Initiate money transfers (internal & external)
  - Saga orchestration for distributed transactions
  - Transaction history and search
  - Idempotency handling

APIs:
  POST   /api/v1/transfers                - Initiate transfer (Saga starts here)
  GET    /api/v1/transactions/{id}       - Get transaction details
  GET    /api/v1/transactions            - List transactions (paginated)
  GET    /api/v1/transactions/{id}/status- Get transaction status

Kafka Topics Consumed:
  - payment.events        - Listen for payment confirmations
  - notification.events   - Trigger notifications

Kafka Topics Produced:
  - transaction.events    - Publish transaction state changes
  - account.events        - Trigger account operations

Saga Orchestration:
  - TransferSaga: Orchestrates inter-bank transfers
  - Steps: [Reserve, CreatePayment, ConfirmPayment, Settle, Notify]
  - Compensation: Reverse each step on failure

Data Model:
  - Transaction: id, saga_id, source_account_id, target_account_id,
                 amount, currency, status, type, description,
                 payos_payment_id, created_at, updated_at
  - SagaState: id, saga_id, current_step, status, compensating_action,
               retry_count, last_error, created_at, updated_at

Idempotency:
  - Idempotency key in header: X-Idempotency-Key
  - Stored in Redis with 24h TTL
  - Duplicate requests return cached response
```

### 2.5 Payment Service

**Port**: 8084  
**Base Path**: `/api/v1/payments`

```yaml
Responsibilities:
  - PayOS API integration
  - Payment link generation (QR codes, URLs)
  - Webhook handling for payment status
  - Payment reconciliation

APIs:
  POST   /api/v1/payments/link            - Generate PayOS payment link
  GET    /api/v1/payments/{id}           - Get payment details
  POST   /api/v1/payments/webhook        - PayOS webhook endpoint
  GET    /api/v1/payments/{id}/status    - Check payment status
  POST   /api/v1/payments/{id}/cancel    - Cancel payment link

Webhook Events Handled:
  - PAYMENT_SUCCESS   → Publish payment.completed event
  - PAYMENT_FAILED    → Publish payment.failed event
  - PAYMENT_EXPIRED   → Publish payment.expired event

Data Model:
  - Payment: id, transaction_id, payos_order_id, payos_transaction_id,
             amount, status, qr_code_url, payment_url, 
             expires_at, paid_at, created_at, updated_at

PayOS Integration:
  - API Version: v2
  - Webhook signature validation using HMAC-SHA256
  - Retry webhook delivery: 3 times with exponential backoff
  - Idempotent webhook processing using payos_transaction_id
```

### 2.6 Notification Service

**Port**: 8085  
**Base Path**: `/api/v1/notifications`

```yaml
Responsibilities:
  - Send email notifications
  - Send SMS notifications
  - Send push notifications (Firebase)
  - Notification templating
  - Notification preferences

APIs:
  POST   /api/v1/notifications/send      - Send notification
  GET    /api/v1/notifications/{id}       - Get notification status
  GET    /api/v1/notifications            - List user notifications
  PUT    /api/v1/notifications/preferences- Update preferences

Kafka Topics Consumed:
  - notification.events   - Process notification requests

Notification Types:
  - EMAIL    - Via SMTP (Spring Mail)
  - SMS      - Via Twilio/VNG SMS Gateway
  - PUSH     - Via Firebase Cloud Messaging
  - IN_APP   - Stored in database, shown in app

Data Model:
  - Notification: id, user_id, type, channel, recipient,
                  subject, body, status, sent_at, created_at
  - NotificationPreference: id, user_id, type, channel, enabled
```

---

## 3. Database Schema Design

### 3.1 PostgreSQL Schema: Accounts & Transactions

```sql
-- ============================================================================
-- BANKING SYSTEM DATABASE SCHEMA
-- PostgreSQL 15+ with ACID compliance
-- ============================================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE account_status AS ENUM ('ACTIVE', 'FROZEN', 'CLOSED');
CREATE TYPE account_type AS ENUM ('CHECKING', 'SAVINGS', 'BUSINESS');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED');
CREATE TYPE transaction_type AS ENUM ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'PAYMENT', 'REFUND');
CREATE TYPE payment_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED', 'EXPIRED', 'CANCELLED');
CREATE TYPE currency_code AS ENUM ('VND', 'USD', 'EUR');

-- ============================================================================
-- ACCOUNTS TABLE
-- ============================================================================

CREATE TABLE accounts (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id                     UUID NOT NULL,
    account_number              VARCHAR(20) NOT NULL UNIQUE,
    account_type                account_type NOT NULL DEFAULT 'CHECKING',
    balance                     DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    reserved_balance            DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    available_balance           DECIMAL(19, 4) GENERATED ALWAYS AS (balance - reserved_balance) STORED,
    currency                    currency_code NOT NULL DEFAULT 'VND',
    status                      account_status NOT NULL DEFAULT 'ACTIVE',
    daily_transfer_limit        DECIMAL(19, 4) DEFAULT 100000000.0000,  -- 100M VND
    monthly_transfer_limit      DECIMAL(19, 4) DEFAULT 500000000.0000,  -- 500M VND
    daily_transfer_used         DECIMAL(19, 4) DEFAULT 0.0000,
    monthly_transfer_used        DECIMAL(19, 4) DEFAULT 0.0000,
    daily_reset_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_DATE,
    monthly_reset_at            TIMESTAMP WITH TIME ZONE DEFAULT DATE_TRUNC('month', CURRENT_TIMESTAMP),
    version                     BIGINT NOT NULL DEFAULT 0,  -- Optimistic locking
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_reserved_non_negative CHECK (reserved_balance >= 0),
    CONSTRAINT chk_reserved_not_exceed_balance CHECK (reserved_balance <= balance),
    CONSTRAINT chk_daily_limit CHECK (daily_transfer_used >= 0 AND daily_transfer_used <= daily_transfer_limit),
    CONSTRAINT chk_monthly_limit CHECK (monthly_transfer_used >= 0 AND monthly_transfer_used <= monthly_transfer_limit)
);

-- Indexes for accounts
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_created_at ON accounts(created_at DESC);

-- ============================================================================
-- TRANSACTIONS TABLE
-- ============================================================================

CREATE TABLE transactions (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    saga_id                     UUID NOT NULL,
    source_account_id           UUID NOT NULL REFERENCES accounts(id),
    target_account_id           UUID,  -- NULL for external transfers
    target_account_number       VARCHAR(20),  -- For external transfers
    target_bank_code            VARCHAR(10),  -- For inter-bank transfers
    amount                      DECIMAL(19, 4) NOT NULL,
    currency                    currency_code NOT NULL DEFAULT 'VND',
    fee                         DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
    total_amount                DECIMAL(19, 4) NOT NULL,  -- amount + fee
    status                      transaction_status NOT NULL DEFAULT 'PENDING',
    type                        transaction_type NOT NULL,
    description                 VARCHAR(500),
    idempotency_key             VARCHAR(64) UNIQUE,  -- Prevent duplicate transactions
    reference_number            VARCHAR(30) NOT NULL UNIQUE,  -- For external reference
    payos_payment_id            UUID,  -- Link to payment if applicable
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at                TIMESTAMP WITH TIME ZONE,  -- When status changed to terminal
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_fee_non_negative CHECK (fee >= 0),
    CONSTRAINT chk_total_positive CHECK (total_amount > 0),
    CONSTRAINT chk_different_accounts CHECK (source_account_id != target_account_id OR target_account_id IS NULL)
);

-- Indexes for transactions
CREATE INDEX idx_transactions_saga_id ON transactions(saga_id);
CREATE INDEX idx_transactions_source_account_id ON transactions(source_account_id);
CREATE INDEX idx_transactions_target_account_id ON transactions(target_account_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_created_at ON transactions(created_at DESC);
CREATE INDEX idx_transactions_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_transactions_reference_number ON transactions(reference_number);
CREATE INDEX idx_transactions_source_date ON transactions(source_account_id, created_at DESC);

-- Partial index for pending transactions (Saga recovery)
CREATE INDEX idx_transactions_pending ON transactions(created_at) 
    WHERE status IN ('PENDING', 'PROCESSING');

-- ============================================================================
-- STATEMENTS TABLE (Account Ledger)
-- ============================================================================

CREATE TABLE statements (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id                  UUID NOT NULL REFERENCES accounts(id),
    transaction_id              UUID REFERENCES transactions(id),
    statement_type              VARCHAR(20) NOT NULL,  -- CREDIT, DEBIT, RESERVE, RELEASE
    amount                      DECIMAL(19, 4) NOT NULL,
    balance_before              DECIMAL(19, 4) NOT NULL,
    balance_after               DECIMAL(19, 4) NOT NULL,
    description                 VARCHAR(500),
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for statements
CREATE INDEX idx_statements_account_id ON statements(account_id);
CREATE INDEX idx_statements_transaction_id ON statements(transaction_id);
CREATE INDEX idx_statements_created_at ON statements(created_at DESC);
CREATE INDEX idx_statements_account_date ON statements(account_id, created_at DESC);

-- ============================================================================
-- BALANCE RESERVATIONS TABLE (For Saga Pattern)
-- ============================================================================

CREATE TABLE balance_reservations (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id                  UUID NOT NULL REFERENCES accounts(id),
    transaction_id              UUID NOT NULL REFERENCES transactions(id),
    amount                      DECIMAL(19, 4) NOT NULL,
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, COMMITTED, ROLLED_BACK
    expires_at                  TIMESTAMP WITH TIME ZONE NOT NULL,  -- Auto-release after timeout
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_reservation_amount_positive CHECK (amount > 0)
);

-- Indexes for balance reservations
CREATE INDEX idx_reservations_account_id ON balance_reservations(account_id);
CREATE INDEX idx_reservations_transaction_id ON balance_reservations(transaction_id);
CREATE INDEX idx_reservations_status ON balance_reservations(status);
CREATE INDEX idx_reservations_expires_at ON balance_reservations(expires_at);

-- ============================================================================
-- SAGA STATES TABLE (Saga Orchestration Tracking)
-- ============================================================================

CREATE TABLE saga_states (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    saga_id                     UUID NOT NULL UNIQUE,
    saga_type                   VARCHAR(50) NOT NULL,  -- TRANSFER_SAGA, PAYMENT_SAGA
    current_step                INTEGER NOT NULL DEFAULT 0,
    total_steps                 INTEGER NOT NULL,
    status                      VARCHAR(20) NOT NULL,  -- RUNNING, COMPLETED, FAILED, COMPENSATING
    payload                     JSONB NOT NULL,  -- Saga input data
    steps_completed             JSONB DEFAULT '[]'::jsonb,  -- [{step: 1, status: 'OK'}]
    compensating_actions        JSONB DEFAULT '[]'::jsonb,  -- [{step: 1, action: 'credit_account'}]
    retry_count                 INTEGER NOT NULL DEFAULT 0,
    max_retries                 INTEGER NOT NULL DEFAULT 3,
    last_error                  TEXT,
    started_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at                TIMESTAMP WITH TIME ZONE,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for saga states
CREATE INDEX idx_saga_states_status ON saga_states(status);
CREATE INDEX idx_saga_states_started_at ON saga_states(started_at DESC);

-- ============================================================================
-- FUNCTIONS & TRIGGERS
-- ============================================================================

-- Function to update daily/monthly reset counters
CREATE OR REPLACE FUNCTION reset_transfer_limits()
RETURNS TRIGGER AS $$
BEGIN
    -- Reset daily if past reset time
    IF CURRENT_DATE > (SELECT daily_reset_at::date FROM accounts WHERE id = NEW.id) THEN
        UPDATE accounts SET 
            daily_transfer_used = 0,
            daily_reset_at = CURRENT_DATE
        WHERE id = NEW.id;
    END IF;
    
    -- Reset monthly if past reset time
    IF DATE_TRUNC('month', CURRENT_TIMESTAMP) > DATE_TRUNC('month', 
        (SELECT monthly_reset_at FROM accounts WHERE id = NEW.id)) THEN
        UPDATE accounts SET 
            monthly_transfer_used = 0,
            monthly_reset_at = DATE_TRUNC('month', CURRENT_TIMESTAMP)
        WHERE id = NEW.id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to check limits before transaction
CREATE OR REPLACE FUNCTION check_transfer_limits()
RETURNS TRIGGER AS $$
DECLARE
    v_daily_limit DECIMAL(19, 4);
    v_monthly_limit DECIMAL(19, 4);
    v_daily_used DECIMAL(19, 4);
    v_monthly_used DECIMAL(19, 4);
BEGIN
    SELECT daily_transfer_limit, monthly_transfer_limit, 
           daily_transfer_used, monthly_transfer_used
    INTO v_daily_limit, v_monthly_limit, v_daily_used, v_monthly_used
    FROM accounts WHERE id = NEW.source_account_id;
    
    IF v_daily_used + NEW.total_amount > v_daily_limit THEN
        RAISE EXCEPTION 'DAILY_TRANSFER_LIMIT_EXCEEDED: Cannot transfer %. Daily limit is %.', 
            v_daily_used + NEW.total_amount, v_daily_limit;
    END IF;
    
    IF v_monthly_used + NEW.total_amount > v_monthly_limit THEN
        RAISE EXCEPTION 'MONTHLY_TRANSFER_LIMIT_EXCEEDED: Cannot transfer %. Monthly limit is %.', 
            v_monthly_used + NEW.total_amount, v_monthly_limit;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_transfer_limits
    BEFORE INSERT ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION check_transfer_limits();

-- Function to auto-expire old reservations
CREATE OR REPLACE FUNCTION expire_old_reservations()
RETURNS void AS $$
BEGIN
    UPDATE balance_reservations 
    SET status = 'ROLLED_BACK', updated_at = CURRENT_TIMESTAMP
    WHERE status = 'ACTIVE' AND expires_at < CURRENT_TIMESTAMP;
    
    -- Release expired reservations back to available balance
    UPDATE accounts a
    SET reserved_balance = reserved_balance - r.amount,
        updated_at = CURRENT_TIMESTAMP
    FROM balance_reservations r
    WHERE r.account_id = a.id 
      AND r.status = 'ROLLED_BACK' 
      AND r.updated_at = CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Run expiration check every minute (via pg_cron or external scheduler)
-- SELECT cron.schedule('expire-reservations', '* * * * *', 'SELECT expire_old_reservations()');

-- ============================================================================
-- AUDIT LOG (Immutable)
-- ============================================================================

CREATE TABLE audit_log (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name                  VARCHAR(50) NOT NULL,
    record_id                   UUID NOT NULL,
    action                      VARCHAR(10) NOT NULL,  -- INSERT, UPDATE, DELETE
    old_values                  JSONB,
    new_values                  JSONB,
    changed_by                  VARCHAR(100),
    changed_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason                      VARCHAR(500)
);

-- Index for audit log
CREATE INDEX idx_audit_log_table_record ON audit_log(table_name, record_id);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at DESC);

-- Prevent deletion from audit log
CREATE OR REPLACE FUNCTION prevent_audit_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit log records cannot be deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_audit_delete
    BEFORE DELETE ON audit_log
    FOR EACH ROW
    EXECUTE FUNCTION prevent_audit_delete();
```

---

## 4. Integration Logic: PayOS Payment Gateway

### 4.1 PayOS Integration Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        PayOS Payment Flow                                        │
└─────────────────────────────────────────────────────────────────────────────────┘

┌──────────┐                         ┌──────────────┐                         ┌──────────┐
│  Client  │                         │ Payment Svc  │                         │  PayOS   │
└────┬─────┘                         └──────┬───────┘                         └────┬─────┘
     │                                    │                                    │
     │  POST /payments/link               │                                    │
     │  {amount, orderId, desc}           │                                    │
     │───────────────────────────────────>│                                    │
     │                                    │                                    │
     │                                    │  Create payment link via API       │
     │                                    │───────────────────────────────────>│
     │                                    │                                    │
     │                                    │  {checkoutUrl, qrCode, orderId}    │
     │                                    │<───────────────────────────────────│
     │                                    │                                    │
     │  {checkoutUrl, qrCode}             │                                    │
     │<───────────────────────────────────│                                    │
     │                                    │                                    │
     │  User pays via PayOS App/Bank App  │                                    │
     │<─────────────────────────────────────────────────────────────────────────│
     │                                    │                                    │
     │                                    │        Webhook: PAYMENT_SUCCESS     │
     │                                    │        {orderId, status, ...}      │
     │                                    │<───────────────────────────────────│
     │                                    │                                    │
     │                                    │  Validate webhook signature         │
     │                                    │  (HMAC-SHA256)                      │
     │                                    │                                    │
     │                                    │  Publish to Kafka:                  │
     │                                    │  payment.events                     │
     │                                    │                                    │
     │                                    │  Return 200 to PayOS               │
     │                                    │───────────────────────────────────>│
     │                                    │                                    │
```

### 4.2 Payment Service Implementation

```java
package com.banking.payment.service;

import com.banking.payment.config.PayOSProperties;
import com.banking.payment.dto.PaymentLinkRequest;
import com.banking.payment.dto.PaymentLinkResponse;
import com.banking.payment.dto.PaymentStatus;
import com.banking.payment.dto.WebhookPayload;
import com.banking.payment.entity.Payment;
import com.banking.payment.exception.PaymentException;
import com.banking.payment.exception.WebhookValidationException;
import com.banking.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * PayOS Payment Gateway Integration Service
 * 
 * Handles:
 * - Payment link generation (QR codes, checkout URLs)
 * - Webhook signature validation
 * - Payment status tracking
 * - Idempotent webhook processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSService {

    private final PayOSProperties payOSProperties;
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KAFKA_TOPIC = "payment.events";
    private static final String WEBHOOK_CACHE_PREFIX = "payos:webhook:";
    private static final Duration WEBHOOK_CACHE_TTL = Duration.ofHours(24);

    /**
     * Generate PayOS payment link for a transaction
     */
    @Transactional
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        log.info("Creating PayOS payment link for transaction: {}", request.getTransactionId());

        // Build PayOS API request
        PayOSCreateOrder payOSRequest = PayOSCreateOrder.builder()
                .orderCode(String.valueOf(System.currentTimeMillis()))
                .amount(request.getAmount().intValue())
                .description(request.getDescription())
                .buyerEmail(request.getBuyerEmail())
                .buyerPhone(request.getBuyerPhone())
                .buyerName(request.getBuyerName())
                .build();

        try {
            // Call PayOS API
            PayOSResponse payOSResponse = callPayOSApi(payOSRequest);

            // Create payment record
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .transactionId(request.getTransactionId())
                    .payosOrderCode(payOSResponse.getOrderCode())
                    .amount(request.getAmount())
                    .status(PaymentStatus.PENDING)
                    .qrCodeUrl(payOSResponse.getQrCodeUrl())
                    .paymentUrl(payOSResponse.getCheckoutUrl())
                    .expiresAt(Instant.now().plus(Duration.ofMinutes(15)))
                    .build();

            paymentRepository.save(payment);

            return PaymentLinkResponse.builder()
                    .paymentId(payment.getId())
                    .checkoutUrl(payOSResponse.getCheckoutUrl())
                    .qrCodeUrl(payOSResponse.getQrCodeUrl())
                    .expiresAt(payment.getExpiresAt())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create PayOS payment link", e);
            throw new PaymentException("PAYMENT_LINK_CREATION_FAILED", "Failed to create payment link");
        }
    }

    /**
     * Process PayOS webhook - MUST be idempotent
     * 
     * Security: Validate HMAC-SHA256 signature
     * Idempotency: Check webhook cache before processing
     */
    @Transactional
    public void processWebhook(WebhookPayload payload) {
        String webhookKey = WEBHOOK_CACHE_PREFIX + payload.getOrderCode();

        // Idempotency check - skip if already processed
        if (Boolean.TRUE.equals(redisTemplate.hasKey(webhookKey))) {
            log.info("Webhook already processed for order: {}", payload.getOrderCode());
            return;
        }

        // Validate webhook signature
        validateWebhookSignature(payload);

        // Find payment by PayOS order code
        Payment payment = paymentRepository.findByPayosOrderCode(payload.getOrderCode())
                .orElseThrow(() -> new WebhookValidationException("Payment not found: " + payload.getOrderCode()));

        // Update payment status
        PaymentStatus newStatus = mapPayOSStatus(payload.getStatus());
        payment.setStatus(newStatus);
        payment.setPaidAt(payload.getPaidAt());

        if (newStatus == PaymentStatus.SUCCESS) {
            payment.setPayosTransactionId(payload.getTransactionId());
        }

        paymentRepository.save(payment);

        // Cache webhook to prevent duplicate processing
        redisTemplate.opsForValue().set(webhookKey, objectMapper.writeValueAsString(payload), WEBHOOK_CACHE_TTL);

        // Publish event to Kafka
        publishPaymentEvent(payment, newStatus);

        log.info("Webhook processed successfully for order: {}, status: {}", 
                payload.getOrderCode(), newStatus);
    }

    /**
     * Validate PayOS webhook signature using HMAC-SHA256
     * 
     * Signature = HMAC-SHA256(data, webhook_key)
     * where data = orderCode|amount|status
     */
    private void validateWebhookSignature(WebhookPayload payload) {
        try {
            String data = String.format("%s|%s|%s",
                    payload.getOrderCode(),
                    payload.getAmount(),
                    payload.getStatus());

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    payOSProperties.getWebhookKey().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(secretKey);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String calculatedSignature = Base64.getEncoder().encodeToString(hash);

            if (!calculatedSignature.equals(payload.getSignature())) {
                log.error("Invalid webhook signature for order: {}", payload.getOrderCode());
                throw new WebhookValidationException("Invalid webhook signature");
            }

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to validate webhook signature", e);
            throw new WebhookValidationException("Signature validation failed");
        }
    }

    /**
     * Map PayOS status to internal PaymentStatus
     */
    private PaymentStatus mapPayOSStatus(String payosStatus) {
        return switch (payosStatus.toUpperCase()) {
            case "PAID" -> PaymentStatus.SUCCESS;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "EXPIRED" -> PaymentStatus.EXPIRED;
            default -> PaymentStatus.PENDING;
        };
    }

    /**
     * Publish payment event to Kafka for downstream consumers
     */
    private void publishPaymentEvent(Payment payment, PaymentStatus status) {
        PaymentEvent event = PaymentEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .paymentId(payment.getId())
                .transactionId(payment.getTransactionId())
                .status(status)
                .payosOrderCode(payment.getPayosOrderCode())
                .amount(payment.getAmount())
                .timestamp(Instant.now())
                .build();

        kafkaTemplate.send(KAFKA_TOPIC, payment.getTransactionId().toString(), event);
    }

    /**
     * Cancel expired payment link
     */
    @Transactional
    public void cancelPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("PAYMENT_NOT_FOUND", "Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new PaymentException("CANNOT_CANCEL_PAID", "Cannot cancel completed payment");
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        // Notify via Kafka
        publishPaymentEvent(payment, PaymentStatus.CANCELLED);
    }
}
```

### 4.3 PayOS Configuration

```java
package com.banking.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PayOS Configuration Properties
 * Loaded from external config server (application.yml or config.yaml)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "banking.payos")
public class PayOSProperties {

    private String clientId;
    private String apiKey;
    private String webhookKey;
    private String baseUrl;
    private int connectionTimeout = 10_000;  // 10 seconds
    private int readTimeout = 30_000;        // 30 seconds
    private int maxRetries = 3;
}
```

---

## 5. Resiliency Strategy: Resilience4j

### 5.1 Resilience4j Configuration

```yaml
# application.yml - Resilience4j Configuration

resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 30s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
        recordExceptions:
          - com.banking.payment.exception.PaymentException
          - java.io.IOException
          - java.util.concurrent.TimeoutException
        
      accountService:
        registerHealthIndicator: true
        slidingWindowSize: 20
        minimumNumberOfCalls: 10
        permittedNumberOfCallsInHalfOpenState: 5
        waitDurationInOpenState: 60s
        failureRateThreshold: 40
        
      externalBankApi:
        registerHealthIndicator: true
        slidingWindowSize: 100
        minimumNumberOfCalls: 50
        permittedNumberOfCallsInHalfOpenState: 10
        waitDurationInOpenState: 120s
        failureRateThreshold: 30
        slowCallRateThreshold: 80
        slowCallDurationThreshold: 5s

  retry:
    instances:
      paymentService:
        maxAttempts: 3
        waitDuration: 2s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - org.springframework.web.client.HttpServerErrorException
          - java.net.SocketTimeoutException
        ignoreExceptions:
          - com.banking.payment.exception.ValidationException
          
      kafkaPublish:
        maxAttempts: 5
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 1.5
        retryExceptions:
          - org.apache.kafka.common.KafkaException

  ratelimiter:
    instances:
      payosApi:
        limitForPeriod: 100
        limitRefreshPeriod: 1m
        timeoutDuration: 5s
        registerHealthIndicator: true
        
      internalApi:
        limitForPeriod: 1000
        limitRefreshPeriod: 1m
        timeoutDuration: 10s

  bulkhead:
    instances:
      externalCalls:
        maxConcurrentCalls: 50
        maxWaitDuration: 100ms
        
      internalCalls:
        maxConcurrentCalls: 200
        maxWaitDuration: 50ms

  threadpool:
    bulkhead:
      paymentThreadPool:
        coreThreadPoolSize: 10
        maxThreadPoolSize: 50
        queueCapacity: 100
        keepAliveDuration: 600s
```

### 5.2 Resilience4j Service Implementation

```java
package com.banking.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * Resilience4j Configuration for Banking Services
 * 
 * Provides:
 * - Circuit Breaker: Prevent cascading failures
 * - Retry: Handle transient failures
 * - Rate Limiter: Protect against overload
 * - Bulkhead: Isolate failures
 * - Time Limiter: Prevent hanging operations
 */
@Configuration
@Slf4j
public class ResilienceConfiguration {

    // ========================================================================
    // Circuit Breaker Configuration
    // ========================================================================

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    @Bean
    public CircuitBreaker paymentCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .recordExceptions(
                        IOException.class,
                        TimeoutException.class,
                        IllegalStateException.class)
                .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("paymentService", config);

        // Event consumers for monitoring
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> 
                    log.warn("Circuit breaker state transition: {} -> {}", 
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()))
                .onFailureRateExceeded(event ->
                    log.error("Circuit breaker failure rate exceeded: {}", 
                            event.getFailureRate()));

        return circuitBreaker;
    }

    // ========================================================================
    // Retry Configuration
    // ========================================================================

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(2))
                .retryExceptions(IOException.class, TimeoutException.class)
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    @Bean
    public Retry kafkaRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofSeconds(1))
                .enableExponentialBackoff(true)
                .exponentialBackoffMultiplier(1.5)
                .retryExceptions(IOException.class, org.apache.kafka.common.KafkaException.class)
                .build();

        Retry retry = registry.retry("kafkaPublish", config);

        retry.getEventPublisher()
                .onRetry(event ->
                    log.warn("Kafka publish retry attempt #{}, waiting {}ms",
                            event.getNumberOfRetryAttempts(),
                            event.getWaitInterval().toMillis()))
                .onError(event ->
                    log.error("Kafka publish failed after {} retries", 
                            event.getNumberOfRetryAttempts()));

        return retry;
    }

    // ========================================================================
    // Resilience4j Template - Clean API for Services
    // ========================================================================

    @Bean
    public ResilienceTemplate resilienceTemplate(
            CircuitBreakerRegistry cbRegistry,
            RetryRegistry retryRegistry) {
        return new ResilienceTemplate(cbRegistry, retryRegistry);
    }
}

/**
 * Clean API wrapper for Resilience4j operations
 */
@Slf4j
public class ResilienceTemplate {

    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;

    public ResilienceTemplate(CircuitBreakerRegistry cbRegistry, RetryRegistry retryRegistry) {
        this.cbRegistry = cbRegistry;
        this.retryRegistry = retryRegistry;
    }

    /**
     * Execute operation with circuit breaker and retry
     */
    public <T> T execute(String name, Supplier<T> operation) {
        CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);

        return Decorators.ofSupplier(operation)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .withFallback(List.of(IOException.class, TimeoutException.class),
                        e -> {
                            log.error("Operation {} failed with fallback", name, e);
                            return null;
                        })
                .decorate()
                .get();
    }

    /**
     * Execute async operation with circuit breaker, retry, and time limiter
     */
    public <T> CompletableFuture<T> executeAsync(String name, 
                                                   Supplier<CompletableFuture<T>> operation,
                                                   Duration timeout) {
        CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);
        TimeLimiter timeLimiter = TimeLimiterRegistry.of(
                TimeLimiterConfig.custom()
                        .timeoutDuration(timeout)
                        .build()
        ).timeLimiter(name);

        return Decorators.ofSupplier(operation)
                .withCircuitBreaker(circuitBreaker)
                .withRetry(retry)
                .withTimeLimiter(timeLimiter)
                .withFallback(List.of(TimeoutException.class),
                        e -> {
                            log.error("Async operation {} timed out", name);
                            return CompletableFuture.completedFuture(null);
                        })
                .decorate()
                .get();
    }
}
```

### 5.3 Usage in Services

```java
package com.banking.payment.service;

import com.banking.resilience.ResilienceTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Payment Service with Resilience Patterns
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PayOSService payOSService;
    private final ResilienceTemplate resilienceTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create payment link with circuit breaker protection
     */
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        return resilienceTemplate.execute("paymentService", () -> {
            return payOSService.createPaymentLink(request);
        });
    }

    /**
     * Publish event with retry (Kafka with idempotency)
     */
    public void publishPaymentEvent(Payment payment) {
        resilienceTemplate.execute("kafkaPublish", () -> {
            kafkaTemplate.send("payment.events", payment.getId().toString(), payment);
            return null;
        });
    }

    /**
     * Async call with timeout (e.g., external validation service)
     */
    public CompletableFuture<ValidationResult> validateAsync(Payment payment) {
        return resilienceTemplate.executeAsync(
                "validationService",
                () -> externalValidationClient.validate(payment),
                Duration.ofSeconds(5)
        );
    }
}
```

---

## 6. Sample Code Structure

### 6.1 Maven Multi-Module Project Structure

```
banking-system/
│
├── pom.xml                                    # Parent POM (Bill of Materials)
├── README.md
├── docker-compose.yml                         # Local development environment
├── config.yaml                                # External configuration
│
├───────────────────────────────────────────────────────────────────
│ MODULES
│─────────────────────────────────────────────────────────────────
│
├── banking-common/                            # Shared code across all services
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/banking/common/
│       │   │   ├── config/                    # Common configuration
│       │   │   ├── KafkaConfig.java
│       │   │   │   ├── RedisConfig.java
│       │   │   │   │   └── SecurityConfig.java
│       │   │   ├── dto/                       # Shared DTOs
│       │   │   │   ├── ApiResponse.java
│       │   │   │   │   ├── ErrorResponse.java
│       │   │   │   │   └── PageResponse.java
│       │   │   ├── entity/                    # Shared entities (JPA base)
│       │   │   │   ├── BaseEntity.java
│       │   │   │   │   └── AuditableEntity.java
│       │   │   ├── exception/                 # Common exceptions
│       │   │   │   ├── BankingException.java
│       │   │   │   │   ├── ErrorCode.java
│       │   │   │   │   └── GlobalExceptionHandler.java
│       │   │   ├── kafka/                     # Kafka utilities
│       │   │   │   ├── KafkaProducer.java
│       │   │   │   │   └── KafkaConsumer.java
│       │   │   ├── resilience/                # Resilience4j utilities
│       │   │   │   └── ResilienceTemplate.java
│       │   │   ├── security/                 # JWT utilities
│       │   │   │   ├── JwtTokenProvider.java
│       │   │   │   │   └── SecurityUtils.java
│       │   │   └── util/                     # Common utilities
│       │   │       ├── DateUtils.java
│       │   │       └── IdGenerator.java
│       │   └── resources/
│       │       └── application-common.yml
│       └── test/java/com/banking/common/
│
├───────────────────────────────────────────────────────────────────
│
├── auth-service/                              # Authentication & Authorization
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/auth/
│           │   ├── AuthServiceApplication.java
│           │   ├── config/
│           │   │   ├── AuthConfig.java
│           │   │   │   └── KeycloakConfig.java
│           │   ├── controller/
│           │   │   └── AuthController.java
│           │   ├── service/
│           │   │   ├── AuthService.java
│           │   │   │   ├── JwtService.java
│           │   │   │   └── SessionService.java
│           │   ├── repository/
│           │   │   ├── UserRepository.java
│           │   │   │   └── SessionRepository.java
│           │   └── mapper/
│           │       └── UserMapper.java
│           └── resources/
│               └── application.yml
│
├───────────────────────────────────────────────────────────────────
│
├── account-service/                           # Account Management
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/account/
│           │   ├── AccountServiceApplication.java
│           │   ├── config/
│           │   │   └── AccountConfig.java
│           │   ├── controller/
│           │   │   └── AccountController.java
│           │   ├── service/
│           │   │   ├── AccountService.java
│           │   │   ├── BalanceService.java
│           │   │   └── StatementService.java
│           │   ├── repository/
│           │   │   ├── AccountRepository.java
│           │   │   │   ├── StatementRepository.java
│           │   │   │   └── BalanceReservationRepository.java
│           │   ├── entity/
│           │   │   ├── Account.java
│           │   │   ├── Statement.java
│           │   │   └── BalanceReservation.java
│           │   ├── dto/
│           │   │   ├── AccountDTO.java
│           │   │   │   ├── BalanceDTO.java
│           │   │   │   └── StatementDTO.java
│           │   └── kafka/
│           │       └── AccountEventConsumer.java
│           └── resources/
│               ├── application.yml
│               └── db/migration/             # Flyway migrations
│                   └── V1__init_schema.sql
│
├───────────────────────────────────────────────────────────────────
│
├── transaction-service/                       # Transaction & Saga Orchestration
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/transaction/
│           │   ├── TransactionServiceApplication.java
│           │   ├── config/
│           │   │   └── TransactionConfig.java
│           │   ├── controller/
│           │   │   ├── TransactionController.java
│           │   │   └── TransferController.java
│           │   ├── service/
│           │   │   ├── TransactionService.java
│           │   │   ├── TransferService.java
│           │   │   └── saga/
│           │   │       ├── SagaOrchestrator.java
│           │   │       ├── TransferSaga.java
│           │   │       └── SagaState.java
│           │   ├── repository/
│           │   │   ├── TransactionRepository.java
│           │   │   │   └── SagaStateRepository.java
│           │   ├── entity/
│           │   │   ├── Transaction.java
│           │   │   │   └── SagaState.java
│           │   ├── dto/
│           │   │   ├── TransferRequest.java
│           │   │   │   ├── TransferResponse.java
│           │   │   │   └── TransactionDTO.java
│           │   └── kafka/
│           │       ├── TransactionEventProducer.java
│           │       └── PaymentEventConsumer.java
│           └── resources/
│               └── application.yml
│
├───────────────────────────────────────────────────────────────────
│
├── payment-service/                           # PayOS Integration
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/payment/
│           │   ├── PaymentServiceApplication.java
│           │   ├── config/
│           │   │   ├── PayOSConfig.java
│           │   │   │   └── PayOSProperties.java
│           │   ├── controller/
│           │   │   ├── PaymentController.java
│           │   │   │   └── PaymentWebhookController.java
│           │   ├── service/
│           │   │   ├── PaymentService.java
│           │   │   │   └── PayOSService.java
│           │   ├── repository/
│           │   │   └── PaymentRepository.java
│           │   ├── entity/
│           │   │   └── Payment.java
│           │   ├── dto/
│           │   │   ├── PaymentLinkRequest.java
│           │   │   │   ├── PaymentLinkResponse.java
│           │   │   │   └── WebhookPayload.java
│           │   ├── kafka/
│           │   │   └── PaymentEventProducer.java
│           │   └── exception/
│           │       ├── PaymentException.java
│           │       │   └── WebhookValidationException.java
│           └── resources/
│               └── application.yml
│
├───────────────────────────────────────────────────────────────────
│
├── notification-service/                     # Notifications
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/notification/
│           │   ├── NotificationServiceApplication.java
│           │   ├── config/
│           │   │   ├── EmailConfig.java
│           │   │   │   ├── SmsConfig.java
│           │   │   │   └── FirebaseConfig.java
│           │   ├── controller/
│           │   │   └── NotificationController.java
│           │   ├── service/
│           │   │   ├── NotificationService.java
│           │   │   │   ├── EmailService.java
│           │   │   │   ├── SmsService.java
│           │   │   │   └── PushNotificationService.java
│           │   ├── repository/
│           │   │   └── NotificationRepository.java
│           │   ├── entity/
│           │   │   └── Notification.java
│           │   └── kafka/
│           │       └── NotificationEventConsumer.java
│           └── resources/
│               └── application.yml
│
├───────────────────────────────────────────────────────────────────
│
├── api-gateway/                               # Spring Cloud Gateway
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/gateway/
│           │   ├── GatewayApplication.java
│           │   ├── config/
│           │   │   ├── GatewayConfig.java
│           │   │   │   └── SecurityConfig.java
│           │   ├── filter/
│           │   │   ├── JwtAuthenticationFilter.java
│           │   │   │   ├── RateLimitFilter.java
│           │   │   │   │   └── LoggingFilter.java
│           │   └── route/
│           │       └── RouteConfig.java
│           └── resources/
│               └── application.yml
│
├───────────────────────────────────────────────────────────────────
│
├── discovery-server/                          # Eureka Discovery
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/discovery/
│           │   └── DiscoveryServerApplication.java
│           └── resources/
│               └── application.yml
│
├───────────────────────────────────────────────────────────────────
│
├── config-server/                             # Spring Cloud Config
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/banking/config/
│           │   └── ConfigServerApplication.java
│           └── resources/
│               └── application.yml
│
└───────────────────────────────────────────────────────────────────
```

### 6.2 Parent POM (Bill of Materials)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.banking</groupId>
    <artifactId>banking-system</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Banking System - Parent POM</name>
    <description>Modern Microservices Banking System with Spring Cloud</description>

    <!-- Spring Boot & Spring Cloud Version Management -->
    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- Spring Boot -->
        <spring-boot.version>3.2.0</spring-boot.version>
        
        <!-- Spring Cloud -->
        <spring-cloud.version>2023.0.0</spring-cloud.version>
        
        <!-- Database -->
        <postgresql.version>42.7.1</postgresql.version>
        <hikaricp.version>5.1.0</hikaricp.version>
        
        <!-- Kafka -->
        <spring-kafka.version>3.1.0</spring-kafka.version>
        
        <!-- Redis -->
        <spring-data-redis.version>3.2.0</spring-data-redis.version>
        
        <!-- Resilience4j -->
        <resilience4j.version>2.2.0</resilience4j.version>
        
        <!-- Security -->
        <spring-security.version>6.2.0</spring-security.version>
        <jjwt.version>0.12.3</jjwt.version>
        
        <!-- API Documentation -->
        <springdoc.version>2.3.0</springdoc.version>
        
        <!-- Testing -->
        <testcontainers.version>1.19.3</testcontainers.version>
        <mockito.version>5.8.0</mockito.version>
    </properties>

    <!-- Dependency BOMs -->
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- PostgreSQL -->
            <dependency>
                <groupId>org.postgresql</groupId>
                <artifactId>postgresql</artifactId>
                <version>${postgresql.version}</version>
            </dependency>

            <!-- Resilience4j -->
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-spring-boot3</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-circuitbreaker</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-retry</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-ratelimiter</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-timelimiter</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>

            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
            </dependency>

            <!-- TestContainers -->
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- Shared Dependencies (used by all modules) -->
    <dependencies>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Actuator for health checks -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <!-- Modules -->
    <modules>
        <module>banking-common</module>
        <module>auth-service</module>
        <module>account-service</module>
        <module>transaction-service</module>
        <module>payment-service</module>
        <module>notification-service</module>
        <module>api-gateway</module>
        <module>discovery-server</module>
        <module>config-server</module>
    </modules>

    <!-- Build Configuration -->
    <build>
        <pluginManagement>
            <plugins>
                <!-- Spring Boot Maven Plugin -->
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>

                <!-- Maven Compiler -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>${java.version}</source>
                        <target>${java.version}</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                        </annotationProcessorPaths>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>

    <!-- Profiles -->
    <profiles>
        <!-- Docker Profile -->
        <profile>
            <id>docker</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <configuration>
                            <layers>
                                <enabled>true</enabled>
                            </layers>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### 6.3 Service Module POM Example (account-service)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.banking</groupId>
        <artifactId>banking-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>account-service</artifactId>
    <packaging>jar</packaging>
    <name>Account Service</name>
    <description>Account Management Service</description>

    <dependencies>
        <!-- Common Module -->
        <dependency>
            <groupId>com.banking</groupId>
            <artifactId>banking-common</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- PostgreSQL -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Spring Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Spring Data Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Resilience4j -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Spring Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- API Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Flyway for DB Migration -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- TestContainers -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>kafka</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Summary

This architecture delivers a **production-ready microservices banking system** with:

| Requirement | Implementation |
|-------------|----------------|
| **High Availability** | Circuit breakers, retries, bulkhead isolation, multi-zone deployment |
| **ACID Compliance** | PostgreSQL with optimistic locking, proper isolation levels |
| **Saga Pattern** | Orchestration-based for complex transfers, choreography for notifications |
| **Event-Driven** | Apache Kafka with idempotent producers/consumers |
| **Security** | Keycloak/OAuth2/JWT with RBAC, HMAC webhook validation |
| **Observability** | ELK stack + Prometheus/Grafana with distributed tracing |
| **Resiliency** | Resilience4j with configurable circuit breakers, retries, rate limiters |