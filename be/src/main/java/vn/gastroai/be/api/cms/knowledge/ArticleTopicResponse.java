package vn.gastroai.be.api.cms.knowledge;

/** UC0066 - articleCount / faqCount = so bai viet va FAQ dang gan chu de nay. */
public record ArticleTopicResponse(
        Long id,
        String name,
        int sortOrder,
        long articleCount,
        long faqCount,
        Long version
) {
    public ArticleTopicResponse(Long id, String name, int sortOrder, long articleCount, Long version) {
        this(id, name, sortOrder, articleCount, 0L, version);
    }

    public ArticleTopicResponse(Long id, String name, int sortOrder, long articleCount) {
        this(id, name, sortOrder, articleCount, 0L, 0L);
    }
}
