package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** UC0065 - Payload tao hoac cap nhat bai viet kien thuc. */
public record CmsArticleRequest(
        @NotBlank(message = "Tiêu đề không được để trống") @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự") String title,
        @Size(max = 120, message = "Slug tối đa 120 ký tự") String slug,
        @NotBlank(message = "Tóm tắt không được để trống") @Size(max = 500, message = "Tóm tắt tối đa 500 ký tự") String summary,
        @NotBlank(message = "Chủ đề không được để trống") @Size(max = 80, message = "Chủ đề tối đa 80 ký tự") String category,
        @Min(1) @Max(30) Integer readingMinutes,
        @NotBlank(message = "Nội dung bài viết không được để trống") String content,
        @NotBlank(message = "Tên nguồn không được để trống") @Size(max = 200, message = "Tên nguồn tối đa 200 ký tự") String sourceName,
        @NotBlank(message = "Đường dẫn nguồn không được để trống")
        @Pattern(regexp = "^https?://.+", message = "Đường dẫn nguồn phải là URL hợp lệ bắt đầu bằng http:// hoặc https://")
        @Size(max = 1000, message = "Đường dẫn nguồn tối đa 1000 ký tự")
        String sourceUrl,
        @PastOrPresent(message = "Ngày kiểm tra nguồn không được lớn hơn thời điểm hiện tại")
        LocalDate sourceCheckedAt,
        Boolean published,
        @Size(max = 200, message = "Tên bác sĩ kiểm duyệt tối đa 200 ký tự") String medicalReviewer,
        @PositiveOrZero(message = "Thứ tự sắp xếp phải lớn hơn hoặc bằng 0") Integer sortOrder,
        Long version
) {
    public CmsArticleRequest(
            String title, String slug, String summary, String category,
            Integer readingMinutes, String content, String sourceName,
            String sourceUrl, LocalDate sourceCheckedAt, Boolean published,
            String medicalReviewer, Integer sortOrder
    ) {
        this(title, slug, summary, category, readingMinutes, content, sourceName, sourceUrl, sourceCheckedAt, published, medicalReviewer, sortOrder, null);
    }
}
