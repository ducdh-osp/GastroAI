package vn.gastroai.be.api.cms.catalog;

import vn.gastroai.be.domain.catalog.CatalogItem;
import vn.gastroai.be.domain.catalog.CatalogType;

/** UC0060 - Response tra ve danh muc chuan hoa cho CMS. */
public record CmsCatalogItemResponse(
        Long id,
        CatalogType type,
        String code,
        String name,
        String triageCode,
        Integer sortOrder,
        boolean active
) {
    public static CmsCatalogItemResponse from(CatalogItem item) {
        return new CmsCatalogItemResponse(
                item.getId(),
                item.getType(),
                item.getCode(),
                item.getName(),
                item.getTriageCode(),
                item.getSortOrder(),
                item.isActive()
        );
    }
}
