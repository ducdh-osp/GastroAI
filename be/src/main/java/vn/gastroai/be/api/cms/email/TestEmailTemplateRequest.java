package vn.gastroai.be.api.cms.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestEmailTemplateRequest(
        @NotBlank(message = "Email nhận không được để trống")
        @Email(message = "Email nhận không hợp lệ")
        @Size(max = 255, message = "Email nhận tối đa 255 ký tự")
        String recipientEmail) {
}
