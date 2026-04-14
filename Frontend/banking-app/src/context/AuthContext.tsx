import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import type { User, LoginRequest } from '@/types'
import { authApi } from '@/api'
import { setTokens, clearTokens, getAccessToken } from '@/utils/tokenStorage'
import { FORCE_LOGOUT_EVENT } from '@/api/axios'

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  isLoading: boolean
}

interface AuthContextType extends AuthState {
  login: (credentials: LoginRequest) => Promise<void>
  register: (data: { username: string; email: string; password: string }) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
  })

  // Check for existing session on mount
  useEffect(() => {
    const checkAuth = async () => {
      const token = await getAccessToken()
      if (token) {
        authApi.getMe()
          .then((user) => {
            setState({ user, isAuthenticated: true, isLoading: false })
          })
          .catch(() => {
            clearTokens()
            setState({ user: null, isAuthenticated: false, isLoading: false })
          })
      } else {
        setState((prev) => ({ ...prev, isLoading: false }))
      }
    }
    checkAuth()
  }, [])

  // Listen for force logout events from axios interceptor
  useEffect(() => {
    const handleForceLogout = () => {
      setState({ user: null, isAuthenticated: false, isLoading: false })
    }
    window.addEventListener(FORCE_LOGOUT_EVENT, handleForceLogout)
    return () => window.removeEventListener(FORCE_LOGOUT_EVENT, handleForceLogout)
  }, [])

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials)
    await setTokens(response.accessToken, response.refreshToken)
    setState({ user: response.user, isAuthenticated: true, isLoading: false })
  }, [])

  const register = useCallback(async (data: { username: string; email: string; password: string }) => {
    await authApi.register(data)
    // Auto-login after registration
    await login({ username: data.username, password: data.password })
  }, [login])

  const logout = useCallback(() => {
    authApi.logout().catch(console.error)
    clearTokens()
    setState({ user: null, isAuthenticated: false, isLoading: false })
  }, [])

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
