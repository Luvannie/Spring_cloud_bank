import { useEffect, useState } from 'react'
import { Button, Card, CardContent, Spinner, Alert } from '@/components/common'
import { AccountCard } from '@/components/banking'
import { useAccounts } from '@/hooks'
import { useAuth } from '@/context/AuthContext'
import { Plus, Lock, Unlock } from 'lucide-react'
import type { Account, CreateAccountRequest } from '@/types'

export function AccountsPage() {
  const { accounts, isLoading, fetchAccounts, createAccount, freezeAccount, unfreezeAccount } = useAccounts()
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [actionLoading, setActionLoading] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  useEffect(() => {
    fetchAccounts()
  }, [fetchAccounts])

  const handleCreateAccount = async (data: CreateAccountRequest) => {
    setActionLoading(true)
    setActionError(null)
    try {
      await createAccount(data)
      setShowCreateModal(false)
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Failed to create account')
    } finally {
      setActionLoading(false)
    }
  }

  const handleFreezeToggle = async (account: Account) => {
    setActionError(null)
    const isFreeze = account.status !== 'FROZEN'
    const confirmMsg = isFreeze 
      ? 'This will prevent the account from being used for transactions.'
      : 'This will allow the account to be used again.'
    
    if (!window.confirm(`${isFreeze ? 'Freeze' : 'Unfreeze'} Account?\n${confirmMsg}`)) {
      return
    }
    
    setActionLoading(true)
    try {
      if (isFreeze) {
        await freezeAccount(account.id)
      } else {
        await unfreezeAccount(account.id)
      }
    } catch (err) {
      setActionError(err instanceof Error ? err.message : 'Action failed')
    } finally {
      setActionLoading(false)
    }
  }

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <Spinner size="lg" />
      </div>
    )
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Accounts</h1>
          <p className="text-gray-500 mt-1">Manage your bank accounts</p>
        </div>
        <Button onClick={() => setShowCreateModal(true)}>
          <Plus className="h-4 w-4 mr-2" />
          New Account
        </Button>
      </div>

      {/* Error Alert */}
      {actionError && (
        <Alert variant="danger" onClose={() => setActionError(null)}>
          {actionError}
        </Alert>
      )}

      {/* Accounts Grid */}
      {accounts.length === 0 ? (
        <Card>
          <CardContent className="text-center py-12">
            <p className="text-gray-500 mb-4">You don't have any accounts yet.</p>
            <Button onClick={() => setShowCreateModal(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Create Your First Account
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {accounts.map((account) => (
            <div key={account.id} className="relative">
              <AccountCard account={account} />
              <div className="absolute top-4 right-4">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleFreezeToggle(account)}
                  disabled={account.status === 'CLOSED'}
                >
                  {account.status === 'FROZEN' ? (
                    <Unlock className="h-4 w-4 text-green-600" />
                  ) : (
                    <Lock className="h-4 w-4 text-yellow-600" />
                  )}
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Account Summary */}
      {accounts.length > 0 && (
        <Card>
          <CardContent>
            <h3 className="font-semibold text-gray-900 mb-4">Account Summary</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <p className="text-sm text-gray-500">Total Accounts</p>
                <p className="text-xl font-bold text-gray-900">{accounts.length}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Active</p>
                <p className="text-xl font-bold text-green-600">
                  {accounts.filter((a) => a.status === 'ACTIVE').length}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Frozen</p>
                <p className="text-xl font-bold text-yellow-600">
                  {accounts.filter((a) => a.status === 'FROZEN').length}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Total Balance</p>
                <p className="text-xl font-bold text-gray-900">
                  {new Intl.NumberFormat('vi-VN', {
                    style: 'currency',
                    currency: 'VND',
                    minimumFractionDigits: 0,
                  }).format(accounts.reduce((sum, a) => sum + a.balance, 0))}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Create Account Modal */}
      {showCreateModal && (
        <CreateAccountModal
          onClose={() => setShowCreateModal(false)}
          onSubmit={handleCreateAccount}
          isLoading={actionLoading}
        />
      )}
    </div>
  )
}

// Simple Create Account Modal Component
function CreateAccountModal({
  onClose,
  onSubmit,
  isLoading
}: {
  onClose: () => void
  onSubmit: (data: CreateAccountRequest) => void
  isLoading: boolean
}) {
  const { user } = useAuth()
  const [accountType, setAccountType] = useState<'CHECKING' | 'SAVINGS' | 'BUSINESS'>('CHECKING')
  const [currency, setCurrency] = useState<'VND' | 'USD' | 'EUR'>('VND')

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!user) {
      throw new Error('User not authenticated')
    }
    onSubmit({
      userId: user.id,
      accountType,
      currency,
    })
  }

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-screen items-center justify-center p-4">
        <div className="fixed inset-0 bg-black bg-opacity-50" onClick={onClose} />
        
        <div className="relative bg-white rounded-lg shadow-xl max-w-md w-full p-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Create New Account</h3>
          
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Account Type</label>
              <div className="grid grid-cols-3 gap-2">
                {(['CHECKING', 'SAVINGS', 'BUSINESS'] as const).map((type) => (
                  <button
                    key={type}
                    type="button"
                    onClick={() => setAccountType(type)}
                    className={`p-3 border rounded-lg text-sm font-medium transition-colors ${
                      accountType === type
                        ? 'border-primary-500 bg-primary-50 text-primary-600'
                        : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    {type.charAt(0) + type.slice(1).toLowerCase()}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Currency</label>
              <div className="grid grid-cols-3 gap-2">
                {(['VND', 'USD', 'EUR'] as const).map((curr) => (
                  <button
                    key={curr}
                    type="button"
                    onClick={() => setCurrency(curr)}
                    className={`p-3 border rounded-lg text-sm font-medium transition-colors ${
                      currency === curr
                        ? 'border-primary-500 bg-primary-50 text-primary-600'
                        : 'border-gray-200 text-gray-600 hover:bg-gray-50'
                    }`}
                  >
                    {curr}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex space-x-3 pt-4">
              <Button type="button" variant="outline" onClick={onClose} className="flex-1">
                Cancel
              </Button>
              <Button type="submit" isLoading={isLoading} className="flex-1">
                Create
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
