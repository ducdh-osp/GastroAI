package vn.gastroai.be.api.chat;

import java.util.List;

/** Kết quả phân trang danh sách phiên chat. */
public record ChatSessionListResponse(
        List<ChatSessionSummary> sessions,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
