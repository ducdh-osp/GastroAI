package vn.gastroai.be.application.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.catalog.CmsCatalogItemRequest;
import vn.gastroai.be.api.cms.catalog.CmsCatalogItemResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.catalog.CatalogItem;
import vn.gastroai.be.domain.catalog.CatalogType;
import vn.gastroai.be.infrastructure.persistence.postgres.CatalogItemRepository;

import java.util.List;
import java.util.Locale;

/**
 * UC0060 - Nghiep vu quan ly danh muc chuan hoa (trieu chung, benh ly, thuoc) cho Admin/Bac si.
 */
@Service
public class CmsCatalogAdminService {

    private static final String TX = "postgresTransactionManager";
    private static final int SORT_STEP = 10;

    private final CatalogItemRepository catalogItemRepository;

    public CmsCatalogAdminService(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    @Transactional(value = TX, readOnly = true)
    public List<CmsCatalogItemResponse> list(CatalogType type, String query, Boolean active) {
        List<CatalogItem> items = type != null
                ? catalogItemRepository.findByTypeOrderBySortOrderAscNameAsc(type)
                : catalogItemRepository.findAllByOrderByTypeAscSortOrderAscNameAsc();

        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : null;

        return items.stream()
                .filter(item -> {
                    if (active != null && item.isActive() != active) {
                        return false;
                    }
                    if (normalizedQuery != null && !normalizedQuery.isEmpty()) {
                        boolean matchName = item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                        boolean matchCode = item.getCode() != null && item.getCode().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                        boolean matchTriage = item.getTriageCode() != null && item.getTriageCode().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                        return matchName || matchCode || matchTriage;
                    }
                    return true;
                })
                .map(CmsCatalogItemResponse::from)
                .toList();
    }

    @Transactional(value = TX, readOnly = true)
    public CmsCatalogItemResponse getById(Long id) {
        return CmsCatalogItemResponse.from(load(id));
    }

    @Transactional(TX)
    public CmsCatalogItemResponse create(CmsCatalogItemRequest request) {
        CatalogType type = request.type();
        String code = cleanCode(request.code());
        String name = cleanName(request.name());
        String triageCode = cleanTriageCode(request.triageCode());

        if (catalogItemRepository.existsByTypeAndCodeIgnoreCase(type, code)) {
            throw new IllegalStateException("Mã \"" + code + "\" đã tồn tại trong danh mục " + type);
        }

        int sortOrder = request.sortOrder() != null ? request.sortOrder() : nextSortOrder(type);
        boolean active = request.active() == null || request.active();

        CatalogItem item = new CatalogItem();
        item.setType(type);
        item.setCode(code);
        item.setName(name);
        item.setTriageCode(triageCode);
        item.setSortOrder(sortOrder);
        item.setActive(active);

        CatalogItem saved = catalogItemRepository.saveAndFlush(item);
        return CmsCatalogItemResponse.from(saved);
    }

    @Transactional(TX)
    public CmsCatalogItemResponse update(Long id, CmsCatalogItemRequest request) {
        CatalogItem item = load(id);

        CatalogType type = request.type();
        String code = cleanCode(request.code());
        String name = cleanName(request.name());
        String triageCode = cleanTriageCode(request.triageCode());

        if (catalogItemRepository.existsByTypeAndCodeIgnoreCaseAndIdNot(type, code, id)) {
            throw new IllegalStateException("Mã \"" + code + "\" đã tồn tại trong danh mục " + type);
        }

        item.setType(type);
        item.setCode(code);
        item.setName(name);
        item.setTriageCode(triageCode);
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            item.setActive(request.active());
        }

        CatalogItem saved = catalogItemRepository.saveAndFlush(item);
        return CmsCatalogItemResponse.from(saved);
    }

    @Transactional(TX)
    public CmsCatalogItemResponse toggleActive(Long id) {
        CatalogItem item = load(id);
        item.setActive(!item.isActive());
        CatalogItem saved = catalogItemRepository.saveAndFlush(item);
        return CmsCatalogItemResponse.from(saved);
    }

    @Transactional(TX)
    public void delete(Long id) {
        CatalogItem item = load(id);
        catalogItemRepository.delete(item);
        catalogItemRepository.flush();
    }

    private CatalogItem load(Long id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục chuẩn hóa với id " + id));
    }

    private int nextSortOrder(CatalogType type) {
        return catalogItemRepository.findByTypeOrderBySortOrderAscNameAsc(type).stream()
                .mapToInt(CatalogItem::getSortOrder)
                .max()
                .orElse(0) + SORT_STEP;
    }

    private static String cleanCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Mã chuẩn hóa không được để trống");
        }
        return code;
    }

    private static String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống");
        }
        return name;
    }

    private static String cleanTriageCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }
}
