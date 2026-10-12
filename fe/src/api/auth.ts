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

/** Huỷ token hiện tại phía server (ghi vào revoked_tokens) - BE đọc token từ header
 * Authorization do apiClient tự gắn, không cần truyền gì thêm. */
export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout')
}

export async function register(payload: RegisterPayload): Promise<{ message: string }> {
  const { data } = await apiClient.post<{ message: string }>('/auth/register', payload)
  return data
}

export async function verifyEmail(token: string): Promise<void> {
  await apiClient.post('/auth/verify-email', { token })
}

const verificationRequests = new Map<string, Promise<void>>()
const verifiedTokenHashes = new Set<string>()
const VERIFIED_TOKEN_STORAGE_PREFIX = 'gastroai:verified-email-token:'

async function hashVerificationToken(token: string): Promise<string | null> {
  if (!globalThis.crypto?.subtle) return null
  const digest = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(token))
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('')
}

/** Make verification idempotent in the browser while the BE token remains single-use. */
export async function verifyEmailOnce(token: string): Promise<void> {
  const tokenHash = await hashVerificationToken(token)
  if (tokenHash) {
    if (verifiedTokenHashes.has(tokenHash)) return
    try {
      if (localStorage.getItem(`${VERIFIED_TOKEN_STORAGE_PREFIX}${tokenHash}`) === '1') {
        verifiedTokenHashes.add(tokenHash)
        return
      }
    } catch {
      // Storage can be unavailable in private/restricted browser contexts; in-flight
      // requests are still de-duplicated below for React StrictMode.
    }
    const existingRequest = verificationRequests.get(tokenHash)
    if (existingRequest) return existingRequest

    const request = verifyEmail(token).then(() => {
      verifiedTokenHashes.add(tokenHash)
      try {
        localStorage.setItem(`${VERIFIED_TOKEN_STORAGE_PREFIX}${tokenHash}`, '1')
      } catch {
        // The in-memory marker still covers navigation in this page session.
      }
    })
    verificationRequests.set(tokenHash, request)
    try {
      await request
    } catch (error) {
      verificationRequests.delete(tokenHash)
      throw error
    }
    return
  }

  // Web Crypto is unavailable on some non-secure origins. Prevent duplicate in-flight
  // requests using the token only in memory; never persist the raw verification token.
  const existingRequest = verificationRequests.get(token)
  if (existingRequest) return existingRequest
  const request = verifyEmail(token)
  verificationRequests.set(token, request)
  try {
    await request
  } catch (error) {
    verificationRequests.delete(token)
    throw error
  }
}

export async function resendVerification(email: string): Promise<{ message: string }> {
  const { data } = await apiClient.post<{ message: string }>('/auth/resend-verification', { email })
  return data
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
