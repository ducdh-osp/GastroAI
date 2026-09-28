package vn.gastroai.be.api.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * UC0017 - response cua POST /api/v1/chat/messages.
 * - id: UUID ngau nhien de FE dedup tin nhan trong UI.
 * - dbMessageId: id cua ban ghi chat_messages trong DB — FE gui kem khi goi API rating.
 * - sessionId: id phien chat hien tai — FE gui kem o cac luot tiep theo de tiep tuc phien.
 */
public record ChatMessageResponse(
        String id,
        String sender,
        String content,
        String createdAt,
        String status,
        List<ChatSourceResponse> sources,
        List<String> relatedQuestions,
        boolean emergency,
        List<String> matchedGroups,
        Long dbMessageId,
        Long sessionId
) {

    public static ChatMessageResponse assistantReply(
            String content,
            List<ChatSourceResponse> sources,
            List<String> relatedQuestions,
            boolean emergency,
            List<String> matchedGroups,
            Long dbMessageId,
            Long sessionId) {

        return new ChatMessageResponse(
                UUID.randomUUID().toString(),
                "assistant",
                content,
                Instant.now().toString(),
                "sent",
                sources,
                relatedQuestions,
                emergency,
                matchedGroups,
                dbMessageId,
                sessionId
        );
    }
}