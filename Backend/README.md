# Modern Microservices Banking System

A Spring Cloud-based microservices banking platform with ACID compliance, Saga pattern for distributed transactions, and PayOS payment gateway integration.

**Status:** ✅ Building | ✅ 93 Tests Passing | ✅ Security Review Complete (8/10)

---

## Features

- **Multi-Service Architecture** - 5 core microservices + infrastructure services
- **ACID Transactions** - PostgreSQL with pessimistic locking for financial operations
- **Saga Pattern** - Orchestration-based distributed transactions for inter-bank transfers with compensation
- **PayOS Integration** - Payment link generation, QR codes, webhooks
- **JWT Authentication** - Secure API access with Redis-backed token blacklist
- **Refresh Token Rotation** - Old tokens invalidated after use to prevent token reuse attacks
- **Event-Driven** - Kafka-based asynchronous communication between services
- **Real-Time Notifications** - Email, SMS, Push notifications
- **Token Encryption** - AES-256 encryption for tokens stored in browser localStorage

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway (JWT, Rate Limiting)             │
└─────────────────────────────────────────────────────────────────┘
                                   │
         ┌──────────┬──────────┬────┴────┬──────────┬──────────┐
         ▼          ▼          ▼         ▼          ▼          │
    ┌─────────┐┌─────────┐┌──────────┐┌────────┐┌──────────┐     │
    │  Auth   ││ Account ││Transaction││Payment ││Notification│   │
    │ Service ││ Service ││ Service  ││Service ││ Service  │     │
    └────┬────┘└────┬────┘└────┬─────┘└───┬────┘└────┬─────┘     │
         │          │          │           │          │           │
         ▼          ▼          ▼           ▼          ▼           │
    ┌─────────┐┌─────────┐┌──────────┐┌────────┐┌──────────┐     │
    │  Redis  ││PostgreSQL││ PostgreSQL││PostgreSQL││  Redis  │     │
    │(Sessions)││(Accounts)││(Transactions)││(Payments)││(Queue) │     │
    └─────────┘└─────────┘└──────────┘└────────┘└──────────┘     │
```

### Services

| Service | Port | Responsibility |
|---------|------|----------------|
| **auth-service** | 8081 | Authentication, JWT issuance, OAuth2, RBAC |
| **account-service** | 8082 | Account CRUD, Balance management, Freeze/Unfreeze |
| **transaction-service** | 8083 | Money transfers, Saga orchestration |
| **payment-service** | 8084 | PayOS integration, QR codes, Webhooks |
| **notification-service** | 8085 | Email, SMS, Push notifications |
| **api-gateway** | 8080 | JWT validation, Rate limiting, Request logging |
| **discovery-server** | 8761 | Eureka service registry |
| **config-server** | 8888 | Centralized configuration |

---

## Tech Stack

- **Framework:** Spring Boot 3.2, Spring Cloud 2023.0
- **Database:** PostgreSQL 15+ with JPA/Hibernate
- **Messaging:** Apache Kafka
- **Security:** Spring Security, JWT (jjwt), Redis token blacklist
- **API Documentation:** OpenAPI/Swagger (springdoc)
- **Resiliency:** Resilience4j (Circuit Breaker, Retry)
- **Build:** Maven
- **Testing:** JUnit 5, Mockito, AssertJ

---

## 🚢 Deployment

### Docker Compose (Recommended for Development)

```bash
# From project root
cd ..
docker-compose -f docker-compose.dev.yml up --build

# Access services at:
# - Frontend: http://localhost:3000
# - API Gateway: http://localhost:8080
# - Auth Service: http://localhost:8081
```

See [docker-compose.dev.yml](../docker-compose.dev.yml) for full service configuration.

### Manual Deployment

1. Ensure infrastructure is running:
   ```bash
   docker-compose -f docker-compose.yml up -d
   ```

2. Build all services:
   ```bash
   mvn clean package -DskipTests
   ```

3. Start services in order:
   ```bash
   mvn spring-boot:run -pl auth-service
   mvn spring-boot:run -pl account-service
   mvn spring-boot:run -pl transaction-service
   mvn spring-boot:run -pl payment-service
   mvn spring-boot:run -pl notification-service
   mvn spring-boot:run -pl api-gateway
   ```

---

## Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 15+
- Redis 7+
- Kafka 3.x (with Zookeeper)
- Docker (for containerized deployment)
- PayOS API credentials (for payment-service)

---

## Installation

### 1. Clone and Build

```bash
# Build all modules (skip tests for faster build)
mvn clean install -DskipTests

# Build and run tests
mvn clean install
```

### 2. Environment Configuration

Each service requires configuration via config-server or local `application.yml`:

**Required Environment Variables:**
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
PAYOS_CHECKSUM_KEY=your_checksum_key

# JWT
JWT_SECRET=your_jwt_secret_key_min_256_bits
```

### 3. Database Setup

```sql
-- Create database
CREATE DATABASE banking;

-- Run schema from ARCHITECTURE.md (Section 3)
-- Or let services auto-create tables via JPA
```

### 4. Start Infrastructure Services

```bash
# Start Redis
redis-server

# Start PostgreSQL
pg_ctl -D /usr/local/var/postgres start

# Start Kafka
kafka-server-start.sh config/server.properties

# Start Eureka Discovery
cd discovery-server && mvn spring-boot:run

# Start Config Server
cd config-server && mvn spring-boot:run
```

### 5. Start Microservices

```bash
# In separate terminals, start each service:
cd auth-service && mvn spring-boot:run
cd account-service && mvn spring-boot:run
cd transaction-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run

# Finally, start API Gateway
cd api-gateway && mvn spring-boot:run
```

---

## Quick Start

### Authentication

```bash
# Register a new user
curl -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","email":"john@example.com","password":"Password123!"}'

# Login
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"Password123!"}'
```

### Create Account

```bash
curl -X POST http://localhost:8082/api/v1/accounts \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user-uuid>","accountType":"CHECKING","currency":"VND"}'
```

### Initiate Transfer

```bash
curl -X POST http://localhost:8083/api/v1/transfers \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId":"<account-uuid>",
    "targetAccountNumber":"ACC-002",
    "amount":1000000,
    "currency":"VND",
    "description":"Payment for order #12345"
  }'
```

---

## Project Structure

```
Spring_cloud_bank/
├── banking-common/              # Shared DTOs, entities, utilities
├── auth-service/               # Authentication & authorization
│   └── src/
│       ├── main/java/.../auth/
│       │   ├── controller/    # REST endpoints
│       │   ├── service/       # Business logic
│       │   ├── repository/    # Data access
│       │   ├── entity/        # JPA entities
│       │   ├── dto/           # Request/Response DTOs
│       │   ├── security/      # JWT, filters
│       │   └── config/        # Spring configuration
│       └── test/              # Unit tests
├── account-service/           # Account & balance management
├── transaction-service/       # Transfers & Saga orchestration
│   └── src/main/java/.../transaction/
│       └── service/saga/       # Saga pattern implementation
├── payment-service/           # PayOS integration
├── notification-service/      # Email, SMS, Push notifications
├── api-gateway/               # Spring Cloud Gateway
├── discovery-server/          # Eureka server
├── config-server/             # Configuration server
├── ARCHITECTURE.md            # Detailed architecture documentation
└── SESSION_SUMMARY.md         # Session notes & test status
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Tests for Specific Service

```bash
mvn test -pl auth-service
mvn test -pl account-service
mvn test -pl transaction-service
```

### Test Coverage

| Service | Tests |
|---------|-------|
| auth-service | 37 |
| account-service | 19 |
| transaction-service | 19 |
| payment-service | 7 |
| notification-service | 11 |
| **Total** | **93** |

---

## API Documentation

Detailed API specifications available in:

- [ARCHITECTURE.md](ARCHITECTURE.md) - Full system architecture, data models, API specs
- Swagger UI - Available at `/swagger-ui.html` for each service

### Key API Endpoints

**Auth Service (8081)**
```
POST /api/v1/auth/login          # Authenticate
POST /api/v1/auth/register       # Register user
POST /api/v1/auth/refresh         # Refresh token
POST /api/v1/auth/logout          # Invalidate session
```

**Account Service (8082)**
```
POST /api/v1/accounts                        # Create account
GET  /api/v1/accounts/{id}                    # Get account
GET  /api/v1/accounts/{id}/balance            # Get balance
POST /api/v1/accounts/{id}/reserve            # Reserve balance (Saga)
POST /api/v1/accounts/{id}/commit              # Commit reservation
POST /api/v1/accounts/{id}/rollback            # Rollback reservation
```

**Transaction Service (8083)**
```
POST /api/v1/transfers              # Initiate transfer
GET  /api/v1/transactions/{id}      # Get transaction
GET  /api/v1/transactions           # List transactions
```

**Payment Service (8084)**
```
POST /api/v1/payments/link         # Generate PayOS payment link
GET  /api/v1/payments/{id}         # Get payment status
POST /api/v1/payments/webhook      # PayOS webhook
```

---

## Saga Pattern Implementation

The system uses **Saga Orchestration** for distributed transactions:

```
TransferSaga Steps:
1. Reserve balance (Account Service)
2. Create PayOS payment link (Payment Service)
3. Confirm payment via webhook (Payment Service)
4. Settle transfer (Transaction Service)
5. Send notifications (Notification Service)

On Failure: Compensation actions reverse each completed step
```

See [ARCHITECTURE.md](ARCHITECTURE.md) Section 1.2-1.3 for detailed flow diagrams.

---

## Configuration

Services use Spring Cloud Config for centralized configuration. Configuration files are in:

```
config-server/src/main/resources/
├── application.yml           # Config server itself
└── config/                  # Per-service configurations
    ├── auth-service.yml
    ├── account-service.yml
    ├── transaction-service.yml
    └── ...
```

---

## Troubleshooting

### Docker Deployment Issues

```bash
# Check if containers are running
docker-compose -f ../docker-compose.dev.yml ps

# View logs for specific service
docker-compose -f ../docker-compose.dev.yml logs auth-service

# Rebuild specific service
docker-compose -f ../docker-compose.dev.yml up --build auth-service
```

### Build Failures

```bash
# Clean and rebuild
mvn clean install -DskipTests

# Skip linting
mvn clean install -DskipTests -Dcheckstyle.skip=true
```

### Test Failures

```bash
# Run single test class
mvn test -pl auth-service -Dtest=AuthServiceTest

# Skip tests in specific module
mvn clean install -pl '!payment-service'
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose -f ../docker-compose.dev.yml logs postgres

# Connect to database
psql -h localhost -U banking_user -d banking_system
```

### Redis Connection Issues

```bash
# Test Redis connection
redis-cli -a redis_secure_password ping

# Check Redis logs
docker-compose -f ../docker-compose.dev.yml logs redis
```

### Kafka Issues

```bash
# Check Kafka is healthy
docker-compose -f ../docker-compose.dev.yml logs kafka

# Access Kafka UI at http://localhost:8090 to view topics and messages
```

---

## License

Internal use only - Proprietary Banking System

---

## Contact

For architecture questions, see [ARCHITECTURE.md](ARCHITECTURE.md).

For implementation details, explore the service-specific `README.md` files.
