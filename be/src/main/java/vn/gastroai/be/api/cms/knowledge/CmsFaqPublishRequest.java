package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.NotNull;

public record CmsFaqPublishRequest(
        @NotNull(message = "Trạng thái published không được để trống")
        Boolean published
) {
}
