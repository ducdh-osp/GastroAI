package vn.gastroai.be.application.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.knowledge.CmsFaqPageResponse;
import vn.gastroai.be.api.cms.knowledge.CmsFaqRequest;
import vn.gastroai.be.api.cms.knowledge.CmsFaqResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.knowledge.Faq;
import vn.gastroai.be.infrastructure.persistence.postgres.FaqRepository;

import java.time.Instant;
import java.util.List;

/** UC0062 - Quản lý câu hỏi thường gặp (FAQ) cho Admin/Bác sĩ trong CMS. */
@Service
public class FaqAdminService {

    private static final String TX = "postgresTransactionManager";
    private static final int SORT_STEP = 10;

    private final FaqRepository faqRepository;

    public FaqAdminService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Transactional(value = TX, readOnly = true)
    public CmsFaqPageResponse list(String query, String category, Boolean published, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }

        String normalizedQuery = query == null || query.isBlank() ? "" : VietnameseText.normalize(query.trim());
        String normalizedCategory = category == null || category.isBlank() ? "" : VietnameseText.normalize(category.trim());

        List<Faq> all = faqRepository.findAllByOrderBySortOrderAscIdAsc();

        List<Faq> filtered = all.stream()
                .filter(f -> published == null || f.isPublished() == published)
                .filter(f -> normalizedCategory.isEmpty()
                        || VietnameseText.normalize(f.getCategory()).equals(normalizedCategory))
                .filter(f -> normalizedQuery.isEmpty()
                        || containsNormalized(f.getQuestion(), normalizedQuery)
                        || containsNormalized(f.getAnswer(), normalizedQuery))
                .toList();

        Pageable pageable = PageRequest.of(page, size);
        long offset = pageable.getOffset();
        if (offset >= filtered.size()) {
            return new CmsFaqPageResponse(List.of(), filtered.size(), page, size);
        }

        int start = Math.toIntExact(offset);
        int end = Math.min(start + size, filtered.size());
        List<CmsFaqResponse> pageItems = filtered.subList(start, end).stream()
                .map(this::toResponse)
                .toList();

        return new CmsFaqPageResponse(pageItems, filtered.size(), page, size);
    }

    @Transactional(value = TX, readOnly = true)
    public CmsFaqResponse get(Long id) {
        return faqRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay FAQ voi id=" + id));
    }

    @Transactional(value = TX)
    public CmsFaqResponse create(CmsFaqRequest req) {
        int sortOrder = req.sortOrder() != null ? req.sortOrder() : nextSortOrder();
        boolean published = req.published() == null || req.published();

        Faq faq = new Faq(
                req.question().trim(),
                req.answer().trim(),
                req.category().trim(),
                sortOrder,
                published
        );

        return toResponse(faqRepository.save(faq));
    }

    @Transactional(value = TX)
    public CmsFaqResponse update(Long id, CmsFaqRequest req) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay FAQ voi id=" + id));

        faq.setQuestion(req.question().trim());
        faq.setAnswer(req.answer().trim());
        faq.setCategory(req.category().trim());
        if (req.sortOrder() != null) {
            faq.setSortOrder(req.sortOrder());
        }
        if (req.published() != null) {
            faq.setPublished(req.published());
        }
        faq.setUpdatedAt(Instant.now());

        return toResponse(faqRepository.save(faq));
    }

    @Transactional(value = TX)
    public CmsFaqResponse setPublished(Long id, boolean published) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay FAQ voi id=" + id));

        faq.setPublished(published);
        faq.setUpdatedAt(Instant.now());
        return toResponse(faqRepository.save(faq));
    }

    @Transactional(value = TX)
    public void delete(Long id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay FAQ voi id=" + id));
        faqRepository.delete(faq);
    }

    @Transactional(value = TX, readOnly = true)
    public List<String> getCategories() {
        return faqRepository.findDistinctAllCategories();
    }

    private int nextSortOrder() {
        return faqRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .mapToInt(Faq::getSortOrder)
                .max()
                .orElse(0) + SORT_STEP;
    }

    private boolean containsNormalized(String text, String normalizedQuery) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return VietnameseText.normalize(text).contains(normalizedQuery);
    }

    private CmsFaqResponse toResponse(Faq faq) {
        return new CmsFaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCategory(),
                faq.getSortOrder(),
                faq.isPublished(),
                faq.getCreatedAt(),
                faq.getUpdatedAt()
        );
    }
}
