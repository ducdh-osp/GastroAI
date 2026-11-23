import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export interface CmsPatient {
  id: number
  email: string
  fullName: string | null
  phone: string | null
  emailVerified: boolean
  adminLocked: boolean
  adminLockedAt: string | null
  lockedUntil: string | null
  createdAt: string
}

export interface CmsPatientPage {
  items: CmsPatient[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listCmsPatients(params: { q?: string; locked?: boolean; page?: number; size?: number } = {}) {
  try {
    const { data } = await cmsApiClient.get<CmsPatientPage>('/cms/users', { params })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách bệnh nhân.')
  }
}

export function setCmsPatientLock(id: number, locked: true, reason: string): Promise<CmsPatient>
export function setCmsPatientLock(id: number, locked: false): Promise<CmsPatient>
export async function setCmsPatientLock(id: number, locked: boolean, reason?: string): Promise<CmsPatient> {
  try {
    const { data } = await cmsApiClient.post<CmsPatient>(
      `/cms/users/${id}/${locked ? 'lock' : 'unlock'}`,
      locked ? { reason: reason?.trim() } : undefined,
    )
    return data
  } catch (error) {
    throw extractErrorMessage(error, locked ? 'Không thể khóa tài khoản.' : 'Không thể mở khóa tài khoản.')
  }
}
