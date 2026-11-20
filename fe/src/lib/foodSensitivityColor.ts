import type { FoodCorrelationItem } from '../api/foodSensitivity'

/** Cao hơn tỷ lệ nền bao nhiêu điểm % thì coi là "đáng chú ý" - chỉ để tô màu gợi ý, không
 * phải ngưỡng thống kê chặt chẽ (UC0022 là phân tích đơn giản, không phải chẩn đoán). */
const NOTABLE_GAP_POINTS = 20

// Dùng chung cho FoodSensitivityProfilePage và biểu đồ rút gọn trên Trang chủ.
export function foodSensitivityColor(item: FoodCorrelationItem, baseline: number): string {
  const gap = item.correlationPercent - baseline
  if (gap >= NOTABLE_GAP_POINTS) return '#cf1322' // đỏ - cao hơn hẳn mức nền
  if (gap > 0) return '#0d9488' // teal (màu chủ đạo) - hơi cao hơn mức nền
  return '#8c8c8c' // xám - không cao hơn mức nền
}
