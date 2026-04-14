import { Card, CardContent, Badge } from '@/components/common'
import { formatCurrency } from '@/utils'
import type { Account } from '@/types'
import { Wallet, Lock } from 'lucide-react'
import { cn } from '@/utils'

interface AccountCardProps {
  account: Account
  isSelected?: boolean
  onClick?: () => void
}

const statusVariant = {
  ACTIVE: 'success',
  FROZEN: 'warning',
  CLOSED: 'danger',
} as const

const accountTypeLabels = {
  CHECKING: 'Checking',
  SAVINGS: 'Savings',
  BUSINESS: 'Business',
}

export function AccountCard({ account, isSelected, onClick }: AccountCardProps) {
  return (
    <Card
      hover={!!onClick}
      onClick={onClick}
      className={cn(
        'border-2 transition-colors',
        isSelected ? 'border-primary-500' : 'border-transparent'
      )}
    >
      <CardContent>
        <div className="flex items-start justify-between">
          <div className="flex items-center">
            <div className="p-2 bg-primary-100 rounded-lg">
              <Wallet className="h-6 w-6 text-primary-600" />
            </div>
            <div className="ml-4">
              <div className="flex items-center gap-2">
                <h3 className="font-semibold text-gray-900">
                  {accountTypeLabels[account.accountType]}
                </h3>
                {account.status === 'FROZEN' && (
                  <Lock className="h-4 w-4 text-yellow-500" />
                )}
              </div>
              <p className="text-sm text-gray-500 font-mono">
                {account.accountNumber}
              </p>
            </div>
          </div>
          <Badge variant={statusVariant[account.status]}>
            {account.status}
          </Badge>
        </div>

        <div className="mt-6">
          <p className="text-sm text-gray-500">Available Balance</p>
          <p className="text-3xl font-bold text-gray-900">
            {formatCurrency(account.availableBalance, account.currency)}
          </p>
        </div>

        <div className="mt-4 pt-4 border-t border-gray-100 grid grid-cols-2 gap-4">
          <div>
            <p className="text-xs text-gray-500">Total Balance</p>
            <p className="text-sm font-medium text-gray-700">
              {formatCurrency(account.balance, account.currency)}
            </p>
          </div>
          <div>
            <p className="text-xs text-gray-500">Reserved</p>
            <p className="text-sm font-medium text-gray-700">
              {formatCurrency(account.reservedBalance, account.currency)}
            </p>
          </div>
        </div>
      </CardContent>
    </Card>
  )
}
