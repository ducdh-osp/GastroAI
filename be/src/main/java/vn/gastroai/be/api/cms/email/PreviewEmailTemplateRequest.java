package vn.gastroai.be.api.cms.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.gastroai.be.domain.notification.EmailTemplateCode;

public record PreviewEmailTemplateRequest(
        @NotNull(message = "Mã mẫu không được để trống") EmailTemplateCode code,
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự") String subjectTemplate,
        @NotBlank(message = "Nội dung HTML không được để trống")
        @Size(max = 100000, message = "Nội dung HTML tối đa 100.000 ký tự") String htmlBodyTemplate,
        @NotBlank(message = "Nội dung text không được để trống")
        @Size(max = 100000, message = "Nội dung text tối đa 100.000 ký tự") String textBodyTemplate) {
}
