import { useState, useCallback } from 'react'
import { transactionApi } from '@/api'
import type { Transaction, TransferRequest, PaginatedResponse } from '@/types'

interface UseTransactionsReturn {
  transactions: Transaction[]
  isLoading: boolean
  error: string | null
  totalPages: number
  currentPage: number
  fetchTransactions: (page?: number) => Promise<void>
  getTransaction: (id: string) => Promise<Transaction>
  initiateTransfer: (data: TransferRequest) => Promise<Transaction>
  getTransactionStatus: (id: string) => Promise<{ status: string; sagaId: string }>
}

export function useTransactions(): UseTransactionsReturn {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [totalPages, setTotalPages] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)

  const fetchTransactions = useCallback(async (page = 0) => {
    setIsLoading(true)
    setError(null)
    try {
      const response: PaginatedResponse<Transaction> = await transactionApi.getList(page, 20)
      setTransactions(response.content)
      setTotalPages(response.totalPages)
      setCurrentPage(response.page)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to fetch transactions')
    } finally {
      setIsLoading(false)
    }
  }, [])

  const getTransaction = useCallback(async (id: string): Promise<Transaction> => {
    return transactionApi.getById(id)
  }, [])

  const initiateTransfer = useCallback(async (data: TransferRequest): Promise<Transaction> => {
    const transaction = await transactionApi.initiateTransfer(data)
    setTransactions((prev) => [transaction, ...prev])
    return transaction
  }, [])

  const getTransactionStatus = useCallback(async (id: string) => {
    return transactionApi.getStatus(id)
  }, [])

  return {
    transactions,
    isLoading,
    error,
    totalPages,
    currentPage,
    fetchTransactions,
    getTransaction,
    initiateTransfer,
    getTransactionStatus,
  }
}
