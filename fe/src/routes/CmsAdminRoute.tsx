import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useCmsAuth } from '../stores/cmsAuthStore'

/** Bao ve UX cho cac trang chi danh cho ADMIN; backend van kiem tra quyen bat buoc. */
export function CmsAdminRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated, user } = useCmsAuth()
  if (!isAuthenticated) return <Navigate to="/cms/login" replace />
  if (user?.userType !== 'ADMIN') return <Navigate to="/cms" replace />
  return children
}
