package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** UC0066 - tao/sua chu de. sortOrder khong gui thi giu nguyen (khi sua) hoac xep cuoi (khi tao). */
public record ArticleTopicRequest(
        @NotBlank @Size(max = 80) String name,
        @PositiveOrZero Integer sortOrder,
        Long version
) {
    public ArticleTopicRequest(String name, Integer sortOrder) {
        this(name, sortOrder, null);
    }
}
