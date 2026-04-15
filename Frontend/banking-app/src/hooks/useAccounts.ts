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
  freezeAccount: (id: string) => Promise<void>
  unfreezeAccount: (id: string) => Promise<void>
}

export function useAccounts(): UseAccountsReturn {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchAccounts = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await accountApi.listMine()
      setAccounts(response)
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

  const freezeAccount = useCallback(async (id: string): Promise<void> => {
    await accountApi.freeze(id)
    setAccounts((prev) => prev.map((a) => (
      a.id === id ? { ...a, status: 'FROZEN' } : a
    )))
  }, [])

  const unfreezeAccount = useCallback(async (id: string): Promise<void> => {
    await accountApi.unfreeze(id)
    setAccounts((prev) => prev.map((a) => (
      a.id === id ? { ...a, status: 'ACTIVE' } : a
    )))
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
