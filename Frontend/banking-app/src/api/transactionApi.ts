import { axiosInstance } from './axios'
import type { Transaction, TransferRequest, TransferResponse, PaginatedResponse } from '@/types'

interface BackendPageResponse<T> {
  content: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
}

export const transactionApi = {
  initiateTransfer: async (data: TransferRequest): Promise<TransferResponse> => {
    const response = await axiosInstance.post<TransferResponse>('/api/v1/transfers', data)
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
    const response = await axiosInstance.get<BackendPageResponse<Transaction>>(
      '/api/v1/transactions',
      {
        params: { page, size, status },
      }
    )
    return {
      content: response.data.content,
      totalElements: response.data.totalElements,
      totalPages: response.data.totalPages,
      page: response.data.pageNumber,
      size: response.data.pageSize,
    }
  },

  getStatus: async (id: string): Promise<TransferResponse> => {
    const response = await axiosInstance.get<TransferResponse>(`/api/v1/transfers/${id}/status`)
    return response.data
  },
}
