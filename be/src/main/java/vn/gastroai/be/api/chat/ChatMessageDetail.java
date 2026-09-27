package vn.gastroai.be.api.chat;

import java.time.Instant;
import java.util.List;

/** Chi tiết 1 tin nhắn trong phiên chat (trả về khi lấy lịch sử hội thoại). */
public record ChatMessageDetail(
        Long id,
        String sender,
        String content,
        Instant createdAt,
        boolean emergency,
        List<ChatSourceResponse> sources,
        List<String> relatedQuestions,
        /** Đánh giá hiện tại của người dùng: "HELPFUL", "UNHELPFUL", hoặc null nếu chưa đánh giá. */
        String rating
) {}
