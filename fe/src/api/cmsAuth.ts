import { cmsApiClient } from '../lib/cmsAxios'

export type CmsRole = 'ADMIN' | 'DOCTOR'

export interface CmsLoginRequest {
  email: string
  password: string
  role: CmsRole
}

export interface CmsAuthResponse {
  id: number
  email: string
  name: string
  role: CmsRole
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