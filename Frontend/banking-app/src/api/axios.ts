import axios from 'axios'
import { getAccessToken, clearTokens } from '@/utils/tokenStorage'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

// Custom event for forced logout (works with React Router)
export const FORCE_LOGOUT_EVENT = 'banking:forceLogout'

export const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000, // 30 second timeout
  timeoutErrorMessage: 'Request timed out. Please try again.',
})

// Request interceptor - add auth token
axiosInstance.interceptors.request.use(
  async (config) => {
    const token = await getAccessToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor - handle token refresh
axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    // If 401 and not already retrying
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const refreshToken = localStorage.getItem('refreshToken')
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/api/v1/auth/refresh`, {
            refreshToken,
          })

          const { accessToken } = response.data
          // Re-encrypt the new tokens
          const { setTokens } = await import('@/utils/tokenStorage')
          await setTokens(accessToken, refreshToken)

          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return axiosInstance(originalRequest)
        }
      } catch {
        // Refresh failed - dispatch logout event instead of window.location
        clearTokens()
        window.dispatchEvent(new CustomEvent(FORCE_LOGOUT_EVENT))
      }
    }

    return Promise.reject(error)
  }
)

// Helper to trigger logout from outside interceptors
export function triggerForceLogout() {
  clearTokens()
  window.dispatchEvent(new CustomEvent(FORCE_LOGOUT_EVENT))
}
