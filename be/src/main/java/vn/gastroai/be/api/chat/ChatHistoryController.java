package vn.gastroai.be.api.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.domain.chat.ChatMessage;
import vn.gastroai.be.domain.chat.ChatSession;
import vn.gastroai.be.infrastructure.persistence.postgres.MessageRatingRepository;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * Lịch sử phiên chat của bệnh nhân:
 *   GET /api/v1/chat/sessions          — danh sách phiên (phân trang, mới nhất trước)
 *   GET /api/v1/chat/sessions/{id}     — toàn bộ tin nhắn trong 1 phiên
 */
@RestController
@RequestMapping("/api/v1/chat/sessions")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    private final MessageRatingRepository ratingRepository;
    private final ObjectMapper objectMapper;

    public ChatHistoryController(ChatHistoryService chatHistoryService,
                                 MessageRatingRepository ratingRepository,
                                 ObjectMapper objectMapper) {
        this.chatHistoryService = chatHistoryService;
        this.ratingRepository = ratingRepository;
        this.objectMapper = objectMapper;
    }

    /** Danh sách phiên chat của bệnh nhân đang đăng nhập, mới nhất trước. */
    @GetMapping
    public ChatSessionListResponse listSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        requireAuth(authentication);
        Long patientId = Long.valueOf(principal.getName());
        Page<ChatSession> result = chatHistoryService.listSessions(patientId, page, size);
        List<ChatSessionSummary> summaries = result.getContent().stream()
                .map(s -> new ChatSessionSummary(s.getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
        return new ChatSessionListResponse(summaries, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** Nội dung 1 phiên chat: toàn bộ tin nhắn kèm đánh giá (nếu có). */
    @GetMapping("/{sessionId}")
    public List<ChatMessageDetail> getSessionMessages(
            @PathVariable Long sessionId,
            Principal principal, Authentication authentication) {
        requireAuth(authentication);
        Long patientId = Long.valueOf(principal.getName());
        List<ChatMessage> messages = chatHistoryService.listMessages(patientId, sessionId);
        return messages.stream().map(this::toDetail).toList();
    }

    // ─────────────────────────────── helpers ────────────────────────────────

    private void requireAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
    }

    private ChatMessageDetail toDetail(ChatMessage msg) {
        List<ChatSourceResponse> sources = parseJson(msg.getSources(),
                new TypeReference<List<ChatSourceResponse>>() {});
        List<String> related = parseJson(msg.getRelatedQuestions(),
                new TypeReference<List<String>>() {});
        String rating = ratingRepository.findByMessageId(msg.getId())
                .map(r -> r.getRating())
                .orElse(null);
        return new ChatMessageDetail(msg.getId(), msg.getSender(), msg.getContent(),
                msg.getCreatedAt(), msg.isEmergency(), sources, related, rating);
    }

    private <T> List<T> parseJson(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
