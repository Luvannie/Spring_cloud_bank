import { axiosInstance } from './axios'
import type { Transaction, TransferRequest, PaginatedResponse } from '@/types'

export const transactionApi = {
  initiateTransfer: async (data: TransferRequest): Promise<Transaction> => {
    const response = await axiosInstance.post<Transaction>('/api/v1/transfers', data)
    return response.data
  },

  getById: async (id: string): Promise<Transaction> => {
    const response = await axiosInstance.get<Transaction>(`/api/v1/transactions/${id}`)
    return response.data
  },

  getList: async (
    page = 0,
    size = 20,
    status?: string
  ): Promise<PaginatedResponse<Transaction>> => {
    const response = await axiosInstance.get<PaginatedResponse<Transaction>>(
      '/api/v1/transactions',
      {
        params: { page, size, status },
      }
    )
    return response.data
  },

  getStatus: async (id: string): Promise<{ status: string; sagaId: string }> => {
    const response = await axiosInstance.get(`/api/v1/transactions/${id}/status`)
    return response.data
  },
}
