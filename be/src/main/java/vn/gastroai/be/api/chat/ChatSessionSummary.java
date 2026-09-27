package vn.gastroai.be.api.chat;

import java.time.Instant;

/** Tóm tắt 1 phiên chat — dùng trong danh sách lịch sử. */
public record ChatSessionSummary(
        Long id,
        String title,
        Instant createdAt,
        Instant updatedAt
) {}
