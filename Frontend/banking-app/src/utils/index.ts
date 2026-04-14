/**
 * Format currency amount with proper decimal places
 */
export function formatCurrency(amount: number, currency: string = 'VND'): string {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency,
    minimumFractionDigits: currency === 'VND' ? 0 : 2,
    maximumFractionDigits: currency === 'VND' ? 0 : 2,
  }).format(amount)
}

/**
 * Format date to local string
 */
export function formatDate(date: string | Date): string {
  return new Intl.DateTimeFormat('vi-VN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date))
}

/**
 * Format relative time (e.g., "2 hours ago")
 */
export function formatRelativeTime(date: string | Date): string {
  const now = new Date()
  const then = new Date(date)
  const diffInSeconds = Math.floor((now.getTime() - then.getTime()) / 1000)

  if (diffInSeconds < 60) return 'Just now'
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} minutes ago`
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)} hours ago`
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)} days ago`

  return formatDate(date)
}

/**
 * Mask account number for display (show last 4 digits)
 */
export function maskAccountNumber(accountNumber: string): string {
  if (accountNumber.length <= 4) return accountNumber
  return '*'.repeat(accountNumber.length - 4) + accountNumber.slice(-4)
}

/**
 * Generate transaction reference number
 */
export function generateReferenceNumber(): string {
  const timestamp = Date.now().toString(36).toUpperCase()
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  return `TXN-${timestamp}-${random}`
}

/**
 * Validate VND amount (no decimals)
 */
export function isValidVNDAmount(amount: number): boolean {
  return Number.isInteger(amount) && amount > 0
}

/**
 * cn - classnames utility
 */
import { clsx, type ClassValue } from 'clsx'

export function cn(...inputs: ClassValue[]): string {
  return clsx(inputs)
}
