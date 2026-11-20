package vn.gastroai.be.api.document;

import java.util.List;

/**
 * Kết quả tìm nội dung (UC0055): tối đa 20 tài liệu khớp nhiều nhất; totalDocuments là tổng số
 * tài liệu khớp, lớn hơn items.size() thì FE nhắc gõ cụ thể hơn.
 */
public record DocumentSearchResult(List<DocumentSearchHit> items, long totalDocuments) {
}