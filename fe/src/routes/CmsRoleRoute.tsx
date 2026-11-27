import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { canAccessCmsPath } from '../components/cms/cmsNav'
import { useCmsAuth } from '../stores/cmsAuthStore'
import { CMS_FORBIDDEN_MESSAGE, setCmsFlash } from '../lib/cmsFlash'

/**
 * Chặn route CMS theo vai trò. `path` là đường dẫn của mục menu tương ứng trong CMS_NAV_GROUPS
 * (route con truyền path của mục cha, vd "/cms/articles" cho "/cms/articles/:id/edit") — quyền tra
 * từ cmsNav.ts nên route và menu luôn khớp. Đây là bảo vệ UX; backend vẫn kiểm tra quyền bắt buộc.
 */
export function CmsRoleRoute({ path, children }: { path: string; children: ReactNode }) {
  const { isAuthenticated, user } = useCmsAuth()
  if (!isAuthenticated) return <Navigate to="/cms/login" replace />
  if (!canAccessCmsPath(user?.userType, path)) {
    setCmsFlash(CMS_FORBIDDEN_MESSAGE)
    return <Navigate to="/cms" replace />
  }
  return children
}
