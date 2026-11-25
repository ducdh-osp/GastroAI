package vn.gastroai.be.domain.rag;

/** Trạng thái tổng quát hiển thị cho người dùng - suy ra từ {@link DocumentProcessingStage#status()}, không lưu DB. */
public enum DocumentStatus {
    PENDING,
    PROCESSING,
    DONE,
    ERROR
}
