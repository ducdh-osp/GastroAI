import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export type CmsAccountType = 'ADMIN' | 'DOCTOR'
export type CmsPermissionRole = 'ADMIN' | 'DOCTOR' | 'CONTENT_EDITOR'

export interface CmsAccount {
  id: number
  accountType: CmsAccountType
  email: string
  fullName: string | null
  role: CmsPermissionRole
  createdAt: string
}

export interface CmsAccountPage {
  items: CmsAccount[]
  page: number
  size: number
  totalElements: number
  totalPages: number
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
    const { data } = await cmsApiClient.put<CmsAccount>(
      `/cms/rbac/accounts/${account.accountType}/${account.id}/role`,
      { role },
    )
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể cập nhật vai trò.')
  }
}
