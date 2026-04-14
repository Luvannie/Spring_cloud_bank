// User & Auth Types
export interface User {
  id: string
  username: string
  email: string
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED'
  createdAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

export interface RegisterRequest {
  username: string
  email: string
  password: string
}

// Account Types
export type AccountType = 'CHECKING' | 'SAVINGS' | 'BUSINESS'
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED'
export type CurrencyCode = 'VND' | 'USD' | 'EUR'

export interface Account {
  id: string
  userId: string
  accountNumber: string
  accountType: AccountType
  balance: number
  reservedBalance: number
  availableBalance: number
  currency: CurrencyCode
  status: AccountStatus
  createdAt: string
  updatedAt: string
}

export interface CreateAccountRequest {
  userId: string
  accountType: AccountType
  currency: CurrencyCode
}

// Transaction Types
export type TransactionStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
export type TransactionType = 'TRANSFER' | 'DEPOSIT' | 'WITHDRAWAL' | 'PAYMENT' | 'REFUND'

export interface Transaction {
  id: string
  sagaId: string
  sourceAccountId: string
  targetAccountId: string | null
  targetAccountNumber: string | null
  targetBankCode: string | null
  amount: number
  currency: CurrencyCode
  fee: number
  totalAmount: number
  status: TransactionStatus
  type: TransactionType
  description: string
  referenceNumber: string
  payosPaymentId: string | null
  createdAt: string
  processedAt: string | null
}

export interface TransferRequest {
  sourceAccountId: string
  targetAccountNumber: string
  targetBankCode?: string
  amount: number
  currency: CurrencyCode
  description: string
}

// Payment Types
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'EXPIRED' | 'CANCELLED'

export interface Payment {
  id: string
  transactionId: string
  payosOrderId: string
  payosTransactionId: string | null
  amount: number
  status: PaymentStatus
  qrCodeUrl: string | null
  paymentUrl: string | null
  expiresAt: string
  paidAt: string | null
  createdAt: string
}

export interface PaymentLinkRequest {
  transactionId: string
  amount: number
  description: string
}

export interface PaymentLinkResponse {
  checkoutUrl: string
  qrCode: string
  orderId: string
}

// API Response Types
export interface ApiResponse<T> {
  success: boolean
  data?: T
  error?: string
  message?: string
}

export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

// Balance Operations (Saga)
export interface ReserveBalanceRequest {
  accountId: string
  transactionId: string
  amount: number
}

export interface CommitBalanceRequest {
  accountId: string
  transactionId: string
  amount: number
}

export interface RollbackBalanceRequest {
  accountId: string
  transactionId: string
  amount: number
}
