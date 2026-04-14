import { Badge } from '@/components/common'
import { formatCurrency, formatRelativeTime, maskAccountNumber } from '@/utils'
import type { Transaction } from '@/types'
import { ArrowUpRight, ArrowDownLeft, ArrowRightLeft, CreditCard, RefreshCw } from 'lucide-react'

interface TransactionItemProps {
  transaction: Transaction
  onClick?: () => void
}

const statusVariant = {
  PENDING: 'warning',
  PROCESSING: 'info',
  COMPLETED: 'success',
  FAILED: 'danger',
  CANCELLED: 'gray',
} as const

const typeIcon = {
  TRANSFER: ArrowRightLeft,
  DEPOSIT: ArrowDownLeft,
  WITHDRAWAL: ArrowUpRight,
  PAYMENT: CreditCard,
  REFUND: RefreshCw,
}

export function TransactionItem({ transaction, onClick }: TransactionItemProps) {
  const Icon = typeIcon[transaction.type]
  const isCredit = transaction.type === 'DEPOSIT' || transaction.type === 'REFUND'

  return (
    <div
      onClick={onClick}
      className="flex items-center justify-between p-4 bg-white rounded-lg border border-gray-100 hover:border-gray-200 hover:shadow-sm transition-all cursor-pointer"
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && onClick?.()}
    >
      <div className="flex items-center">
        <div className={`
          p-2 rounded-full
          ${isCredit ? 'bg-green-100 text-green-600' : 'bg-blue-100 text-blue-600'}
        `}>
          <Icon className="h-5 w-5" />
        </div>
        <div className="ml-4">
          <div className="flex items-center gap-2">
            <p className="font-medium text-gray-900">
              {transaction.type.charAt(0) + transaction.type.slice(1).toLowerCase()}
            </p>
            <Badge variant={statusVariant[transaction.status]} size="sm">
              {transaction.status}
            </Badge>
          </div>
          <p className="text-sm text-gray-500">
            {transaction.targetAccountNumber 
              ? `To: ${maskAccountNumber(transaction.targetAccountNumber)}`
              : 'N/A'
            }
          </p>
          {transaction.description && (
            <p className="text-xs text-gray-400 mt-1 truncate max-w-xs">
              {transaction.description}
            </p>
          )}
        </div>
      </div>

      <div className="text-right">
        <p className={`
          font-semibold
          ${isCredit ? 'text-green-600' : 'text-gray-900'}
        `}>
          {isCredit ? '+' : '-'}{formatCurrency(transaction.amount, transaction.currency)}
        </p>
        <p className="text-xs text-gray-400 mt-1">
          {formatRelativeTime(transaction.createdAt)}
        </p>
      </div>
    </div>
  )
}
