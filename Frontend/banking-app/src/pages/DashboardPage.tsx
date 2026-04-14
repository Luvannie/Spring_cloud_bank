import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '@/context/AuthContext'
import { Card, CardContent, Spinner, Button } from '@/components/common'
import { AccountCard, TransactionItem } from '@/components/banking'
import { useAccounts, useTransactions } from '@/hooks'
import { formatCurrency } from '@/utils'
import { ArrowRightLeft, Wallet, TrendingUp, CreditCard, Plus } from 'lucide-react'

export function DashboardPage() {
  const { user } = useAuth()
  const { accounts, isLoading: accountsLoading, fetchAccounts } = useAccounts()
  const { transactions, isLoading: transactionsLoading, fetchTransactions } = useTransactions()
  const [isInitialLoad, setIsInitialLoad] = useState(true)

  useEffect(() => {
    Promise.all([fetchAccounts(), fetchTransactions()]).finally(() => {
      setIsInitialLoad(false)
    })
  }, [fetchAccounts, fetchTransactions])

  const isLoading = isInitialLoad || accountsLoading || transactionsLoading

  // Calculate totals
  const totalBalance = accounts.reduce((sum, acc) => sum + acc.availableBalance, 0)
  const pendingTransactions = transactions.filter((t) => t.status === 'PENDING' || t.status === 'PROCESSING')
  const recentTransactions = transactions.slice(0, 5)

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
          <h1 className="text-2xl font-bold text-gray-900">
            Welcome back, {user?.username}! 👋
          </h1>
          <p className="text-gray-500 mt-1">Here's your financial overview</p>
        </div>
        <Link to="/accounts">
          <Button className="mt-4 sm:mt-0">
            <Plus className="h-4 w-4 mr-2" />
            New Account
          </Button>
        </Link>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <Card>
          <CardContent className="flex items-center">
            <div className="p-3 bg-primary-100 rounded-lg">
              <Wallet className="h-6 w-6 text-primary-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm text-gray-500">Total Balance</p>
              <p className="text-xl font-bold text-gray-900">
                {formatCurrency(totalBalance, 'VND')}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex items-center">
            <div className="p-3 bg-green-100 rounded-lg">
              <TrendingUp className="h-6 w-6 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm text-gray-500">Active Accounts</p>
              <p className="text-xl font-bold text-gray-900">
                {accounts.filter((a) => a.status === 'ACTIVE').length}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex items-center">
            <div className="p-3 bg-yellow-100 rounded-lg">
              <ArrowRightLeft className="h-6 w-6 text-yellow-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm text-gray-500">Pending Transfers</p>
              <p className="text-xl font-bold text-gray-900">
                {pendingTransactions.length}
              </p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="flex items-center">
            <div className="p-3 bg-blue-100 rounded-lg">
              <CreditCard className="h-6 w-6 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm text-gray-500">This Month</p>
              <p className="text-xl font-bold text-gray-900">
                {transactions.filter((t) => {
                  const txDate = new Date(t.createdAt)
                  const now = new Date()
                  return txDate.getMonth() === now.getMonth() && txDate.getFullYear() === now.getFullYear()
                }).length}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Accounts & Transactions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Accounts */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Your Accounts</h2>
            <Link to="/accounts" className="text-sm text-primary-600 hover:text-primary-500">
              View all
            </Link>
          </div>
          
          {accounts.length === 0 ? (
            <Card>
              <CardContent className="text-center py-8">
                <Wallet className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-500 mb-4">No accounts yet</p>
                <Link to="/accounts">
                  <Button variant="outline" size="sm">
                    <Plus className="h-4 w-4 mr-2" />
                    Create Account
                  </Button>
                </Link>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-4">
              {accounts.slice(0, 3).map((account) => (
                <AccountCard key={account.id} account={account} />
              ))}
            </div>
          )}
        </div>

        {/* Recent Transactions */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-900">Recent Transactions</h2>
            <Link to="/transfers" className="text-sm text-primary-600 hover:text-primary-500">
              View all
            </Link>
          </div>

          {recentTransactions.length === 0 ? (
            <Card>
              <CardContent className="text-center py-8">
                <ArrowRightLeft className="h-12 w-12 text-gray-300 mx-auto mb-3" />
                <p className="text-gray-500 mb-4">No transactions yet</p>
                <Link to="/transfers">
                  <Button variant="outline" size="sm">
                    Make a Transfer
                  </Button>
                </Link>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-3">
              {recentTransactions.map((transaction) => (
                <TransactionItem key={transaction.id} transaction={transaction} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
