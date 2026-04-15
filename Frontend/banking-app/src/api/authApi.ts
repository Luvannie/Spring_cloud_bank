import { axiosInstance } from './axios'
import type { 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest, 
  User 
} from '@/types'

export const authApi = {
  login: async (data: LoginRequest): Promise<LoginResponse> => {
    const response = await axiosInstance.post<LoginResponse>('/api/v1/auth/login', data)
    return response.data
  },

  register: async (data: RegisterRequest): Promise<User> => {
    const response = await axiosInstance.post<User>('/api/v1/auth/register', data)
    return response.data
  },

  refresh: async (refreshToken: string): Promise<LoginResponse> => {
    const response = await axiosInstance.post<LoginResponse>('/api/v1/auth/refresh', { refreshToken })
    return response.data
  },

  logout: async (): Promise<void> => {
    await axiosInstance.post('/api/v1/auth/logout')
  },

  getMe: async (): Promise<User> => {
    const response = await axiosInstance.get<User>('/api/v1/auth/me')
    return response.data
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await axiosInstance.put('/api/v1/auth/password', { currentPassword, newPassword })
  },
}
