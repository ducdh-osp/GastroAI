package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CmsFaqRequest(
        @NotBlank(message = "Câu hỏi không được để trống")
        @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
        String question,

        @NotBlank(message = "Câu trả lời không được để trống")
        String answer,

        @NotBlank(message = "Chủ đề không được để trống")
        @Size(max = 80, message = "Chủ đề tối đa 80 ký tự")
        String category,

        Integer sortOrder,

        Boolean published
) {
}
