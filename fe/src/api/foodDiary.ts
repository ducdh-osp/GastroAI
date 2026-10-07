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

export interface DailyCountPoint {
  date: string
  count: number
}

/** UC0020 - 1 muc nhat ky an uong dang nam trong thung rac. */
export interface FoodDiaryTrashItem {
  entry: FoodDiaryEntry
  deletedAt: string
  purgeAt: string
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

/** UC0020 - danh sach cac muc dang nam trong thung rac (con trong han 30 ngay). */
export async function listFoodDiaryTrash(): Promise<FoodDiaryTrashItem[]> {
  try {
    const { data } = await apiClient.get<FoodDiaryTrashItem[]>('/patient/food-diary/trash')
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải thùng rác. Vui lòng thử lại.')
  }
}

/** UC0020 - khoi phuc 1 muc tu thung rac. */
export async function restoreFoodDiaryEntry(id: number): Promise<FoodDiaryEntry> {
  try {
    const { data } = await apiClient.post<FoodDiaryEntry>(`/patient/food-diary/${id}/restore`)
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể khôi phục. Vui lòng thử lại.')
  }
}