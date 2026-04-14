import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import type { User, LoginRequest } from '@/types'
import { authApi } from '@/api'

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
    const token = localStorage.getItem('accessToken')
    if (token) {
      authApi.getMe()
        .then((user) => {
          setState({ user, isAuthenticated: true, isLoading: false })
        })
        .catch(() => {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          setState({ user: null, isAuthenticated: false, isLoading: false })
        })
    } else {
      setState((prev) => ({ ...prev, isLoading: false }))
    }
  }, [])

  const login = useCallback(async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials)
    localStorage.setItem('accessToken', response.accessToken)
    localStorage.setItem('refreshToken', response.refreshToken)
    setState({ user: response.user, isAuthenticated: true, isLoading: false })
  }, [])

  const register = useCallback(async (data: { username: string; email: string; password: string }) => {
    await authApi.register(data)
    // Auto-login after registration
    await login({ username: data.username, password: data.password })
  }, [login])

  const logout = useCallback(() => {
    authApi.logout().catch(console.error)
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
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
