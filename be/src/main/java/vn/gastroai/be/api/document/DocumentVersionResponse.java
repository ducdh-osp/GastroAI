package vn.gastroai.be.api.document;

import vn.gastroai.be.domain.rag.DocumentStatus;

import java.time.Instant;

public record DocumentVersionResponse(
        Long id,
        int versionNo,
        String title,
        DocumentStatus status,
        boolean currentVersion,
        Instant uploadedAt,
        String errorMessage) {
}