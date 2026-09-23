import { createContext, useContext, useState, type ReactNode } from 'react'
import { cmsLogout, type CmsAuthResponse } from '../api/cmsAuth'

interface CmsAuthState {
  user: CmsAuthResponse | null
  isAuthenticated: boolean
  setUser: (user: CmsAuthResponse) => void
  clearUser: () => void
  logout: () => void
}

// Export để cmsAxios.ts (interceptor 401) xoá đúng key khi session BE hết hạn/mất mà
// không cần đi qua React context — interceptor chạy ngoài cây component.
export const CMS_AUTH_STORAGE_KEY = 'gastroai.cms.auth'
const STORAGE_KEY = CMS_AUTH_STORAGE_KEY

const CmsAuthContext = createContext<CmsAuthState | null>(null)

function loadStored(): CmsAuthResponse | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as CmsAuthResponse) : null
  } catch {
    return null
  }
}

export function CmsAuthProvider({ children }: { children: ReactNode }) {
  const [user, setUserState] = useState<CmsAuthResponse | null>(loadStored)

  function setUser(user: CmsAuthResponse) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    } catch {
      // Trình duyệt chặn storage (chế độ riêng tư...) — bỏ qua, chỉ giữ trong bộ nhớ.
    }
    setUserState(user)
  }

  function clearUser() {
    try {
      localStorage.removeItem(STORAGE_KEY)
    } catch {
      // ignore
    }
    setUserState(null)
  }

  function logout() {
    // Gọi BE để huỷ session (cookie) — không chặn việc xoá state phía client dù request lỗi.
    void cmsLogout().catch(() => {})
    clearUser()
  }

  return (
    <CmsAuthContext.Provider
      value={{
        user,
        isAuthenticated: user !== null,
        setUser,
        clearUser,
        logout,
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
