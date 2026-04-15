import { axiosInstance } from './axios'
import type { 
  Account, 
  CreateAccountRequest, 
  PaginatedResponse,
  ReserveBalanceRequest,
  CommitBalanceRequest,
  RollbackBalanceRequest
} from '@/types'

export const accountApi = {
  listMine: async (): Promise<Account[]> => {
    const response = await axiosInstance.get<Account[]>('/api/v1/accounts')
    return response.data
  },

  create: async (data: CreateAccountRequest): Promise<Account> => {
    const response = await axiosInstance.post<Account>('/api/v1/accounts', data)
    return response.data
  },

  getById: async (id: string): Promise<Account> => {
    const response = await axiosInstance.get<Account>(`/api/v1/accounts/${id}`)
    return response.data
  },

  getBalance: async (id: string): Promise<{ balance: number; availableBalance: number; reservedBalance: number }> => {
    const response = await axiosInstance.get(`/api/v1/accounts/${id}/balance`)
    return response.data
  },

  getStatements: async (
    id: string, 
    page = 0, 
    size = 20
  ): Promise<PaginatedResponse<unknown>> => {
    const response = await axiosInstance.get(`/api/v1/accounts/${id}/statements`, {
      params: { page, size },
    })
    return response.data
  },

  freeze: async (id: string): Promise<void> => {
    await axiosInstance.put(`/api/v1/accounts/${id}/freeze`)
  },

  unfreeze: async (id: string): Promise<void> => {
    await axiosInstance.put(`/api/v1/accounts/${id}/unfreeze`)
  },

  // Saga balance operations
  reserveBalance: async (data: ReserveBalanceRequest): Promise<void> => {
    await axiosInstance.post(`/api/v1/accounts/${data.accountId}/reserve`, {
      transactionId: data.transactionId,
      amount: data.amount,
    })
  },

  commitBalance: async (data: CommitBalanceRequest): Promise<void> => {
    await axiosInstance.post(`/api/v1/accounts/${data.accountId}/commit`, null, {
      params: { reservationId: data.reservationId },
    })
  },

  rollbackBalance: async (data: RollbackBalanceRequest): Promise<void> => {
    await axiosInstance.post(`/api/v1/accounts/${data.accountId}/rollback`, null, {
      params: { reservationId: data.reservationId },
    })
  },
}
