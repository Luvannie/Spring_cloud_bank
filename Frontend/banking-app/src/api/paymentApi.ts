import { axiosInstance } from './axios'
import type { Payment, PaymentLinkRequest, PaymentLinkResponse } from '@/types'

export const paymentApi = {
  createPaymentLink: async (data: PaymentLinkRequest): Promise<PaymentLinkResponse> => {
    const response = await axiosInstance.post<PaymentLinkResponse>('/api/v1/payments/link', data)
    return response.data
  },

  getById: async (id: string): Promise<Payment> => {
    const response = await axiosInstance.get<Payment>(`/api/v1/payments/${id}`)
    return response.data
  },

  getStatus: async (id: string): Promise<{ status: string }> => {
    const response = await axiosInstance.get(`/api/v1/payments/${id}/status`)
    return response.data
  },

  cancel: async (id: string): Promise<Payment> => {
    const response = await axiosInstance.post<Payment>(`/api/v1/payments/${id}/cancel`)
    return response.data
  },
}
