import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useCmsAuth } from '../stores/cmsAuthStore'

export function CmsProtectedRoute({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useCmsAuth()
  if (!isAuthenticated) return <Navigate to="/cms/login" replace />
  return children
}
