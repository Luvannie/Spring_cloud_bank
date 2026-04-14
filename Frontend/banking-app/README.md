# Banking Frontend

A React + TypeScript frontend for the Banking Microservices System.

## Features

- **Authentication** - JWT-based login/registration
- **Dashboard** - Financial overview with balance and recent transactions
- **Accounts** - Create and manage bank accounts
- **Transfers** - Send money to other accounts with Saga pattern
- **Payments** - Create PayOS payment links and QR codes

## Tech Stack

- **Framework**: React 18 + Vite + TypeScript
- **Styling**: Tailwind CSS + Flowbite
- **Routing**: React Router v6
- **Forms**: React Hook Form + Zod validation
- **Icons**: Lucide React
- **HTTP Client**: Axios with interceptors

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn

### Installation

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

## Project Structure

```
src/
├── api/              # API clients for backend services
│   ├── axios.ts      # Axios instance with interceptors
│   ├── authApi.ts    # Authentication endpoints
│   ├── accountApi.ts # Account management endpoints
│   ├── transactionApi.ts
│   └── paymentApi.ts
├── components/       # Reusable UI components
│   ├── common/       # Generic components (Button, Input, Card, etc.)
│   └── banking/      # Banking-specific components (AccountCard, etc.)
├── context/          # React Context (AuthContext)
├── hooks/            # Custom hooks (useAccounts, useTransactions)
├── pages/             # Route page components
├── types/            # TypeScript interfaces
└── utils/            # Utility functions
```

## Backend Integration

The frontend expects the following backend services running:

| Service | Port | Base Path |
|---------|------|-----------|
| API Gateway | 8080 | /api/v1 |

Configure the API URL in `.env` (see `.env.example`).

## API Endpoints

### Auth Service (8081)
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/register` - Register
- `POST /api/v1/auth/refresh` - Refresh token
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/me` - Get current user

### Account Service (8082)
- `POST /api/v1/accounts` - Create account
- `GET /api/v1/accounts/{id}` - Get account
- `GET /api/v1/accounts/{id}/balance` - Get balance
- `PUT /api/v1/accounts/{id}/freeze` - Freeze account
- `PUT /api/v1/accounts/{id}/unfreeze` - Unfreeze account

### Transaction Service (8083)
- `POST /api/v1/transfers` - Initiate transfer
- `GET /api/v1/transactions/{id}` - Get transaction
- `GET /api/v1/transactions` - List transactions

### Payment Service (8084)
- `POST /api/v1/payments/link` - Create payment link
- `GET /api/v1/payments/{id}` - Get payment
- `POST /api/v1/payments/{id}/cancel` - Cancel payment

For detailed API documentation, see [Backend/ARCHITECTURE.md](../Backend/ARCHITECTURE.md).
