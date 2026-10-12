package vn.gastroai.be.application.triage;

import java.util.List;
import java.util.Map;

/**
 * UC0054 - nguồn cung cấp từ khóa cấp cứu, tách interface riêng khỏi TriageService (logic so
 * khớp) để GĐ5 đổi sang đọc từ DB (màn hình admin CRUD từ khóa) mà không phải sửa
 * TriageService - chỉ cần viết thêm 1 implementation mới, giống cách EmbeddingStore không phụ
 * thuộc entity Java cụ thể nào.
 */
public interface TriageKeywordSource {
    /** Key = mã nhóm triệu chứng (vd "XUAT_HUYET_TIEU_HOA"), value = các cách nói không dấu. */
    Map<String, List<String>> keywordsByGroup();
}
