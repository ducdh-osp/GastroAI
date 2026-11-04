package vn.gastroai.be.api.cms.triage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TriageAlertMessageRequest(
        @NotBlank(message = "Vui lòng nhập nội dung tin nhắn")
        @Size(max = 2000, message = "Nội dung tin nhắn tối đa 2000 ký tự")
        String content
) {
}
