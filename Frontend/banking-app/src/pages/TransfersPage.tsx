import { useState, useEffect } from 'react'
import { Button, Card, CardContent, Input, Alert, Spinner } from '@/components/common'
import { TransactionItem } from '@/components/banking'
import { useAccounts, useTransactions } from '@/hooks'
import { formatCurrency, isValidVNDAmount } from '@/utils'
import { ArrowRightLeft, Search, AlertCircle } from 'lucide-react'
import type { TransferRequest } from '@/types'

export function TransfersPage() {
  const { accounts } = useAccounts()
  const { transactions, isLoading, fetchTransactions, initiateTransfer } = useTransactions()
  const [showForm, setShowForm] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')

  // Form state
  const [formData, setFormData] = useState<TransferRequest>({
    sourceAccountId: '',
    targetAccountNumber: '',
    targetBankCode: '',
    amount: 0,
    currency: 'VND',
    description: '',
  })
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    fetchTransactions()
  }, [fetchTransactions])

  // Set default source account
  useEffect(() => {
    if (accounts.length > 0 && !formData.sourceAccountId) {
      const activeAccount = accounts.find((a) => a.status === 'ACTIVE')
      if (activeAccount) {
        setFormData((prev) => ({ ...prev, sourceAccountId: activeAccount.id }))
      }
    }
  }, [accounts, formData.sourceAccountId])

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {}

    if (!formData.sourceAccountId) {
      errors.sourceAccountId = 'Select a source account'
    }

    if (!formData.targetAccountNumber || formData.targetAccountNumber.length < 6) {
      errors.targetAccountNumber = 'Enter a valid target account number'
    }

    if (!isValidVNDAmount(formData.amount)) {
      errors.amount = 'Enter a valid amount (whole number for VND)'
    }

    if (formData.amount <= 0) {
      errors.amount = 'Amount must be greater than 0'
    }

    // Check sufficient balance
    const sourceAccount = accounts.find((a) => a.id === formData.sourceAccountId)
    if (sourceAccount && formData.amount > sourceAccount.availableBalance) {
      errors.amount = 'Insufficient balance'
    }

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)

    if (!validateForm()) return

    setIsSubmitting(true)
    try {
      const transaction = await initiateTransfer(formData)
      setSuccess(`Transfer initiated! Reference: ${transaction.referenceNumber}`)
      setShowForm(false)
      setFormData({
        sourceAccountId: accounts.find((a) => a.status === 'ACTIVE')?.id || '',
        targetAccountNumber: '',
        targetBankCode: '',
        amount: 0,
        currency: 'VND',
        description: '',
      })
      fetchTransactions()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Transfer failed. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  const filteredTransactions = transactions.filter((tx) => {
    if (!searchTerm) return true
    const search = searchTerm.toLowerCase()
    return (
      tx.referenceNumber.toLowerCase().includes(search) ||
      tx.description?.toLowerCase().includes(search) ||
      tx.targetAccountNumber?.includes(search)
    )
  })

  const sourceAccount = accounts.find((a) => a.id === formData.sourceAccountId)

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Transfers</h1>
          <p className="text-gray-500 mt-1">Send money to other accounts</p>
        </div>
        <Button onClick={() => setShowForm(!showForm)}>
          <ArrowRightLeft className="h-4 w-4 mr-2" />
          New Transfer
        </Button>
      </div>

      {/* Error/Success Alerts */}
      {error && (
        <Alert variant="danger" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert variant="success" onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}

      {/* Transfer Form */}
      {showForm && (
        <Card>
          <CardContent>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">New Transfer</h3>
            
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Source Account */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  From Account
                </label>
                <select
                  value={formData.sourceAccountId}
                  onChange={(e) => setFormData({ ...formData, sourceAccountId: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
                >
                  <option value="">Select account</option>
                  {accounts.filter((a) => a.status === 'ACTIVE').map((account) => (
                    <option key={account.id} value={account.id}>
                      {account.accountNumber} - {formatCurrency(account.availableBalance, account.currency)}
                    </option>
                  ))}
                </select>
                {formErrors.sourceAccountId && (
                  <p className="mt-1 text-sm text-red-600">{formErrors.sourceAccountId}</p>
                )}
              </div>

              {/* Target Account */}
              <Input
                label="To Account Number"
                value={formData.targetAccountNumber}
                onChange={(e) => setFormData({ ...formData, targetAccountNumber: e.target.value })}
                error={formErrors.targetAccountNumber}
                placeholder="Enter target account number"
              />

              {/* Target Bank (Optional - for inter-bank) */}
              <Input
                label="Target Bank Code (Optional)"
                value={formData.targetBankCode || ''}
                onChange={(e) => setFormData({ ...formData, targetBankCode: e.target.value })}
                placeholder="e.g., ACB, VCB"
              />

              {/* Amount */}
              <div>
                <Input
                  label="Amount"
                  type="number"
                  value={formData.amount || ''}
                  onChange={(e) => setFormData({ ...formData, amount: Number(e.target.value) })}
                  error={formErrors.amount}
                  placeholder="0"
                />
                {sourceAccount && (
                  <p className="mt-1 text-sm text-gray-500">
                    Available: {formatCurrency(sourceAccount.availableBalance, sourceAccount.currency)}
                  </p>
                )}
              </div>

              {/* Description */}
              <Input
                label="Description (Optional)"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="What's this transfer for?"
              />

              {/* Actions */}
              <div className="flex space-x-3 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setShowForm(false)}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button type="submit" isLoading={isSubmitting} className="flex-1">
                  Transfer
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Search */}
      <div className="relative">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
        <input
          type="text"
          placeholder="Search transactions..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
        />
      </div>

      {/* Transactions List */}
      <div>
        <h2 className="text-lg font-semibold text-gray-900 mb-4">Transaction History</h2>
        
        {isLoading ? (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        ) : filteredTransactions.length === 0 ? (
          <Card>
            <CardContent className="text-center py-8">
              <AlertCircle className="h-12 w-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-500">
                {searchTerm ? 'No transactions match your search' : 'No transactions yet'}
              </p>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-3">
            {filteredTransactions.map((transaction) => (
              <TransactionItem key={transaction.id} transaction={transaction} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
