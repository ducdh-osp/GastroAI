import { create } from 'zustand'
import type { CmsAuthResponse } from '../api/cmsAuth'

interface CmsAuthState {
  user: CmsAuthResponse | null
  setUser: (user: CmsAuthResponse) => void
  clearUser: () => void
}

export const useCmsAuthStore = create<CmsAuthState>((set) => ({
  user: null,

  setUser: (user) => {
    set({ user })
  },

  clearUser: () => {
    set({ user: null })
  },
}))