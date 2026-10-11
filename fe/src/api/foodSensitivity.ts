import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

/** UC0022 - 1 từ khoá món ăn và mức độ hay đi kèm triệu chứng trong khoảng thời gian phân tích. */
export interface FoodCorrelationItem {
  foodKeyword: string
  timesEaten: number
  timesWithSymptom: number
  correlationPercent: number
}

/** UC0023 - Hồ sơ nhạy cảm thực phẩm cá nhân, tổng hợp từ UC0022. */
export interface FoodSensitivityProfileResponse {
  hasEnoughData: boolean
  totalMealsAnalyzed: number
  daysAnalyzed: number
  baselineSymptomRatePercent: number
  topCorrelations: FoodCorrelationItem[]
}

export async function getFoodSensitivityProfile(days = 90): Promise<FoodSensitivityProfileResponse> {
  try {
    const { data } = await apiClient.get<FoodSensitivityProfileResponse>(
      '/patient/food-diary/sensitivity-profile',
      { params: { days } },
    )
    return data
  } catch (error) {
    throw extractErrorMessage(error, 'Không thể tải hồ sơ nhạy cảm thực phẩm. Vui lòng thử lại.')
  }
}
