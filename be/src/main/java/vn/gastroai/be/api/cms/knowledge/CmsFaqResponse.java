package vn.gastroai.be.api.cms.knowledge;

import vn.gastroai.be.domain.knowledge.Faq;

import java.time.Instant;

public record CmsFaqResponse(
        Long id,
        String question,
        String answer,
        String category,
        int sortOrder,
        boolean published,
        String medicalReviewer,
        Instant medicalReviewedAt,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public CmsFaqResponse(
            Long id, String question, String answer, String category, int sortOrder,
            boolean published, Instant createdAt, Instant updatedAt
    ) {
        this(id, question, answer, category, sortOrder, published, null, null, createdAt, updatedAt, 0L);
    }

    public static CmsFaqResponse from(Faq faq) {
        return new CmsFaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCategory(),
                faq.getSortOrder(),
                faq.isPublished(),
                faq.getMedicalReviewer(),
                faq.getMedicalReviewedAt(),
                faq.getCreatedAt(),
                faq.getUpdatedAt(),
                faq.getVersion()
        );
    }
}
