package vn.gastroai.be.api.cms.triage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TriageAlertFalseAlarmRequest(
        @NotBlank(message = "Vui lòng nhập lý do")
        @Size(min = 3, max = 1000, message = "Lý do phải từ 3 đến 1000 ký tự")
        String reason
) {
}
