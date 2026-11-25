package vn.gastroai.be.infrastructure.persistence.postgres;

import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;

import java.time.Instant;

/** 1 dong nhe cua danh sach tai lieu (ban thay the neu co, khong thi dong chinh) - KHONG co full_text. */
public interface DocumentListRow {
    Long getId();
    String getTitle();
    String getSource();
    String getSourceUrl();
    DocumentProcessingStage getProcessingStage();

    /** Suy ra từ bước xử lý - không còn cột status riêng (3NF). */
    default DocumentStatus getStatus() {
        return getProcessingStage().status();
    }
    String getErrorMessage();
    Instant getUploadedAt();
    Instant getUpdatedAt();
    Long getGroupId();
    Instant getContentApprovedAt();
}