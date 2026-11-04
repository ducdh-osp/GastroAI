import { apiClient } from '../lib/axios'
import { extractErrorMessage } from '../lib/errors'

export type FodmapLevel = 'LOW' | 'MODERATE' | 'HIGH' | 'UNKNOWN'

export interface FoodVisionIngredient {
  detectedName: string
  normalizedName: string | null
  /** Độ tin cậy trong khoảng 0..1; null khi model không cung cấp. */
  confidence: number | null
  fodmapLevel: FodmapLevel
  servingSize: string | null
  matchedCatalogId: number | null
}

export interface FoodVisionAnalysis {
  dishName: string
  /** Độ tin cậy trong khoảng 0..1; null khi model không cung cấp. */
  confidence: number | null
  overallFodmapLevel: FodmapLevel
  ingredients: FoodVisionIngredient[]
  warnings: string[]
}

/**
 * Gửi một ảnh món ăn tới endpoint tổng hợp. BE chịu trách nhiệm nhận diện bằng
 * Vision rồi đối chiếu dữ liệu FODMAP; FE không tự suy luận mức FODMAP.
 */
export async function analyzeFoodImage(image: File, signal?: AbortSignal): Promise<FoodVisionAnalysis> {
  const formData = new FormData()
  formData.append('image', image)

  try {
    const { data } = await apiClient.post<FoodVisionAnalysis>(
      '/patient/food-vision/analyze',
      formData,
      {
        signal,
        // Gemini Vision có thể cần lâu hơn request CRUD thông thường. Chỉ tăng
        // timeout cho request này, không tác động cấu hình chung của ứng dụng.
        timeout: 90_000,
      },
    )
    return data
  } catch (error) {
    if (signal?.aborted) throw error
    throw extractErrorMessage(error, 'Không thể phân tích ảnh món ăn. Vui lòng thử lại.')
  }
}
