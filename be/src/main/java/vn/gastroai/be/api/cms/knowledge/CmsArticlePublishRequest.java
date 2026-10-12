package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.Size;

/** UC0065 - Payload bat/tat xuat ban va cap nhat nguoi duyet y khoa. */
public record CmsArticlePublishRequest(
        boolean published,
        @Size(max = 200) String medicalReviewer
) {
}
