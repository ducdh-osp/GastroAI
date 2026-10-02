package vn.gastroai.be.api.chat;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.gastroai.be.api.support.AuthenticatedRequest;
import vn.gastroai.be.application.chat.ChatAnswer;
import vn.gastroai.be.application.chat.ChatAttachmentProcessor;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.RagAnswer;
import vn.gastroai.be.application.rag.StreamingDoneEvent;
import vn.gastroai.be.application.rag.StreamingRagQueryService;
import vn.gastroai.be.application.triage.TriageAlertService;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;
    private final StreamingRagQueryService streamingRagQueryService;
    private final TriageAlertService triageAlertService;
    private final ChatAttachmentProcessor attachmentProcessor;

    public ChatController(
            ChatService chatService,
            ChatHistoryService chatHistoryService,
            StreamingRagQueryService streamingRagQueryService,
            TriageAlertService triageAlertService,
            ChatAttachmentProcessor attachmentProcessor) {

        this.chatService = chatService;
        this.chatHistoryService = chatHistoryService;
        this.streamingRagQueryService = streamingRagQueryService;
        this.triageAlertService = triageAlertService;
        this.attachmentProcessor = attachmentProcessor;
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

        Long patientId = AuthenticatedRequest.patientId(principal, authentication);

        ChatAnswer chatAnswer = chatService.ask(request.content(), triageResult -> {
            if (triageResult.emergency()) {
                triageAlertService.createAndPublish(
                        patientId,
                        null,
                        null,
                        request.content(),
                        triageResult.matchedGroups());
            }
        });

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
     * Nhu sendMessage(), nhung nhan them file/anh dinh kem (UC chat dinh kem) - endpoint rieng
     * (khong doi sendMessage() hien co) de khong phai sua lai toan bo test/contract JSON dang
     * dung cho truong hop khong co dinh kem (van la da so request). files rong hoac thieu van
     * hoat dong binh thuong, giong het sendMessage().
     */
    @PostMapping(value = "/messages/with-attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMessageResponse sendMessageWithAttachments(
            @Valid @RequestPart("request") ChatMessageRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Principal principal,
            Authentication authentication) {

        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        List<MultipartFile> attachedFiles = files == null ? List.of() : files.stream().filter(Objects::nonNull).toList();
        ChatAttachmentProcessor.ProcessedAttachments processed = attachmentProcessor.process(attachedFiles);

        ChatAnswer chatAnswer = chatService.ask(request.content(), processed.images(), processed.documentText(),
                triageResult -> {
                    if (triageResult.emergency()) {
                        triageAlertService.createAndPublish(
                                patientId,
                                null,
                                null,
                                request.content(),
                                triageResult.matchedGroups());
                    }
                });

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

        chatHistoryService.saveAttachments(saved.patientMessageId(), processed.attachments());

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

        Long patientId = AuthenticatedRequest.patientId(principal, authentication);

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

        AuthenticatedRequest.requireAuthenticated(authentication);

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
                        },
                        triageResult -> {
                            if (triageResult.emergency()) {
                                triageAlertService.createAndPublish(
                                        patientId,
                                        null,
                                        null,
                                        request.content(),
                                        triageResult.matchedGroups());
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

                try {
                    chatHistoryService.saveExchange(
                            patientId,
                            request.sessionId(),
                            request.content(),
                            new RagAnswer(result.answer(), result.sources(), result.relatedQuestions()),
                            result.emergency(),
                            result.matchedGroups());
                } catch (Exception exception) {
                    log.error("Khong the luu lich su chat cho luong streaming, patientId={}: {}",
                            patientId, exception.getMessage(), exception);
                }

            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }
}
