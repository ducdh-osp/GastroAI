import { apiClient } from '../lib/axios'

const useMockAuth = import.meta.env.VITE_USE_MOCK_AUTH !== 'false'
const MOCK_USER_KEY = 'gastroai.mock.user'
const MOCK_RESET_TOKEN_KEY = 'gastroai.mock.reset-token'

interface MockUser {
  email: string
  password: string
  fullName: string
  emailVerified: boolean
}

const mockDelay = () => new Promise((resolve) => setTimeout(resolve, 350))

function getMockUser(): MockUser | null {
  const raw = localStorage.getItem(MOCK_USER_KEY)
  return raw ? (JSON.parse(raw) as MockUser) : null
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  fullName: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  patientId: number
  email: string
  fullName: string | null
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  if (useMockAuth) {
    await mockDelay()
    const user = getMockUser() ?? {
      email: 'test@gastroai.vn',
      password: 'Passw0rd!',
      fullName: 'Bệnh nhân Test',
      emailVerified: true,
    }
    if (payload.email.trim().toLowerCase() !== user.email || payload.password !== user.password) {
      throw new Error('Email hoặc mật khẩu không đúng')
    }
    return { token: 'mock-token', patientId: 1, email: user.email, fullName: user.fullName }
  }
  const { data } = await apiClient.post<AuthResponse>('/auth/login', payload)
  return data
}

export async function register(payload: RegisterPayload): Promise<{ message: string }> {
  if (useMockAuth) {
    await mockDelay()
    if (getMockUser()?.email === payload.email.trim().toLowerCase()) {
      throw new Error('Email đã được sử dụng')
    }
    localStorage.setItem(MOCK_USER_KEY, JSON.stringify({
      email: payload.email.trim().toLowerCase(),
      password: payload.password,
      fullName: payload.fullName.trim(),
      emailVerified: true,
    } satisfies MockUser))
    return { message: 'Đăng ký mô phỏng thành công. Bạn có thể đăng nhập.' }
  }
  const { data } = await apiClient.post<{ message: string }>('/auth/register', payload)
  return data
}

export async function verifyEmail(token: string): Promise<void> {
  if (useMockAuth) {
    await mockDelay()
    if (!token) throw new Error('Token xác thực không hợp lệ')
    return
  }
  await apiClient.post('/auth/verify-email', { token })
}

export async function requestPasswordReset(payload: { email: string }): Promise<{ message: string }> {
  if (useMockAuth) {
    await mockDelay()
    const user = getMockUser()
    if (!user || user.email !== payload.email.trim().toLowerCase()) {
      throw new Error('Email không tồn tại trong dữ liệu mô phỏng')
    }
    const token = `mock-reset-${Date.now()}`
    localStorage.setItem(MOCK_RESET_TOKEN_KEY, token)
    return { message: `Đã tạo yêu cầu mô phỏng. Token: ${token}` }
  }
  const { data } = await apiClient.post<{ message: string }>('/auth/forgot-password', payload)
  return data
}

export async function resetPassword(payload: { token: string; newPassword: string }): Promise<void> {
  if (useMockAuth) {
    await mockDelay()
    if (localStorage.getItem(MOCK_RESET_TOKEN_KEY) !== payload.token) {
      throw new Error('Token không hợp lệ hoặc đã hết hạn')
    }
    const user = getMockUser()
    if (!user) throw new Error('Không tìm thấy tài khoản mô phỏng')
    localStorage.setItem(MOCK_USER_KEY, JSON.stringify({ ...user, password: payload.newPassword }))
    localStorage.removeItem(MOCK_RESET_TOKEN_KEY)
    return
  }
  await apiClient.post('/auth/reset-password', payload)
}

// --- Login History ---

export interface LoginHistoryItem {
  id: number
  attemptedAt: string   // ISO 8601 Instant từ BE
  outcome: string       // 'SUCCESS' | 'FAILED' | 'BLOCKED'
  failureReason: string | null
  ipAddress: string | null
  userAgent: string | null
  deviceLabel: string | null
}

export interface LoginHistoryResponse {
  items: LoginHistoryItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

const MOCK_LOGIN_HISTORY: LoginHistoryItem[] = [
  { id: 1, attemptedAt: '2026-09-22T11:42:00Z', outcome: 'SUCCESS', failureReason: null, ipAddress: '192.168.1.12', userAgent: 'Chrome/140', deviceLabel: 'Windows PC' },
  { id: 2, attemptedAt: '2026-09-22T10:15:00Z', outcome: 'SUCCESS', failureReason: null, ipAddress: '113.161.42.18', userAgent: 'Safari Mobile', deviceLabel: 'iPhone 15' },
  { id: 3, attemptedAt: '2026-09-21T16:08:00Z', outcome: 'FAILED', failureReason: 'Sai mật khẩu', ipAddress: '45.77.18.203', userAgent: 'Firefox/141', deviceLabel: null },
  { id: 4, attemptedAt: '2026-09-21T16:07:00Z', outcome: 'BLOCKED', failureReason: 'Quá nhiều lần thất bại', ipAddress: '45.77.18.203', userAgent: 'Firefox/141', deviceLabel: null },
  { id: 5, attemptedAt: '2026-09-20T02:30:00Z', outcome: 'SUCCESS', failureReason: null, ipAddress: '10.0.0.24', userAgent: 'Chrome/140', deviceLabel: 'MacBook Pro' },
]

export async function getLoginHistory(page = 0, size = 20): Promise<LoginHistoryResponse> {
  if (useMockAuth) {
    await mockDelay()
    return { items: MOCK_LOGIN_HISTORY, page: 0, size, totalElements: MOCK_LOGIN_HISTORY.length, totalPages: 1 }
  }
  const { data } = await apiClient.get<LoginHistoryResponse>('/me/login-history', {
    params: { page, size },
  })
  return data
}

