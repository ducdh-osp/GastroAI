import { cmsApiClient } from '../lib/cmsAxios'
import type { LoginHistoryResponse } from './auth'

// Vai trò là thuộc tính của tài khoản, BE tự đọc ra và trả về sau khi đăng nhập.
export type CmsRole = 'ADMIN' | 'CONTENT_EDITOR'

export interface CmsLoginRequest {
  email: string
  password: string
}

// Khớp đúng field name JSON mà CmsAuthResult (BE) trả về — KHÔNG phải id/name/role.
export interface CmsAuthResponse {
  userId: number
  email: string
  fullName: string
  userType: CmsRole
  lockedUntil?: string | null
}

export async function cmsLogin(
  data: CmsLoginRequest
): Promise<CmsAuthResponse> {
  const response = await cmsApiClient.post<CmsAuthResponse>(
    '/cms/auth/login',
    data
  )

  return response.data
}

export async function cmsLogout(): Promise<void> {
  await cmsApiClient.post('/cms/auth/logout')
}

export async function getCmsLoginHistory(page = 0, size = 20): Promise<LoginHistoryResponse> {
  const { data } = await cmsApiClient.get<LoginHistoryResponse>('/cms/auth/login-history', {
    params: { page, size },
  })
  return data
}

export async function changeCmsPassword(payload: { currentPassword: string; newPassword: string }): Promise<void> {
  await cmsApiClient.post('/cms/auth/change-password', payload)
}