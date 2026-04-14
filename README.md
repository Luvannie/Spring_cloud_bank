# 🏦 Modern Banking System

A full-stack microservices banking platform with Spring Cloud backend and React frontend.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Tests](https://img.shields.io/badge/tests-93%20passing-brightgreen)](#)
[![Java](https://img.shields.io/badge/Java-17+-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-blue)](#)
[![React](https://img.shields.io/badge/React-18-blue)](#)
[![Security](https://img.shields.io/badge/Security-8%2F10-brightgreen)](#)

---

## 🚀 Quick Start

### Option 1: Docker Compose (Recommended)

```bash
# Build and start all services
docker-compose -f docker-compose.dev.yml up --build

# Or run in background
docker-compose -f docker-compose.dev.yml up -d --build
```

Access at **http://localhost:3000** (Frontend) | **http://localhost:8080** (API Gateway)

### Option 2: Manual Setup

#### Prerequisites

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 17+ | Backend runtime |
| Maven | 3.9+ | Backend build |
| Node.js | 18+ | Frontend runtime |
| PostgreSQL | 15+ | Primary database |
| Redis | 7+ | Sessions & caching |
| Kafka | 3.x | Event streaming |

#### Backend Setup

```bash
# Navigate to Backend
cd Backend

# Build all services
mvn clean install -DskipTests

# Start infrastructure services (or use Docker)
# docker-compose up -d postgres redis kafka zookeeper

# Run services
mvn spring-boot:run -pl auth-service,account-service,transaction-service,payment-service,notification-service,api-gateway
```

#### Frontend Setup

```bash
# Navigate to Frontend
cd Frontend/banking-app

# Install dependencies
npm install

# Start development server
npm run dev
```

Frontend runs at **http://localhost:3000** with proxy to API Gateway at **http://localhost:8080**

---

## 📁 Project Structure

```
Spring_cloud_bank/
├── Backend/                    # Spring Cloud Microservices
│   ├── auth-service/           # Authentication & JWT (Port 8081)
│   ├── account-service/        # Account & Balance management (Port 8082)
│   ├── transaction-service/    # Transfers & Saga orchestration (Port 8083)
│   ├── payment-service/       # PayOS integration (Port 8084)
│   ├── notification-service/   # Email, SMS, Push (Port 8085)
│   ├── api-gateway/           # Spring Cloud Gateway (Port 8080)
│   ├── discovery-server/      # Eureka Service Registry (Port 8761)
│   ├── config-server/         # Centralized Configuration (Port 8888)
│   ├── banking-common/        # Shared DTOs, entities, utilities
│   ├── docker-compose.yml      # Infrastructure only
│   ├── docker-compose.prod.yml # Production deployment
│   └── ARCHITECTURE.md         # Detailed architecture documentation
│
├── Frontend/                   # React Application
│   └── banking-app/           # React + TypeScript + Vite
│       ├── src/
│       │   ├── api/            # API clients (axios with encryption)
│       │   ├── components/     # UI components
│       │   ├── pages/          # Route pages
│       │   ├── context/         # Auth context
│       │   ├── hooks/          # Custom hooks
│       │   ├── utils/          # Utilities (token encryption)
│       │   └── types/         # TypeScript interfaces
│       ├── nginx.conf          # Nginx configuration with API proxy
│       └── Dockerfile          # Multi-stage Docker build
│
├── docker-compose.dev.yml      # Development environment (Backend + Frontend)
├── FULL_CODE_REVIEW_Spring_Cloud_Bank.md  # Security review report
├── MONKEY_TEST_REPORT_Spring_Cloud_Bank.md # Testing report
├── README.md                   # This file
└── .gitignore                  # Git ignore rules
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend (Port 3000)                        │
│              React + Vite + Nginx API Proxy                    │
└─────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (Port 8080)                       │
│              JWT Validation • Rate Limiting • Routing            │
└─────────────────────────────────────────────────────────────────┘
                                    │
     ┌──────────┬──────────┬──────────┬──────────┬──────────────┐
     ▼          ▼          ▼          ▼          ▼              ▼
┌─────────┐┌─────────┐┌──────────┐┌────────┐┌──────────┐   ┌──────────┐
│  Auth   ││ Account ││Transaction││Payment ││Notification│   │  Kafka   │
│   8081  ││  8082   ││   8083   ││  8084  ││   8085    │   │  (Kafka) │
└────┬────┘└────┬────┘└────┬─────┘└───┬────┘└────┬─────┘   └────┬─────┘
     │          │          │         │          │              │
     ▼          ▼          ▼         ▼          ▼              ▼
┌─────────┐┌─────────┐┌──────────┐┌────────┐┌──────────┐   ┌─────────┐
│  Redis  ││PostgreSQL││PostgreSQL││PostgreSQL││  Redis   │   │  Kafka  │
│(Tokens) ││(Accounts)││(Transact)││(Payments)││(Queue)   │   │ Broker  │
└─────────┘└─────────┘└──────────┘└────────┘└──────────┘   └─────────┘
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **auth-service** | 8081 | User authentication, JWT issuance, Session management, Token blacklist |
| **account-service** | 8082 | Account CRUD, Balance management, Freeze/Unfreeze |
| **transaction-service** | 8083 | Money transfers, Saga orchestration, Compensation |
| **payment-service** | 8084 | PayOS integration, QR codes, Webhooks |
| **notification-service** | 8085 | Email, SMS, Push notifications |
| **api-gateway** | 8080 | JWT validation, Rate limiting, Request routing |
| **frontend** | 3000 | React app served via Nginx with API proxy |

---

## 🔐 Security Features (Post-Review)

All critical security issues identified in code reviews have been fixed:

### ✅ Authentication & Authorization
- ✅ JWT `getUserRoles()` fixed - now returns only user's roles, not all roles
- ✅ Token blacklist on logout - tokens invalidated via Redis before expiry
- ✅ Refresh token rotation - old tokens invalidated after use
- ✅ JWT secret validation - minimum 32 character enforcement
- ✅ Source account ownership verification on transfers
- ✅ Transaction ownership verification on status checks
- ✅ Account ownership verification on all account operations

### ✅ Data Protection
- ✅ Account numbers generated with `SecureRandom` (not predictable)
- ✅ Account number uniqueness check with retry logic
- ✅ Tokens encrypted in localStorage using AES
- ✅ `@Data` removed from JPA entities (prevents proxy issues)
- ✅ Daily transfer limit enforcement

### ✅ Input Validation
- ✅ Password complexity requirements (uppercase, lowercase, number, special char)
- ✅ Username/email enumeration prevention (generic error messages)
- ✅ Magic numbers replaced with configuration

### Security Score: **8/10** (Up from 4/10)

---

## 🔑 Key Features

### Saga Pattern for Distributed Transactions
```
Transfer Saga Steps:
1. Reserve balance (Account Service)
2. Create PayOS payment link (Payment Service)  
3. Wait for payment confirmation (via webhook)
4. Commit transfer (Transaction Service)
5. Send notifications (Notification Service)

On Failure: Compensation actions reverse each completed step in reverse order
```

### Token Security
- Access tokens: 15-minute expiry with Redis blacklist
- Refresh tokens: 7-day expiry with rotation
- Encrypted storage in frontend localStorage

---

## 📋 API Endpoints

### Auth Service (8081)
```
POST /api/v1/auth/login         - Authenticate user
POST /api/v1/auth/register      - Register new user
POST /api/v1/auth/refresh       - Refresh access token (with rotation)
POST /api/v1/auth/logout       - Invalidate session & blacklist token
GET  /api/v1/auth/me           - Get current user info
```

### Account Service (8082)
```
POST /api/v1/accounts                        - Create account (userId verified)
GET  /api/v1/accounts/{id}                  - Get account (ownership verified)
GET  /api/v1/accounts/{id}/balance          - Get balance (ownership verified)
POST /api/v1/accounts/{id}/reserve           - Reserve balance for saga
POST /api/v1/accounts/{id}/commit           - Commit reservation
POST /api/v1/accounts/{id}/rollback         - Rollback reservation
PUT  /api/v1/accounts/{id}/freeze          - Freeze account (ownership verified)
PUT  /api/v1/accounts/{id}/unfreeze         - Unfreeze account (ownership verified)
```

### Transaction Service (8083)
```
POST /api/v1/transfers                       - Initiate transfer (ownership verified)
GET  /api/v1/transfers/{id}/status         - Get transfer status (ownership verified)
GET  /api/v1/transactions                   - List transactions (paginated)
```

### Payment Service (8084)
```
POST /api/v1/payments/links                  - Generate PayOS payment link
GET  /api/v1/payments/{id}                  - Get payment status
GET  /api/v1/payments/transaction/{id}      - Get payment by transaction
POST /api/v1/payments/{id}/cancel           - Cancel payment
POST /api/v1/payments/webhook                - PayOS webhook endpoint
```

---

## 🧪 Testing

### Run All Backend Tests
```bash
cd Backend
mvn test
```

### Test Results
| Service | Tests | Status |
|---------|-------|--------|
| auth-service | 37 | ✅ |
| account-service | 19 | ✅ |
| transaction-service | 19 | ✅ |
| payment-service | 7 | ✅ |
| notification-service | 11 | ✅ |
| **Total** | **93** | **✅ All Passing** |

### Run Frontend Tests
```bash
cd Frontend/banking-app
npm test
```

---

## 🚢 Deployment

### Docker Compose (Development)

```bash
# Start all services (Backend + Frontend + Infrastructure)
docker-compose -f docker-compose.dev.yml up --build

# Start in background
docker-compose -f docker-compose.dev.yml up -d --build

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop all
docker-compose -f docker-compose.dev.yml down
```

### Docker Compose Ports

| Service | Port | URL |
|---------|------|-----|
| Frontend | 3000 | http://localhost:3000 |
| API Gateway | 8080 | http://localhost:8080 |
| Auth Service | 8081 | http://localhost:8081 |
| Account Service | 8082 | http://localhost:8082 |
| Transaction Service | 8083 | http://localhost:8083 |
| Payment Service | 8084 | http://localhost:8084 |
| Notification Service | 8085 | http://localhost:8085 |
| Kafka UI | 8090 | http://localhost:8090 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |

### Manual Deployment

1. Build: `mvn clean package -DskipTests` in Backend
2. Configure environment variables
3. Start infrastructure (PostgreSQL, Redis, Kafka)
4. Start microservices in order: auth → account → transaction → payment → notification
5. Start api-gateway
6. Start frontend

---

## 🔧 Environment Variables

### Backend Services
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=banking_system
DB_USERNAME=banking_user
DB_PASSWORD=banking_secure_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_secure_password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT (Auth Service)
JWT_SECRET=your-jwt-secret-key-min-32-chars
JWT_ACCESS_TOKEN_EXPIRATION=900000
JWT_REFRESH_TOKEN_EXPIRATION=604800000

# Security (Auth Service)
SECURITY_MAX_LOGIN_ATTEMPTS=5
SECURITY_LOCKOUT_DURATION_MINUTES=15

# PayOS (Payment Service)
PAYOS_CLIENT_ID=your_client_id
PAYOS_API_KEY=your_api_key
PAYOS_WEBHOOK_KEY=your_webhook_key
```

### Frontend
```bash
# Production: Set encryption key
VITE_TOKEN_ENCRYPTION_KEY=your-32-char-min-key
```

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](Backend/ARCHITECTURE.md) | Detailed system architecture, database schema, API specs |
| [FULL_CODE_REVIEW](FULL_CODE_REVIEW_Spring_Cloud_Bank.md) | Comprehensive security review with 35+ issues fixed |
| [MONKEY_TEST_REPORT](MONKEY_TEST_REPORT_Spring_Cloud_Bank.md) | Authorization testing report with fixes applied |

---

## 📝 License

Internal use only - Proprietary Banking System

---

## 🤝 Contributing

1. Create a feature branch
2. Make changes with tests
3. Ensure all tests pass
4. Submit a pull request
5. Security-sensitive changes require additional review

---

Built with 🛠️ by OpenAgent
