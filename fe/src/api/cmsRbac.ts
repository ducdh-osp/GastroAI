import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export type CmsPermissionRole = 'ADMIN' | 'CONTENT_EDITOR' | 'CONTENT_AUTHOR' | 'OPERATIONS'

export interface CmsAccount {
  id: number
  email: string
  fullName: string | null
  role: CmsPermissionRole
  createdAt: string
  adminLocked: boolean
}

export interface CmsAccountPage {
  items: CmsAccount[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateCmsAccountRequest {
  fullName: string
  email: string
  role: CmsPermissionRole
  password: string
}

export async function listCmsAccounts(params: { q?: string; page?: number; size?: number } = {}) {
  try {
    const { data } = await cmsApiClient.get<CmsAccountPage>('/cms/rbac/accounts', { params })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách tài khoản CMS.')
  }
}

export async function assignCmsRole(account: CmsAccount, role: CmsPermissionRole) {
  try {
    const { data } = await cmsApiClient.put<CmsAccount>(`/cms/rbac/accounts/${account.id}/role`, { role })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể cập nhật vai trò.')
  }
}

export async function createCmsAccount(request: CreateCmsAccountRequest) {
  try {
    const { data } = await cmsApiClient.post<CmsAccount>('/cms/rbac/accounts', request)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tạo tài khoản.')
  }
}

export async function lockCmsAccount(accountId: number) {
  try {
    const { data } = await cmsApiClient.put<CmsAccount>(`/cms/rbac/accounts/${accountId}/lock`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể khoá tài khoản.')
  }
}

export async function unlockCmsAccount(accountId: number) {
  try {
    const { data } = await cmsApiClient.put<CmsAccount>(`/cms/rbac/accounts/${accountId}/unlock`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể mở khoá tài khoản.')
  }
}
