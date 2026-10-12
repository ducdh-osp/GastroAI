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
        Instant updatedAt
) {
}
