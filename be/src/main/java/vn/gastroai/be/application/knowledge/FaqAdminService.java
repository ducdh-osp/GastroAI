package vn.gastroai.be.application.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.knowledge.CmsFaqPageResponse;
import vn.gastroai.be.api.cms.knowledge.CmsFaqPublishRequest;
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
    private static final String MSG_REVIEW_REQUIRED =
            "FAQ muốn xuất bản bắt buộc phải có thông tin bác sĩ kiểm duyệt y khoa cho nội dung hiện tại";

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
        String question = clean(req.question(), "Câu hỏi không được để trống");
        String answer = clean(req.answer(), "Câu trả lời không được để trống");
        String category = clean(req.category(), "Chủ đề không được để trống");
        int sortOrder = req.sortOrder() != null ? req.sortOrder() : nextSortOrder();
        // Mặc định là bản nháp - chỉ công khai khi client yêu cầu rõ ràng VÀ có bác sĩ kiểm duyệt
        boolean published = Boolean.TRUE.equals(req.published());
        String reviewer = trimToNull(req.medicalReviewer());

        Faq faq = new Faq(question, answer, category, sortOrder, false);
        if (published) {
            if (reviewer == null) {
                throw new IllegalArgumentException(MSG_REVIEW_REQUIRED);
            }
            faq.setPublished(true);
        }
        if (reviewer != null) {
            faq.setMedicalReviewer(reviewer);
            faq.setMedicalReviewedAt(Instant.now());
        }

        return toResponse(faqRepository.saveAndFlush(faq));
    }

    @Transactional(value = TX)
    public CmsFaqResponse update(Long id, CmsFaqRequest req) {
        Faq faq = load(id);
        checkVersion(faq, req.version());

        String question = clean(req.question(), "Câu hỏi không được để trống");
        String answer = clean(req.answer(), "Câu trả lời không được để trống");
        String category = clean(req.category(), "Chủ đề không được để trống");
        String newReviewer = trimToNull(req.medicalReviewer());

        boolean contentChanged = !question.equals(faq.getQuestion()) || !answer.equals(faq.getAnswer());
        boolean targetPublished = req.published() != null ? req.published() : faq.isPublished();

        faq.setQuestion(question);
        faq.setAnswer(answer);
        faq.setCategory(category);
        if (req.sortOrder() != null) {
            faq.setSortOrder(req.sortOrder());
        }

        if (targetPublished) {
            if (faq.isPublished() && contentChanged && newReviewer == null) {
                // Nội dung y khoa thay đổi trên FAQ đang công khai mà không có kiểm duyệt mới
                // -> tự động chuyển về bản nháp để bệnh nhân không thấy nội dung chưa được duyệt
                faq.setPublished(false);
                faq.setMedicalReviewedAt(null);
            } else {
                String reviewer = newReviewer != null ? newReviewer : trimToNull(faq.getMedicalReviewer());
                // Lần duyệt cũ chỉ còn hiệu lực khi nội dung không đổi và chưa bị vô hiệu hoá trước đó
                boolean previousReviewValid = !contentChanged && faq.getMedicalReviewedAt() != null;
                if (reviewer == null || (newReviewer == null && !previousReviewValid)) {
                    throw new IllegalArgumentException(MSG_REVIEW_REQUIRED);
                }
                faq.setPublished(true);
                faq.setMedicalReviewer(reviewer);
                if (newReviewer != null) {
                    faq.setMedicalReviewedAt(Instant.now());
                }
            }
        } else {
            faq.setPublished(false);
            if (newReviewer != null) {
                faq.setMedicalReviewer(newReviewer);
                faq.setMedicalReviewedAt(Instant.now());
            } else if (contentChanged) {
                // Lần duyệt cũ không còn áp dụng cho nội dung mới
                faq.setMedicalReviewedAt(null);
            }
        }
        faq.setUpdatedAt(Instant.now());

        return toResponse(faqRepository.saveAndFlush(faq));
    }

    @Transactional(value = TX)
    public CmsFaqResponse setPublished(Long id, CmsFaqPublishRequest req) {
        Faq faq = load(id);
        checkVersion(faq, req.version());

        if (Boolean.TRUE.equals(req.published())) {
            String newReviewer = trimToNull(req.medicalReviewer());
            String reviewer = newReviewer != null ? newReviewer : trimToNull(faq.getMedicalReviewer());
            // Cần có người duyệt VÀ lần duyệt phải còn hiệu lực cho nội dung hiện tại
            if (reviewer == null || (newReviewer == null && faq.getMedicalReviewedAt() == null)) {
                throw new IllegalArgumentException(
                        "Không thể xuất bản FAQ khi chưa có thông tin bác sĩ kiểm duyệt y khoa cho nội dung hiện tại");
            }
            faq.setPublished(true);
            faq.setMedicalReviewer(reviewer);
            if (newReviewer != null) {
                faq.setMedicalReviewedAt(Instant.now());
            }
        } else {
            faq.setPublished(false);
        }
        faq.setUpdatedAt(Instant.now());
        return toResponse(faqRepository.saveAndFlush(faq));
    }

    @Transactional(value = TX)
    public void delete(Long id) {
        faqRepository.delete(load(id));
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

    private Faq load(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay FAQ voi id=" + id));
    }

    private static void checkVersion(Faq faq, Long requestVersion) {
        if (requestVersion != null && !requestVersion.equals(faq.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Faq.class, faq.getId());
        }
    }

    private static String clean(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean containsNormalized(String text, String normalizedQuery) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return VietnameseText.normalize(text).contains(normalizedQuery);
    }

    private CmsFaqResponse toResponse(Faq faq) {
        return CmsFaqResponse.from(faq);
    }
}
