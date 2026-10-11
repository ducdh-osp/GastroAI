package vn.gastroai.be.api.catalog;

import vn.gastroai.be.domain.catalog.CatalogItem;
import vn.gastroai.be.domain.catalog.CatalogType;

public record CatalogItemResponse(Long id, CatalogType type, String code, String name, String triageCode) {
    public static CatalogItemResponse from(CatalogItem item) {
        return new CatalogItemResponse(item.getId(), item.getType(), item.getCode(), item.getName(), item.getTriageCode());
    }
}
