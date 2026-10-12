package vn.gastroai.be.application.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.knowledge.FaqResponse;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.knowledge.Faq;
import vn.gastroai.be.infrastructure.persistence.postgres.FaqRepository;

import java.util.List;

/** UC0021 - Dịch vụ tra cứu FAQ cho bệnh nhân. */
@Service
public class FaqPatientService {

    private static final String TX = "postgresTransactionManager";

    private final FaqRepository faqRepository;

    public FaqPatientService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Transactional(value = TX, readOnly = true)
    public List<FaqResponse> list(String query, String category) {
        String normalizedQuery = query == null || query.isBlank() ? "" : VietnameseText.normalize(query.trim());
        String normalizedCategory = category == null || category.isBlank() ? "" : VietnameseText.normalize(category.trim());

        List<Faq> published = faqRepository.findByPublishedTrueOrderBySortOrderAscIdAsc();

        return published.stream()
                .filter(f -> normalizedCategory.isEmpty()
                        || VietnameseText.normalize(f.getCategory()).equals(normalizedCategory))
                .filter(f -> normalizedQuery.isEmpty()
                        || containsNormalized(f.getQuestion(), normalizedQuery)
                        || containsNormalized(f.getAnswer(), normalizedQuery))
                .map(FaqResponse::from)
                .toList();
    }

    @Transactional(value = TX, readOnly = true)
    public List<String> listCategories() {
        return faqRepository.findDistinctPublishedCategories();
    }

    private boolean containsNormalized(String text, String normalizedQuery) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return VietnameseText.normalize(text).contains(normalizedQuery);
    }
}
