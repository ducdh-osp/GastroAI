import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

/** UC0025 - 1 ngày trong cửa sổ theo dõi: có ghi nhật ký (ăn uống hoặc Bristol) hay không. */
export interface AdherenceDayPoint {
  date: string
  foodDiaryCount: number
  bristolCount: number
  logged: boolean
}

/** UC0025 - tiến độ và mức độ duy trì thói quen ghi nhật ký. */
export interface AdherenceResponse {
  windowDays: number
  currentStreak: number
  longestStreak: number
  loggedDays: number
  totalDays: number
  adherencePercent: number
  loggedToday: boolean
  days: AdherenceDayPoint[]
}

export async function getAdherence(days = 30): Promise<AdherenceResponse> {
  try {
    const { data } = await apiClient.get<AdherenceResponse>('/patient/adherence', { params: { days } })
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải tiến độ theo dõi sức khỏe. Vui lòng thử lại.')
  }
}
