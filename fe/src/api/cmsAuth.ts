import { cmsApiClient } from '../lib/cmsAxios'

export interface CmsLoginPayload {
  email: string
  password: string
}

export interface CmsAuthResponse {
  userId: number
  email: string
  fullName: string | null
  userType: 'ADMIN' | 'DOCTOR'
}

export async function cmsLogin(
  payload: CmsLoginPayload,
): Promise<CmsAuthResponse> {
  const { data } = await cmsApiClient.post<CmsAuthResponse>(
    '/cms/auth/login',
    payload,
  )

  return data
}