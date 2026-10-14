package vn.gastroai.be.infrastructure.persistence.postgres;

import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;

import java.time.Instant;

/** 1 dong nhe cua danh sach tai lieu (phien ban moi nhat cua moi nhom) - KHONG co full_text. */
public interface DocumentListRow {
    Long getId();
    String getTitle();
    String getSource();
    String getSourceUrl();
    DocumentStatus getStatus();
    DocumentProcessingStage getProcessingStage();
    String getErrorMessage();
    Instant getUploadedAt();
    Instant getUpdatedAt();
    Integer getVersionNo();
    Long getGroupId();
}