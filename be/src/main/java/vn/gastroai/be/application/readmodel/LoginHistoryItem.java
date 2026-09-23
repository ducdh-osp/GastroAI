package vn.gastroai.be.application.readmodel;

import java.time.Instant;

/** 1 dòng lịch sử đăng nhập đã map cho FE — outcome là tên enum LoginOutcome dạng String (SUCCESS/FAILURE/BLOCKED). */
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
