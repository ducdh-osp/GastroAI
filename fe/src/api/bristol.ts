import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

export interface BristolLog {
  id: number
  loggedAt: string
  bristolType: number
  notes: string | null
}

export interface BristolListResponse {
  items: BristolLog[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface BristolLogRequest {
  loggedAt: string
  bristolType: number
  notes: string | null
}

export interface BristolLogPoint {
  loggedAt: string
  bristolType: number
}

export async function listBristolLogs(page = 0, size = 20): Promise<BristolListResponse> {
  try {
    const { data } = await apiClient.get<BristolListResponse>('/patient/bristol-logs', { params: { page, size } })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải danh sách đã ghi nhận. Vui lòng thử lại.')
  }
}

export async function getBristolTrend(days = 30): Promise<BristolLogPoint[]> {
  try {
    const { data } = await apiClient.get<{ points: BristolLogPoint[] }>('/patient/bristol-logs/trend', { params: { days } })
    return data.points
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải xu hướng Bristol. Vui lòng thử lại.')
  }
}

export async function createBristolLog(payload: BristolLogRequest): Promise<BristolLog> {
  try {
    const { data } = await apiClient.post<BristolLog>('/patient/bristol-logs', payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể ghi nhận tình trạng tiêu hóa. Vui lòng thử lại.')
  }
}

export async function updateBristolLog(id: number, payload: BristolLogRequest): Promise<BristolLog> {
  try {
    const { data } = await apiClient.put<BristolLog>(`/patient/bristol-logs/${id}`, payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể sửa mục đã ghi nhận. Vui lòng thử lại.')
  }
}

export async function deleteBristolLog(id: number): Promise<void> {
  try {
    await apiClient.delete(`/patient/bristol-logs/${id}`)
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể xoá mục đã ghi nhận. Vui lòng thử lại.')
  }
}
