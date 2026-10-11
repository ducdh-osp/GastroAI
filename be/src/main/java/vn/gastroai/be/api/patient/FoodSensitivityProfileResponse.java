package vn.gastroai.be.api.patient;

import java.util.List;

/**
 * UC0023 - Hồ sơ nhạy cảm thực phẩm cá nhân, tổng hợp từ kết quả phân tích tương quan UC0022.
 *
 * hasEnoughData=false khi bệnh nhân chưa ghi đủ số bữa ăn để phân tích có ý nghĩa - lúc đó
 * topCorrelations rỗng, FE nên hiện thông báo "chưa đủ dữ liệu" thay vì bảng trống gây hiểu
 * lầm là "không có món nào liên quan".
 *
 * baselineSymptomRatePercent là % bữa ăn có triệu chứng đi kèm TÍNH TRÊN TOÀN BỘ dữ liệu đã
 * phân tích, dùng để FE/bác sĩ so sánh - 1 món có correlationPercent chỉ cao hơn baseline
 * một chút thì không đáng chú ý bằng món cao hơn hẳn baseline.
 */
public record FoodSensitivityProfileResponse(
        boolean hasEnoughData,
        int totalMealsAnalyzed,
        int daysAnalyzed,
        double baselineSymptomRatePercent,
        List<FoodCorrelationItem> topCorrelations) {
}
