package vn.gastroai.be.api.cms.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateAuditAnomalyRuleRequest(
        @NotNull(message = "Vui lòng nhập ngưỡng")
        @Min(value = 1, message = "Ngưỡng phải từ 1 trở lên")
        @Max(value = 1000, message = "Ngưỡng tối đa là 1000")
        Integer threshold,

        @NotNull(message = "Vui lòng nhập số phút")
        @Min(value = 1, message = "Khoảng thời gian phải từ 1 phút trở lên")
        @Max(value = 1440, message = "Khoảng thời gian tối đa là 1440 phút (1 ngày)")
        Integer windowMinutes,

        @NotNull(message = "Thiếu trạng thái bật/tắt")
        Boolean enabled
) {
}