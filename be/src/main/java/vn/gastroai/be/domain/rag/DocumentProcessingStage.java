package vn.gastroai.be.domain.rag;

/**
 * Bước xử lý hiện tại của tài liệu. Đây là trạng thái DUY NHẤT được lưu (cột processing_stage);
 * trạng thái tổng quát {@link DocumentStatus} suy ra từ bước này nên không lưu thành cột riêng (3NF).
 */
public enum DocumentProcessingStage {
    PENDING,
    EXTRACTION,
    CHUNKING,
    EMBEDDING,
    DONE,
    ERROR;

    /** Trạng thái tổng quát tương ứng: 3 bước đang chạy đều tính là PROCESSING. */
    public DocumentStatus status() {
        return switch (this) {
            case PENDING -> DocumentStatus.PENDING;
            case EXTRACTION, CHUNKING, EMBEDDING -> DocumentStatus.PROCESSING;
            case DONE -> DocumentStatus.DONE;
            case ERROR -> DocumentStatus.ERROR;
        };
    }
}
