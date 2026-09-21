import { createContext, useContext, useState, type ReactNode } from 'react'
import { login as loginApi, type LoginPayload } from '../api/auth'

interface AuthState {
  token: string | null
  email: string | null
  fullName: string | null
  isAuthenticated: boolean
  login: (payload: LoginPayload) => Promise<void>
  logout: () => void
}

const STORAGE_KEY = 'gastroai.auth'

const AuthContext = createContext<AuthState | null>(null)

function loadStored() {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return { token: null, email: null, fullName: null }
  try {
    return JSON.parse(raw) as { token: string; email: string; fullName: string | null }
  } catch {
    return { token: null, email: null, fullName: null }
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState(loadStored)

  async function login(payload: LoginPayload) {
    const result = await loginApi(payload)
    const next = { token: result.token, email: result.email, fullName: result.fullName }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    setState(next)
  }

  function logout() {
    localStorage.removeItem(STORAGE_KEY)
    setState({ token: null, email: null, fullName: null })
  }

  return (
    <AuthContext.Provider
      value={{ ...state, isAuthenticated: state.token !== null, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth phải dùng bên trong AuthProvider')
  return ctx
}
