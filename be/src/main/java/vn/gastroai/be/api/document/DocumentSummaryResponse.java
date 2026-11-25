package vn.gastroai.be.api.document;

import vn.gastroai.be.domain.rag.DocumentProcessingStage;
import vn.gastroai.be.domain.rag.DocumentStatus;

import java.time.Instant;

public record DocumentSummaryResponse(
        Long id,
        String title,
        String source,
        String sourceUrl,
        DocumentStatus status,
        DocumentProcessingStage processingStage,
        String errorMessage,
        long chunkCount,
        Instant uploadedAt,
        Instant updatedAt,
        /** Chatbot đang dùng tài liệu này (có 1 bản đã xác nhận và không bị gỡ). */
        boolean inUse,
        /** Dòng đang hiện là bản thay thế chờ xử lý / chờ xác nhận (bản cũ vẫn phục vụ chatbot). */
        boolean replacing,
        boolean unpublished,
        boolean contentApproved
) {
}