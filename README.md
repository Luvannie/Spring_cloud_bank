# 🏦 Modern Banking System

A full-stack microservices banking platform with Spring Cloud backend and React frontend.

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Tests](https://img.shields.io/badge/tests-93%20passing-brightgreen)](#)
[![Java](https://img.shields.io/badge/Java-17+-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-blue)](#)
[![React](https://img.shields.io/badge/React-18-blue)](#)

---

## 🚀 Quick Start

### Prerequisites

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 17+ | Backend runtime |
| Maven | 3.8+ | Backend build |
| Node.js | 18+ | Frontend runtime |
| PostgreSQL | 15+ | Primary database |
| Redis | 7+ | Sessions & caching |
| Kafka | 3.x | Event streaming |

### Backend Setup

```bash
# Navigate to Backend
cd Backend

# Build all services
mvn clean install -DskipTests

# Run tests (93 tests)
mvn test

# Start services (requires infrastructure running)
mvn spring-boot:run -pl auth-service,account-service,transaction-service,payment-service,notification-service
```

### Frontend Setup

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
│   ├── auth-service/           # Authentication & JWT (37 tests)
│   ├── account-service/        # Account & Balance management (19 tests)
│   ├── transaction-service/    # Transfers & Saga orchestration (19 tests)
│   ├── payment-service/       # PayOS integration (7 tests)
│   ├── notification-service/ # Email, SMS, Push (11 tests)
│   ├── api-gateway/          # Spring Cloud Gateway
│   ├── discovery-server/      # Eureka Service Registry
│   ├── config-server/         # Centralized Configuration
│   ├── banking-common/        # Shared DTOs, entities, utilities
│   ├── ARCHITECTURE.md        # Detailed architecture documentation
│   └── README.md              # Backend documentation
│
├── Frontend/                   # React Application
│   └── banking-app/          # React + TypeScript + Vite
│       ├── src/
│       │   ├── api/          # API clients
│       │   ├── components/   # UI components
│       │   ├── pages/        # Route pages
│       │   ├── context/       # Auth context
│       │   ├── hooks/        # Custom hooks
│       │   └── types/        # TypeScript interfaces
│       └── README.md         # Frontend documentation
│
├── README.md                   # This file
└── .gitignore                # Git ignore rules
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    API Gateway (Port 8080)                       │
│              JWT Validation • Rate Limiting • Routing            │
└─────────────────────────────────────────────────────────────────┘
                                   │
         ┌──────────┬──────────┬────┴────┬──────────┬──────────┐
         ▼          ▼          ▼         ▼          ▼          ▼
    ┌─────────┐┌─────────┐┌──────────┐┌────────┐┌──────────┐
    │  Auth   ││ Account ││Transaction││Payment ││Notification│
    │   8081  ││  8082   ││   8083   ││  8084  ││   8085    │
    └────┬────┘└────┬────┘└────┬─────┘└───┬────┘└────┬─────┘
         │          │          │           │          │
         ▼          ▼          ▼           ▼          ▼
    ┌─────────┐┌─────────┐┌──────────┐┌────────┐┌──────────┐
    │  Redis  ││PostgreSQL││PostgreSQL││PostgreSQL││  Redis   │
    │(Sessions)││(Accounts)││(Transact)││(Payments)││(Queue)   │
    └─────────┘└─────────┘└──────────┘└────────┘└──────────┘
```

### Services

| Service | Port | Description |
|---------|------|-------------|
| **auth-service** | 8081 | User authentication, JWT issuance, Session management |
| **account-service** | 8082 | Account CRUD, Balance management, Freeze/Unfreeze |
| **transaction-service** | 8083 | Money transfers, Saga orchestration |
| **payment-service** | 8084 | PayOS integration, QR codes, Webhooks |
| **notification-service** | 8085 | Email, SMS, Push notifications |
| **api-gateway** | 8080 | JWT validation, Rate limiting |
| **discovery-server** | 8761 | Eureka service registry |
| **config-server** | 8888 | Centralized configuration |

---

## 🔑 Key Features

### Saga Pattern for Distributed Transactions
```
Transfer Saga Steps:
1. Reserve balance (Account Service)
2. Create PayOS payment link (Payment Service)  
3. Confirm payment via webhook (Payment Service)
4. Settle transfer (Transaction Service)
5. Send notifications (Notification Service)

On Failure: Compensation actions reverse each completed step
```

### Tech Stack

**Backend:**
- Spring Boot 3.2 / Spring Cloud 2023.0
- PostgreSQL 15+ with JPA/Hibernate
- Redis for sessions and caching
- Apache Kafka for event streaming
- Resilience4j for circuit breakers
- JWT authentication

**Frontend:**
- React 18 + Vite + TypeScript
- Tailwind CSS + Flowbite
- React Router v6
- React Hook Form + Zod
- Axios with interceptors

---

## 📋 API Endpoints

### Auth Service (8081)
```
POST /api/v1/auth/login       - Authenticate user
POST /api/v1/auth/register    - Register new user
POST /api/v1/auth/refresh     - Refresh access token
POST /api/v1/auth/logout      - Invalidate session
GET  /api/v1/auth/me          - Get current user
```

### Account Service (8082)
```
POST /api/v1/accounts                        - Create account
GET  /api/v1/accounts/{id}                  - Get account details
GET  /api/v1/accounts/{id}/balance          - Get balance
POST /api/v1/accounts/{id}/reserve           - Reserve balance (Saga)
POST /api/v1/accounts/{id}/commit            - Commit reservation
POST /api/v1/accounts/{id}/rollback         - Rollback reservation
PUT  /api/v1/accounts/{id}/freeze           - Freeze account
PUT  /api/v1/accounts/{id}/unfreeze         - Unfreeze account
```

### Transaction Service (8083)
```
POST /api/v1/transfers              - Initiate transfer (starts Saga)
GET  /api/v1/transactions/{id}     - Get transaction details
GET  /api/v1/transactions           - List transactions (paginated)
```

### Payment Service (8084)
```
POST /api/v1/payments/link          - Generate PayOS payment link
GET  /api/v1/payments/{id}          - Get payment status
POST /api/v1/payments/{id}/cancel   - Cancel payment
POST /api/v1/payments/webhook       - PayOS webhook endpoint
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

## 🔧 Environment Variables

### Backend
```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=banking
POSTGRES_USER=banking_user
POSTGRES_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# PayOS (Payment Service)
PAYOS_CLIENT_ID=your_client_id
PAYOS_API_KEY=your_api_key
PAYOS_WEBHOOK_KEY=your_webhook_key

# JWT
JWT_SECRET=your_jwt_secret_key_min_256_bits
```

### Frontend
```bash
VITE_API_BASE_URL=/api  # Proxy to API Gateway
```

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](Backend/ARCHITECTURE.md) | Detailed system architecture, database schema, API specs |
| [Backend/README.md](Backend/README.md) | Backend-specific documentation |
| [Frontend/banking-app/README.md](Frontend/banking-app/README.md) | Frontend-specific documentation |

---

## 🚢 Deployment

### Docker Compose (Coming Soon)
```bash
cd Backend
docker-compose up -d
```

### Manual Deployment
1. Build: `mvn clean package -DskipTests`
2. Configure environment variables
3. Start infrastructure (PostgreSQL, Redis, Kafka)
4. Start discovery-server
5. Start config-server
6. Start microservices
7. Start api-gateway

---

## 📝 License

Internal use only - Proprietary Banking System

---

## 🤝 Contributing

1. Create a feature branch
2. Make changes with tests
3. Ensure all tests pass
4. Submit a pull request

---

Built with 🛠️ by OpenAgent
