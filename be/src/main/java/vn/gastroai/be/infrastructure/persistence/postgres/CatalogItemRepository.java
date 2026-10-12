package vn.gastroai.be.infrastructure.persistence.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.gastroai.be.domain.catalog.CatalogItem;
import vn.gastroai.be.domain.catalog.CatalogType;

import java.util.List;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    List<CatalogItem> findByTypeAndActiveTrueOrderBySortOrderAscNameAsc(CatalogType type);
}
