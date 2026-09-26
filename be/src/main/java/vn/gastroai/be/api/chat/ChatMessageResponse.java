package vn.gastroai.be.api.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** UC0017 - response cua POST /api/v1/chat/messages, dung dung field FE dang cho (xem Message trong fe/src/api/chat.ts). */
public record ChatMessageResponse(String id, String sender, String content, String createdAt, String status,
                                  List<ChatSourceResponse> sources, List<String> relatedQuestions,
                                  boolean emergency) {
    public static ChatMessageResponse assistantReply(
            String content, List<ChatSourceResponse> sources, List<String> relatedQuestions, boolean emergency) {
        return new ChatMessageResponse(
                UUID.randomUUID().toString(), "assistant", content, Instant.now().toString(), "sent",
                sources, relatedQuestions, emergency);
    }
}
