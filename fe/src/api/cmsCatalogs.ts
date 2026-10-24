import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export type CatalogType = 'SYMPTOM' | 'CONDITION' | 'MEDICATION'

export interface CmsCatalogItem {
  id: number
  type: CatalogType
  code: string
  name: string
  triageCode: string | null
  sortOrder: number
  active: boolean
}

export interface CmsCatalogItemRequest {
  type: CatalogType
  code: string
  name: string
  triageCode?: string | null
  sortOrder?: number
  active?: boolean
}

export interface CmsCatalogFilter {
  type?: CatalogType | null
  q?: string
  active?: boolean | null
}

export async function listCmsCatalogs(filter?: CmsCatalogFilter): Promise<CmsCatalogItem[]> {
  try {
    const params: Record<string, string | boolean> = {}
    if (filter?.type) params.type = filter.type
    if (filter?.q && filter.q.trim()) params.q = filter.q.trim()
    if (filter?.active !== undefined && filter?.active !== null) params.active = filter.active

    const { data } = await cmsApiClient.get<CmsCatalogItem[]>('/cms/catalogs', { params })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách danh mục chuẩn hóa.')
  }
}

export async function getCmsCatalog(id: number): Promise<CmsCatalogItem> {
  try {
    const { data } = await cmsApiClient.get<CmsCatalogItem>(`/cms/catalogs/${id}`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải chi tiết danh mục chuẩn hóa.')
  }
}

export async function createCmsCatalog(payload: CmsCatalogItemRequest): Promise<CmsCatalogItem> {
  try {
    const { data } = await cmsApiClient.post<CmsCatalogItem>('/cms/catalogs', payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tạo mới danh mục chuẩn hóa.')
  }
}

export async function updateCmsCatalog(id: number, payload: CmsCatalogItemRequest): Promise<CmsCatalogItem> {
  try {
    const { data } = await cmsApiClient.put<CmsCatalogItem>(`/cms/catalogs/${id}`, payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể cập nhật danh mục chuẩn hóa.')
  }
}

export async function toggleCmsCatalogActive(id: number): Promise<CmsCatalogItem> {
  try {
    const { data } = await cmsApiClient.patch<CmsCatalogItem>(`/cms/catalogs/${id}/toggle-active`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể thay đổi trạng thái danh mục.')
  }
}

export async function deleteCmsCatalog(id: number): Promise<void> {
  try {
    await cmsApiClient.delete(`/cms/catalogs/${id}`)
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể xóa danh mục chuẩn hóa này.')
  }
}
