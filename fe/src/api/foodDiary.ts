import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

export interface FoodDiaryEntry {
  id: number
  eatenAt: string
  description: string
  mealType: MealType
  symptomsAfterMeal: string | null
  symptomOnsetMinutes: number | null
  notes: string | null
}

export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | 'OTHER'

export interface FoodDiaryListResponse {
  items: FoodDiaryEntry[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface FoodDiaryEntryRequest {
  eatenAt: string
  description: string
  mealType: MealType
  symptomsAfterMeal: string | null
  symptomOnsetMinutes: number | null
  notes: string | null
}

export interface DigestiveTimelineItem {
  type: 'MEAL' | 'BRISTOL'
  id: number
  occurredAt: string
  mealType: MealType | null
  description: string | null
  symptomsAfterMeal: string | null
  symptomOnsetMinutes: number | null
  bristolType: number | null
  notes: string | null
}

export interface DailyCountPoint {
  date: string
  count: number
}

export async function listFoodDiary(page = 0, size = 20): Promise<FoodDiaryListResponse> {
  try {
    const { data } = await apiClient.get<FoodDiaryListResponse>('/patient/food-diary', { params: { page, size } })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải nhật ký ăn uống. Vui lòng thử lại.')
  }
}

export async function getFoodDiaryTrend(days = 30): Promise<DailyCountPoint[]> {
  try {
    const { data } = await apiClient.get<{ points: DailyCountPoint[] }>('/patient/food-diary/trend', { params: { days } })
    return data.points
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải xu hướng ăn uống. Vui lòng thử lại.')
  }
}

export async function getDigestiveTimeline(days = 30): Promise<DigestiveTimelineItem[]> {
  try {
    const { data } = await apiClient.get<{ items: DigestiveTimelineItem[] }>('/patient/food-diary/timeline', { params: { days } })
    return data.items
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải timeline tiêu hóa. Vui lòng thử lại.')
  }
}

export async function createFoodDiaryEntry(payload: FoodDiaryEntryRequest): Promise<FoodDiaryEntry> {
  try {
    const { data } = await apiClient.post<FoodDiaryEntry>('/patient/food-diary', payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể thêm nhật ký ăn uống. Vui lòng thử lại.')
  }
}

export async function updateFoodDiaryEntry(id: number, payload: FoodDiaryEntryRequest): Promise<FoodDiaryEntry> {
  try {
    const { data } = await apiClient.put<FoodDiaryEntry>(`/patient/food-diary/${id}`, payload)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể sửa nhật ký ăn uống. Vui lòng thử lại.')
  }
}

export async function deleteFoodDiaryEntry(id: number): Promise<void> {
  try {
    await apiClient.delete(`/patient/food-diary/${id}`)
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể xoá nhật ký ăn uống. Vui lòng thử lại.')
  }
}
