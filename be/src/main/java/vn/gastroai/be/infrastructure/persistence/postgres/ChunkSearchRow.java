package vn.gastroai.be.infrastructure.persistence.postgres;

/** 1 tài liệu trong kết quả tìm nội dung (UC0055) - Spring tự khớp theo tên cột AS. */
public interface ChunkSearchRow {
    Long getDocumentId();
    String getTitle();
    Long getMatchCount();
    String getFirstContent();
    Long getTotalDocuments();
}