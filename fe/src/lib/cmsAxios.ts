import axios from 'axios'
import type { CmsAuthResponse } from '../api/cmsAuth'
import { canAccessCmsPath, isRoleRestrictedCmsPath } from '../components/cms/cmsNav'
import { CMS_AUTH_STORAGE_KEY } from '../stores/cmsAuthStore'
import { CMS_FORBIDDEN_MESSAGE, setCmsFlash } from './cmsFlash'

const baseURL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const cmsApiClient = axios.create({
  baseURL,
  timeout: 60000,
  withCredentials: true,
})

// CMS dùng HttpSession — phiên có thể mất phía BE (restart, hết hạn) trong khi FE vẫn còn
// "nhớ" đã đăng nhập nhờ localStorage (xem cmsAuthStore). Khi đó API báo mất phiên, xoá luôn trạng
// thái đăng nhập cục bộ và đưa về màn login thay vì để trang hiện lỗi chung chung khó hiểu.
// Dùng window.location thay vì useNavigate vì interceptor chạy ngoài cây React component.
// 403 ROLE_FORBIDDEN: lấy lại vai trò; chỉ về /cms khi vai trò đã đổi hoặc trang không còn được phép.
let checkingRole = false

async function handleRoleForbidden() {
  if (checkingRole) return
  checkingRole = true
  try {
    const { data: fresh } = await cmsApiClient.get<CmsAuthResponse>('/cms/auth/me')
    let stored: CmsAuthResponse | null = null
    try {
      const raw = localStorage.getItem(CMS_AUTH_STORAGE_KEY)
      stored = raw ? (JSON.parse(raw) as CmsAuthResponse) : null
      localStorage.setItem(CMS_AUTH_STORAGE_KEY, JSON.stringify(fresh))
    } catch {
      // ignore
    }
    const path = window.location.pathname
    const pageAllowed = !isRoleRestrictedCmsPath(path) || canAccessCmsPath(fresh.userType, path)
    if (stored?.userType === fresh.userType && pageAllowed) return
    setCmsFlash(CMS_FORBIDDEN_MESSAGE)
    window.location.href = '/cms'
  } catch {
    // ignore
  } finally {
    checkingRole = false
  }
}

cmsApiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    const err = error as {
      code?: string
      message?: string
      response?: { status?: number; data?: { code?: string } }
    }
    const status = err?.response?.status
    const errorCode = err.response?.data?.code
    if (status === 403 && errorCode === 'ROLE_FORBIDDEN') {
      void handleRoleForbidden()
    }
    if (status === 401 && window.location.pathname !== '/cms/login') {
      try {
        localStorage.removeItem(CMS_AUTH_STORAGE_KEY)
      } catch {
        // ignore
      }
      window.location.href = '/cms/login'
    }
    if (
      err?.code === 'ECONNABORTED' ||
      (err?.message && err.message.toLowerCase().includes('timeout'))
    ) {
      err.message = 'Máy chủ phản hồi quá thời gian chờ (Timeout). Vui lòng thử lại.'
    }
    return Promise.reject(error)
  },
)
