package vn.gastroai.be.api.chat;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.chat.ChatAnswer;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.RagAnswer;

import java.security.Principal;
import java.util.List;

/** REST API cho UC0017 - Benh nhan gui cau hoi, nhan cau tra loi tu Gemini/RAG. */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;

    public ChatController(ChatService chatService, ChatHistoryService chatHistoryService) {
        this.chatService = chatService;
        this.chatHistoryService = chatHistoryService;
    }

    /** Gửi câu hỏi, nhận câu trả lời AI, tự động lưu vào lịch sử phiên chat. */
    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@Valid @RequestBody ChatMessageRequest request,
                                           Principal principal, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
        Long patientId = Long.valueOf(principal.getName());

        ChatAnswer chatAnswer = chatService.ask(request.content());
        RagAnswer ragAnswer = chatAnswer.ragAnswer();
        List<ChatSourceResponse> sources = ragAnswer.sources().stream()
                .map(source -> new ChatSourceResponse(source.documentTitle(), source.snippet()))
                .toList();

        // Lưu trao đổi vào DB (tạo/tiếp tục phiên)
        ChatHistoryService.SavedExchange saved = chatHistoryService.saveExchange(
                patientId, request.sessionId(), request.content(), ragAnswer, chatAnswer.emergency());

        return ChatMessageResponse.assistantReply(
                ragAnswer.answer(), sources, ragAnswer.relatedQuestions(),
                chatAnswer.emergency(), saved.assistantMessageId(), saved.sessionId());
    }

    /** Đánh giá phản hồi AI: HELPFUL hoặc UNHELPFUL (UPSERT — cho phép đổi ý). */
    @PostMapping("/messages/{messageId}/rating")
    public void rateMessage(@PathVariable Long messageId,
                            @Valid @RequestBody RateMessageRequest request,
                            Principal principal, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
        Long patientId = Long.valueOf(principal.getName());
        chatHistoryService.rateMessage(patientId, messageId, request.rating());
    }
}

