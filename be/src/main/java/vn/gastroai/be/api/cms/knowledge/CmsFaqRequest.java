package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * UC0062 - Payload tạo/sửa FAQ. published=true bắt buộc phải có medicalReviewer (kiểm tra ở service
 * + CHECK constraint DB). version dùng cho optimistic locking khi sửa.
 */
public record CmsFaqRequest(
        @NotBlank(message = "Câu hỏi không được để trống")
        @Size(max = 500, message = "Câu hỏi tối đa 500 ký tự")
        String question,

        @NotBlank(message = "Câu trả lời không được để trống")
        String answer,

        @NotBlank(message = "Chủ đề không được để trống")
        @Size(max = 80, message = "Chủ đề tối đa 80 ký tự")
        String category,

        @PositiveOrZero(message = "Thứ tự sắp xếp phải lớn hơn hoặc bằng 0")
        Integer sortOrder,

        Boolean published,

        @Size(max = 200, message = "Tên bác sĩ kiểm duyệt tối đa 200 ký tự")
        String medicalReviewer,

        Long version
) {
    public CmsFaqRequest(String question, String answer, String category, Integer sortOrder, Boolean published) {
        this(question, answer, category, sortOrder, published, null, null);
    }
}
