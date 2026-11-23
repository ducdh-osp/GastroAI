package vn.gastroai.be.api.document;

import vn.gastroai.be.domain.rag.DocumentStatus;

import java.time.Instant;
import java.util.List;

public record DocumentReviewResponse(
        Long id,
        String title,
        DocumentStatus status,
        /** Chatbot đang dùng đúng bản này. */
        boolean inUse,
        boolean contentApproved,
        String contentApprovedBy,
        Instant contentApprovedAt,
        /** PDF, DOCX hoặc null khi không còn file gốc (vd tài liệu mẫu). */
        String originalFileType,
        /** true = đây là bản thay thế: xác nhận nó sẽ xoá vĩnh viễn bản đang dùng (SCR-AD-04 §3.2). */
        boolean replacesExisting,
        /** Chữ đã trích, chỉ gửi khi không xem được file gốc bằng PDF. */
        String fullText,
        List<Chunk> chunks) {

    public record Chunk(int chunkIndex, String content) {
    }
}