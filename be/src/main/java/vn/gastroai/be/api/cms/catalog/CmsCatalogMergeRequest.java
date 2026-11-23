package vn.gastroai.be.api.cms.catalog;

import jakarta.validation.constraints.NotNull;

/** UC0060 - Payload gop 2 muc danh muc chuan hoa cung loai thanh 1. */
public record CmsCatalogMergeRequest(
        @NotNull(message = "sourceId không được để trống") Long sourceId,
        @NotNull(message = "targetId không được để trống") Long targetId
) {
}

