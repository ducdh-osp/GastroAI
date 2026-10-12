package vn.gastroai.be.api.cms.knowledge;

/** UC0066 - articleCount = so bai viet (ca da/chua xuat ban) dang gan chu de nay. */
public record ArticleTopicResponse(
        Long id,
        String name,
        int sortOrder,
        long articleCount
) {
}
