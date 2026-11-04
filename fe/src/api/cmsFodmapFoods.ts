import { cmsApiClient } from '../lib/cmsAxios'
import { extractErrorMessage } from '../lib/errors'

export type FodmapLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export type FodmapGroup = 'FRUCTAN' | 'GOS' | 'LACTOSE' | 'FRUCTOSE' | 'POLYOL'

export const FODMAP_GROUPS: FodmapGroup[] = ['FRUCTAN', 'GOS', 'LACTOSE', 'FRUCTOSE', 'POLYOL']

export interface CmsFodmapFood {
  id: number
  name: string
  aliases: string[]
  level: FodmapLevel
  levelLabel: string
  groups: FodmapGroup[]
  servingNote: string | null
  note: string | null
  source: string
  active: boolean
  updatedAt: string
}

export interface CmsFodmapFoodRequest {
  name: string
  aliases: string[]
  level: FodmapLevel
  groups: FodmapGroup[]
  servingNote?: string | null
  note?: string | null
  source: string
}

export interface CmsFodmapFoodFilter {
  q?: string
  level?: FodmapLevel | null
  active?: boolean | null
}

export interface CmsFodmapUnknownName {
  id: number
  name: string
  hitCount: number
  firstSeenAt: string
  lastSeenAt: string
}

const BASE = '/cms/fodmap-foods'

export async function listCmsFodmapFoods(filter?: CmsFodmapFoodFilter): Promise<CmsFodmapFood[]> {
  try {
    const params: Record<string, string | boolean> = {}
    if (filter?.q && filter.q.trim()) params.q = filter.q.trim()
    if (filter?.level) params.level = filter.level
    if (filter?.active !== undefined && filter?.active !== null) params.active = filter.active

    const { data } = await cmsApiClient.get<CmsFodmapFood[]>(BASE, { params })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh mục FODMAP.')
  }
}

export async function createCmsFodmapFood(payload: CmsFodmapFoodRequest): Promise<CmsFodmapFood> {
  try {
    const { data } = await cmsApiClient.post<CmsFodmapFood>(BASE, payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể thêm món FODMAP.')
  }
}

export async function updateCmsFodmapFood(id: number, payload: CmsFodmapFoodRequest): Promise<CmsFodmapFood> {
  try {
    const { data } = await cmsApiClient.put<CmsFodmapFood>(`${BASE}/${id}`, payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể cập nhật món FODMAP.')
  }
}

export async function toggleCmsFodmapFoodActive(id: number): Promise<CmsFodmapFood> {
  try {
    const { data } = await cmsApiClient.patch<CmsFodmapFood>(`${BASE}/${id}/toggle-active`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể thay đổi trạng thái món FODMAP.')
  }
}

export async function deleteCmsFodmapFood(id: number): Promise<void> {
  try {
    await cmsApiClient.delete(`${BASE}/${id}`)
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể xoá món FODMAP.')
  }
}

export async function listCmsFodmapUnknownNames(): Promise<CmsFodmapUnknownName[]> {
  try {
    const { data } = await cmsApiClient.get<CmsFodmapUnknownName[]>(`${BASE}/unknown-names`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách tên chưa có dữ liệu.')
  }
}

export async function dismissCmsFodmapUnknownName(id: number): Promise<CmsFodmapUnknownName> {
  try {
    const { data } = await cmsApiClient.patch<CmsFodmapUnknownName>(`${BASE}/unknown-names/${id}/dismiss`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể bỏ qua tên này.')
  }
}