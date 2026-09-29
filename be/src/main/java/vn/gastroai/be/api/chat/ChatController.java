package vn.gastroai.be.api.chat;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.gastroai.be.application.chat.ChatAnswer;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.RagAnswer;
import vn.gastroai.be.application.rag.StreamingDoneEvent;
import vn.gastroai.be.application.rag.StreamingRagQueryService;
import vn.gastroai.be.application.triage.TriageAlertPublisher;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import java.time.Instant;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

        private final ChatService chatService;
        private final ChatHistoryService chatHistoryService;
        private final StreamingRagQueryService streamingRagQueryService;
        private final TriageAlertPublisher triageAlertPublisher;

        public ChatController(
                        ChatService chatService,
                        ChatHistoryService chatHistoryService,
                        StreamingRagQueryService streamingRagQueryService,
                        TriageAlertPublisher triageAlertPublisher) {

                this.chatService = chatService;
                this.chatHistoryService = chatHistoryService;
                this.streamingRagQueryService = streamingRagQueryService;
                this.triageAlertPublisher = triageAlertPublisher;
        }

        /**
         * Gửi câu hỏi, nhận câu trả lời AI,
         * đồng thời lưu câu hỏi và câu trả lời vào lịch sử chat.
         */
        @PostMapping("/messages")
        public ChatMessageResponse sendMessage(
                        @Valid @RequestBody ChatMessageRequest request,
                        Principal principal,
                        Authentication authentication) {

                requireAuthenticated(authentication);

                Long patientId = Long.valueOf(principal.getName());

                ChatAnswer chatAnswer = chatService.ask(request.content());

                RagAnswer ragAnswer = chatAnswer.ragAnswer();

                List<ChatSourceResponse> sources = ragAnswer.sources()
                                .stream()
                                .map(source -> new ChatSourceResponse(
                                                source.documentTitle(),
                                                source.snippet(),
                                                source.sourceUrl()))
                                .toList();

                ChatHistoryService.SavedExchange saved = chatHistoryService.saveExchange(
                                patientId,
                                request.sessionId(),
                                request.content(),
                                ragAnswer,
                                chatAnswer.emergency(),
                                chatAnswer.matchedGroups());
                if (chatAnswer.emergency()) {
                        triageAlertPublisher.publish(new TriageAlertEvent(
                                        patientId,
                                        saved.sessionId(),
                                        saved.assistantMessageId(),
                                        request.content(),
                                        chatAnswer.matchedGroups(),
                                        Instant.now()));
                }

                return ChatMessageResponse.assistantReply(
                                ragAnswer.answer(),
                                sources,
                                ragAnswer.relatedQuestions(),
                                chatAnswer.emergency(),
                                chatAnswer.matchedGroups(),
                                saved.assistantMessageId(),
                                saved.sessionId());
        }

        /**
         * Đánh giá câu trả lời AI.
         */
        @PostMapping("/messages/{messageId}/rating")
        public void rateMessage(
                        @PathVariable Long messageId,
                        @Valid @RequestBody RateMessageRequest request,
                        Principal principal,
                        Authentication authentication) {

                requireAuthenticated(authentication);

                Long patientId = Long.valueOf(principal.getName());

                chatHistoryService.rateMessage(
                                patientId,
                                messageId,
                                request.rating());
        }

        /**
         * Streaming câu trả lời từ Gemini thông qua SSE.
         */
        @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter streamMessage(
                        @Valid @RequestBody ChatMessageRequest request,
                        Authentication authentication) {

                requireAuthenticated(authentication);

                Long patientId = Long.valueOf(authentication.getName());
                SseEmitter emitter = new SseEmitter(120_000L);

                Thread.startVirtualThread(() -> {
                        try {
                                StreamingRagQueryService.StreamingResult result = streamingRagQueryService.streamAnswer(
                                                request.content(),
                                                token -> {
                                                        try {
                                                                emitter.send(
                                                                                SseEmitter.event()
                                                                                                .data(token));
                                                        } catch (Exception exception) {
                                                                emitter.completeWithError(
                                                                                exception);

                                                                throw new RuntimeException(
                                                                                exception);
                                                        }
                                                });

                                emitter.send(
                                                SseEmitter.event()
                                                                .name("done")
                                                                .data(
                                                                                new StreamingDoneEvent(
                                                                                                result.sources(),
                                                                                                result.relatedQuestions(),
                                                                                                result.emergency(),
                                                                                                result.matchedGroups())));

                                emitter.complete();

                        } catch (Exception exception) {
                                emitter.completeWithError(exception);
                        }
                });

                return emitter;
        }

        private void requireAuthenticated(
                        Authentication authentication) {

                if (authentication == null
                                || !authentication.isAuthenticated()) {

                        throw new org.springframework.security.access.AccessDeniedException(
                                        "Chua dang nhap");
                }
        }
}
