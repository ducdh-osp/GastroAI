import axios from 'axios'
import { CMS_AUTH_STORAGE_KEY } from '../stores/cmsAuthStore'

const baseURL =
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

export const cmsApiClient = axios.create({
  baseURL,
  timeout: 60000,
  withCredentials: true,
})

// CMS dùng HttpSession — phiên có thể mất phía BE (restart, hết hạn) trong khi FE vẫn còn
// "nhớ" đã đăng nhập nhờ localStorage (xem cmsAuthStore). Khi đó API trả 401, xoá luôn trạng
// thái đăng nhập cục bộ và đưa về màn login thay vì để trang hiện lỗi chung chung khó hiểu.
// Dùng window.location thay vì useNavigate vì interceptor chạy ngoài cây React component.
cmsApiClient.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    const err = error as {
      code?: string
      message?: string
      response?: { status?: number }
    }
    const status = err?.response?.status
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
