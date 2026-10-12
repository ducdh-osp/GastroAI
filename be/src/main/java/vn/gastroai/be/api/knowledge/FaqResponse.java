package vn.gastroai.be.api.knowledge;

import vn.gastroai.be.domain.knowledge.Faq;

public record FaqResponse(
        Long id,
        String question,
        String answer,
        String category,
        int sortOrder
) {
    public static FaqResponse from(Faq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getQuestion(),
                faq.getAnswer(),
                faq.getCategory(),
                faq.getSortOrder()
        );
    }
}
