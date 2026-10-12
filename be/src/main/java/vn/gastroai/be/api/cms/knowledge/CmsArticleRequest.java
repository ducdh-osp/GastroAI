package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** UC0065 - Payload tao hoac cap nhat bai viet kien thuc. */
public record CmsArticleRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 120) String slug,
        @NotBlank @Size(max = 500) String summary,
        @NotBlank @Size(max = 80) String category,
        @Min(1) @Max(30) Integer readingMinutes,
        @NotBlank String content,
        @NotBlank @Size(max = 200) String sourceName,
        @NotBlank @Size(max = 1000) String sourceUrl,
        LocalDate sourceCheckedAt,
        Boolean published,
        @Size(max = 200) String medicalReviewer,
        Integer sortOrder
) {
}
