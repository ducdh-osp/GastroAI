package vn.gastroai.be.application.readmodel;

import java.time.Instant;

public record LoginHistoryItem(
        Long id,
        Instant attemptedAt,
        String outcome,
        String failureReason,
        String ipAddress,
        String userAgent,
        String deviceLabel
) {
}
