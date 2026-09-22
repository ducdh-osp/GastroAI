import { createContext, useContext, useState, type ReactNode } from 'react'
import type { CmsAuthResponse } from '../api/cmsAuth'

interface CmsAuthState {
  user: CmsAuthResponse | null
  isAuthenticated: boolean
  setUser: (user: CmsAuthResponse) => void
  clearUser: () => void
}

const CmsAuthContext = createContext<CmsAuthState | null>(null)

export function CmsAuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<CmsAuthResponse | null>(null)

  function setUser(user: CmsAuthResponse) {
    setUserState(user)
  }

  function clearUser() {
    setUserState(null)
  }

  return (
    <CmsAuthContext.Provider
      value={{
        user,
        isAuthenticated: user !== null,
        setUser,
        clearUser,
      }}
    >
      {children}
    </CmsAuthContext.Provider>
  )
}

export function useCmsAuth() {
  const ctx = useContext(CmsAuthContext)

  if (!ctx) {
    throw new Error('useCmsAuth phải dùng bên trong CmsAuthProvider')
  }

  return ctx
}

