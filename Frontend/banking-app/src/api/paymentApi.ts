import { axiosInstance } from './axios'
import type { Payment, PaymentLinkRequest, PaymentLinkResponse } from '@/types'

export const paymentApi = {
  createPaymentLink: async (data: PaymentLinkRequest): Promise<PaymentLinkResponse> => {
    const response = await axiosInstance.post<PaymentLinkResponse>('/api/v1/payments/links', data)
    return response.data
  },

  getById: async (id: string): Promise<Payment> => {
    const response = await axiosInstance.get<Payment>(`/api/v1/payments/${id}`)
    return response.data
  },

  getByTransactionId: async (transactionId: string): Promise<Payment> => {
    const response = await axiosInstance.get<Payment>(`/api/v1/payments/transaction/${transactionId}`)
    return response.data
  },
}
