import { apiClient } from '../lib/axios'

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
  const { data } = await apiClient.post<AuthResponse>('/auth/login', payload)
  return data
}

export async function register(payload: RegisterPayload): Promise<{ message: string }> {
  const { data } = await apiClient.post<{ message: string }>('/auth/register', payload)
  return data
}

export async function verifyEmail(token: string): Promise<void> {
  await apiClient.post('/auth/verify-email', { token })
}

export async function requestPasswordReset(payload: { email: string }): Promise<{ message: string }> {
  const { data } = await apiClient.post<{ message: string }>('/auth/forgot-password', payload)
  return data
}

export async function resetPassword(payload: { token: string; newPassword: string }): Promise<void> {
  await apiClient.post('/auth/reset-password', payload)
}

export async function changePassword(payload: { currentPassword: string; newPassword: string }): Promise<void> {
  await apiClient.post('/auth/change-password', payload)
}

// --- Login History ---

export interface LoginHistoryItem {
  id: number
  attemptedAt: string   // ISO 8601 Instant từ BE
  outcome: string       // 'SUCCESS' | 'FAILURE' | 'BLOCKED'
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
  // Đếm trên TOÀN BỘ lịch sử (7 ngày gần nhất), không phải chỉ "items" của trang đang xem —
  // dùng field này để cảnh báo bảo mật, không tự đếm items (sẽ bỏ sót nếu lần thất bại nằm
  // ở trang khác).
  recentFailureCount: number
}

export async function getLoginHistory(page = 0, size = 20): Promise<LoginHistoryResponse> {
  const { data } = await apiClient.get<LoginHistoryResponse>('/me/login-history', {
    params: { page, size },
  })
  return data
}
