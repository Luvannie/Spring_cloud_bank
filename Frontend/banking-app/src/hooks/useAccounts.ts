import { useState, useCallback } from 'react'
import { accountApi } from '@/api'
import type { Account, CreateAccountRequest } from '@/types'

interface UseAccountsReturn {
  accounts: Account[]
  isLoading: boolean
  error: string | null
  fetchAccounts: () => Promise<void>
  getAccount: (id: string) => Promise<Account>
  createAccount: (data: CreateAccountRequest) => Promise<Account>
  freezeAccount: (id: string) => Promise<Account>
  unfreezeAccount: (id: string) => Promise<Account>
}

export function useAccounts(): UseAccountsReturn {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchAccounts = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      // TODO: Add paginated API call when available
      // For now, we'll get accounts from user profile
      const response = await accountApi.getById('current')
      setAccounts([response])
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch accounts')
    } finally {
      setIsLoading(false)
    }
  }, [])

  const getAccount = useCallback(async (id: string): Promise<Account> => {
    return accountApi.getById(id)
  }, [])

  const createAccount = useCallback(async (data: CreateAccountRequest): Promise<Account> => {
    const account = await accountApi.create(data)
    setAccounts((prev) => [...prev, account])
    return account
  }, [])

  const freezeAccount = useCallback(async (id: string): Promise<Account> => {
    const account = await accountApi.freeze(id)
    setAccounts((prev) => prev.map((a) => (a.id === id ? account : a)))
    return account
  }, [])

  const unfreezeAccount = useCallback(async (id: string): Promise<Account> => {
    const account = await accountApi.unfreeze(id)
    setAccounts((prev) => prev.map((a) => (a.id === id ? account : a)))
    return account
  }, [])

  return {
    accounts,
    isLoading,
    error,
    fetchAccounts,
    getAccount,
    createAccount,
    freezeAccount,
    unfreezeAccount,
  }
}
