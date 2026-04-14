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

  freeze: async (id: string): Promise<Account> => {
    const response = await axiosInstance.put<Account>(`/api/v1/accounts/${id}/freeze`)
    return response.data
  },

  unfreeze: async (id: string): Promise<Account> => {
    const response = await axiosInstance.put<Account>(`/api/v1/accounts/${id}/unfreeze`)
    return response.data
  },

  // Saga balance operations
  reserveBalance: async (data: ReserveBalanceRequest): Promise<void> => {
    await axiosInstance.post('/api/v1/accounts/reserve', data)
  },

  commitBalance: async (data: CommitBalanceRequest): Promise<void> => {
    await axiosInstance.post('/api/v1/accounts/commit', data)
  },

  rollbackBalance: async (data: RollbackBalanceRequest): Promise<void> => {
    await axiosInstance.post('/api/v1/accounts/rollback', data)
  },
}
