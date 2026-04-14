# Banking Frontend

A React + TypeScript frontend for the Banking Microservices System.

## Features

- **Authentication** - JWT-based login/registration with encrypted token storage
- **Dashboard** - Financial overview with balance and recent transactions
- **Accounts** - Create and manage bank accounts with ownership verification
- **Transfers** - Send money to other accounts with Saga pattern
- **Payments** - Create PayOS payment links and QR codes

## Tech Stack

- **Framework**: React 18 + Vite + TypeScript
- **Styling**: Tailwind CSS + Flowbite
- **Routing**: React Router v6
- **Forms**: React Hook Form + Zod validation
- **Icons**: Lucide React
- **HTTP Client**: Axios with interceptors and encrypted token storage

## 🚀 Getting Started

### Option 1: Docker Compose (Recommended)

```bash
# From project root
cd ..
docker-compose -f docker-compose.dev.yml up --build frontend

# Access at http://localhost:3000
```

### Option 2: Manual

#### Prerequisites

- Node.js 18+
- npm or yarn

#### Installation

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at http://localhost:3000

### Building

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

---

## 🔐 Token Security

Tokens are encrypted in localStorage using AES-256 encryption with PBKDF2 key derivation:

- Access tokens: 15-minute expiry
- Refresh tokens: 7-day expiry with rotation
- Encryption key derived from `VITE_TOKEN_ENCRYPTION_KEY` environment variable

### Environment Variables

```bash
# Required for production
VITE_TOKEN_ENCRYPTION_KEY=your-min-32-char-encryption-key

# Optional - defaults to localhost:8080
VITE_API_BASE_URL=/api
```

---

## Project Structure

```
src/
├── api/              # API clients for backend services
│   ├── axios.ts       # Axios instance with interceptors
│   ├── authApi.ts     # Authentication endpoints
│   ├── accountApi.ts # Account management endpoints
│   ├── transactionApi.ts
│   └── paymentApi.ts
├── components/       # Reusable UI components
│   ├── common/       # Generic components (Button, Input, Card, etc.)
│   └── banking/      # Banking-specific components (AccountCard, etc.)
├── context/          # React Context (AuthContext with token encryption)
├── hooks/            # Custom hooks (useAccounts, useTransactions)
├── pages/            # Route page components
├── types/            # TypeScript interfaces
└── utils/            # Utility functions
    └── tokenStorage.ts  # AES token encryption utilities
```

---

## Backend Integration

The frontend communicates with the backend through the API Gateway.

| Service | Port | Base Path |
|---------|------|-----------|
| Frontend (Nginx) | 3000 | http://localhost:3000 |
| API Gateway | 8080 | /api/v1 |

### API Proxy (Nginx)

The Nginx configuration automatically proxies `/api/` requests to the API Gateway:

```nginx
location /api/ {
    proxy_pass http://banking-api-gateway:8080/api/;
}
```

### Docker Deployment

In Docker Compose, the frontend does NOT need `VITE_API_BASE_URL` set - the Nginx proxy handles routing:

```bash
docker-compose -f docker-compose.dev.yml up --build frontend
```

---

## API Endpoints

### Auth Service (8081)
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/register` - Register
- `POST /api/v1/auth/refresh` - Refresh token (with rotation)
- `POST /api/v1/auth/logout` - Logout (blacklists token)
- `GET /api/v1/auth/me` - Get current user

### Account Service (8082)
- `POST /api/v1/accounts` - Create account (userId verified)
- `GET /api/v1/accounts/{id}` - Get account (ownership verified)
- `GET /api/v1/accounts/{id}/balance` - Get balance (ownership verified)
- `PUT /api/v1/accounts/{id}/freeze` - Freeze account (ownership verified)
- `PUT /api/v1/accounts/{id}/unfreeze` - Unfreeze account (ownership verified)

### Transaction Service (8083)
- `POST /api/v1/transfers` - Initiate transfer (ownership verified)
- `GET /api/v1/transfers/{id}/status` - Get transfer status (ownership verified)
- `GET /api/v1/transactions` - List transactions

### Payment Service (8084)
- `POST /api/v1/payments/links` - Create payment link
- `GET /api/v1/payments/{id}` - Get payment
- `GET /api/v1/payments/transaction/{transactionId}` - Get by transaction
- `POST /api/v1/payments/{id}/cancel` - Cancel payment

---

## Troubleshooting

### CORS Issues

Ensure API Gateway allows CORS from frontend origin:

```bash
# Check nginx proxy is correctly forwarding headers
docker-compose logs frontend
```

### Token Expired

If you see 401 errors after login:
1. Check Redis is running (`docker-compose logs redis`)
2. Verify token blacklist is working
3. Clear localStorage and login again

### Build Failures

```bash
# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install
```

---

For detailed API documentation, see [Backend/ARCHITECTURE.md](../Backend/ARCHITECTURE.md).
