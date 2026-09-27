package vn.gastroai.be.api.chat;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import vn.gastroai.be.application.chat.ChatAnswer;
import vn.gastroai.be.application.chat.ChatService;
import vn.gastroai.be.application.rag.RagAnswer;
import vn.gastroai.be.application.rag.StreamingDoneEvent;
import vn.gastroai.be.application.rag.StreamingRagQueryService;

import java.util.List;

/**
 * REST API cho UC0017 - Benh nhan gui cau hoi, nhan cau tra loi tu Gemini/RAG.
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;
    private final StreamingRagQueryService streamingRagQueryService;

    public ChatController(
            ChatService chatService,
            StreamingRagQueryService streamingRagQueryService) {
        this.chatService = chatService;
        this.streamingRagQueryService = streamingRagQueryService;
    }

    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(
            @Valid @RequestBody ChatMessageRequest request) {
        ChatAnswer chatAnswer = chatService.ask(request.content());

        RagAnswer ragAnswer = chatAnswer.ragAnswer();

        List<ChatSourceResponse> sources = ragAnswer.sources().stream()
                .map(source -> new ChatSourceResponse(
                        source.documentTitle(),
                        source.snippet()))
                .toList();

        return ChatMessageResponse.assistantReply(
                ragAnswer.answer(),
                sources,
                ragAnswer.relatedQuestions(),
                chatAnswer.emergency());
    }

    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @Valid @RequestBody ChatMessageRequest request) {
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
                                emitter.completeWithError(exception);
                                throw new RuntimeException(exception);
                            }
                        });

                emitter.send(
                        SseEmitter.event()
                                .name("done")
                                .data(
                                        new StreamingDoneEvent(
                                                result.sources(),
                                                result.relatedQuestions(),
                                                result.emergency())));

                emitter.complete();

            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }
}