package vn.gastroai.be.api.chat;

import jakarta.servlet.http.HttpServletResponse;
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
import vn.gastroai.be.infrastructure.ai.ImagePart;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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

        Long[] alertIdHolder = new Long[1];

        ChatAnswer chatAnswer = chatService.ask(request.content(), triageResult -> {
            if (triageResult.emergency()) {
                try {
                    alertIdHolder[0] = triageAlertService.createAndPublish(
                            patientId,
                            null,
                            null,
                            request.content(),
                            triageResult.matchedGroups());
                } catch (Exception exception) {
                    log.error("Khong the tao/gui canh bao Triage cho patientId={}: {}",
                            patientId, exception.getMessage(), exception);
                }
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

        if (alertIdHolder[0] != null) {
            try {
                triageAlertService.linkConversation(
                        alertIdHolder[0], saved.sessionId(), saved.assistantMessageId());
            } catch (Exception exception) {
                log.error("Khong the gan sessionId/messageId vao canh bao Triage id={}: {}",
                        alertIdHolder[0], exception.getMessage(), exception);
            }
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
     * Nhu sendMessage(), nhung nhan them file/anh dinh kem (UC chat dinh kem) - endpoint rieng
     * (khong doi sendMessage() hien co) de khong phai sua lai toan bo test/contract JSON dang
     * dung cho truong hop khong co dinh kem (van la da so request). files rong hoac thieu van
     * hoat dong binh thuong, giong het sendMessage().
     *
     * Day la duong khong streaming - ca cau tra loi tra ve 1 lan. Muon stream va co dinh kem
     * thi goi /messages/stream-with-attachments.
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
     * Streaming câu trả lời từ Gemini thông qua SSE. Tin nhắn KHÔNG có file đính kèm - có
     * đính kèm thì gọi /messages/stream-with-attachments.
     */
    @PostMapping(value = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication,
            HttpServletResponse response) {

        AuthenticatedRequest.requireAuthenticated(authentication);
        Long patientId = Long.valueOf(authentication.getName());

        return startStreaming(request, patientId, response, List.of(), null, List.of());
    }

    /**
     * Nhu streamMessage(), nhung nhan them file/anh dinh kem va stream cau tra loi qua SSE -
     * gop ca 2 dac diem cua sendMessageWithAttachments() (nhan multipart) va streamMessage()
     * (tra loi dan qua SSE).
     *
     * QUAN TRONG: attachmentProcessor.process(files) PHAI duoc goi NGAY TAI DAY, trong thread
     * cua controller, TRUOC KHI return SseEmitter. Spring se xoa file tam cua multipart request
     * ngay khi method controller return, du luong sinh cau tra loi van con chay tiep tren
     * virtual thread sau do - doc file dinh kem trong virtual thread luc nay co the gap file
     * da bi xoa mat.
     */
    @PostMapping(value = "/messages/stream-with-attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessageWithAttachments(
            @Valid @RequestPart("request") ChatMessageRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication,
            HttpServletResponse response) {

        AuthenticatedRequest.requireAuthenticated(authentication);
        Long patientId = Long.valueOf(authentication.getName());

        List<MultipartFile> attachedFiles = files == null ? List.of() : files.stream().filter(Objects::nonNull).toList();
        ChatAttachmentProcessor.ProcessedAttachments processed = attachmentProcessor.process(attachedFiles);

        return startStreaming(request, patientId, response, processed.images(), processed.documentText(), processed.attachments());
    }

    /**
     * Phan than chung cho ca streamMessage() va streamMessageWithAttachments() - tranh chep
     * lai toan bo logic SSE/virtual thread 2 lan.
     *
     * Chan file mo coi: attachmentsLinked bat dau la true neu khong co file dinh kem (khong
     * co gi can lien ket). Neu co file, chi thanh true SAU KHI saveAttachments() thanh cong -
     * bat ke duong loi nao xay ra truoc do (Gemini loi, saveExchange loi, hay chinh
     * saveAttachments loi), finally se goi attachmentProcessor.discard(attachments) de xoa
     * file da luu tren dia, tranh mo coi vinh vien. Client ngat ket noi giua chung KHONG anh
     * huong - BE van sinh tiep va luu binh thuong nen attachmentsLinked van len true.
     */
    private SseEmitter startStreaming(
            ChatMessageRequest request,
            Long patientId,
            HttpServletResponse response,
            List<ImagePart> images,
            String documentText,
            List<ChatAttachmentProcessor.AttachmentResult> attachments) {

        // Tat bo dem cua nginx cho rieng response nay - khong anh huong gi khi chay local
        // (Vite), nhung can thiet neu sau nay deploy sau nginx thi streaming van chay dung.
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");

        SseEmitter emitter = new SseEmitter(120_000L);

        // Bat len khi client ngat ket noi giua chung (dong tab, mat mang...). Dung
        // AtomicBoolean vi co nay duoc ghi tu thread cua Tomcat (qua onError/onTimeout, hoac
        // luc gui token that bai) nhung duoc doc trong virtual thread dang sinh cau tra loi.
        AtomicBoolean clientGone = new AtomicBoolean(false);
        emitter.onError(exception -> clientGone.set(true));
        emitter.onTimeout(() -> clientGone.set(true));

        Thread.startVirtualThread(() -> {
            Long[] alertIdHolder = new Long[1];
            boolean attachmentsLinked = attachments.isEmpty();
            try {
                StreamingRagQueryService.StreamingResult result = streamingRagQueryService.streamAnswer(
                        request.content(),
                        images,
                        documentText,
                        token -> {
                            if (clientGone.get()) {
                                return;
                            }
                            try {
                                emitter.send(
                                        SseEmitter.event()
                                                .data(token));
                            } catch (Exception exception) {
                                clientGone.set(true);
                            }
                        },
                        triageResult -> {
                            if (triageResult.emergency()) {
                                try {
                                    alertIdHolder[0] = triageAlertService.createAndPublish(
                                            patientId,
                                            null,
                                            null,
                                            request.content(),
                                            triageResult.matchedGroups());
                                } catch (Exception exception) {
                                    log.error(
                                            "Khong the tao/gui canh bao Triage (streaming) cho patientId={}: {}",
                                            patientId, exception.getMessage(), exception);
                                }
                            }
                        });

                ChatHistoryService.SavedExchange saved = null;
                try {
                    saved = chatHistoryService.saveExchange(
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

                if (saved != null && !attachments.isEmpty()) {
                    try {
                        chatHistoryService.saveAttachments(saved.patientMessageId(), attachments);
                        attachmentsLinked = true;
                    } catch (Exception exception) {
                        log.error("Khong the luu danh sach file dinh kem cho patientMessageId={}: {}",
                                saved.patientMessageId(), exception.getMessage(), exception);
                    }
                }

                if (alertIdHolder[0] != null && saved != null) {
                    try {
                        triageAlertService.linkConversation(
                                alertIdHolder[0], saved.sessionId(), saved.assistantMessageId());
                    } catch (Exception exception) {
                        log.error(
                                "Khong the gan sessionId/messageId vao canh bao Triage (streaming) id={}: {}",
                                alertIdHolder[0], exception.getMessage(), exception);
                    }
                }

                if (!clientGone.get()) {
                    try {
                        emitter.send(
                                SseEmitter.event()
                                        .name("done")
                                        .data(
                                                new StreamingDoneEvent(
                                                        result.sources(),
                                                        result.relatedQuestions(),
                                                        result.emergency(),
                                                        result.matchedGroups(),
                                                        saved != null ? saved.sessionId() : request.sessionId(),
                                                        saved != null ? saved.assistantMessageId() : null)));
                    } catch (Exception exception) {
                        // Client vua ngat dung luc gui done - bo qua.
                    }
                    try {
                        emitter.complete();
                    } catch (Exception exception) {
                        // Emitter co the da dong.
                    }
                }

            } catch (Exception exception) {
                if (!clientGone.get()) {
                    try {
                        emitter.send(
                                SseEmitter.event()
                                        .name("error")
                                        .data("Khong the nhan duoc cau tra loi tu AI. Vui long thu lai."));
                    } catch (Exception sendException) {
                        // Emitter co the da dong (client ngat ket noi) - bo qua.
                    }
                    emitter.completeWithError(exception);
                }
            } finally {
                if (!attachmentsLinked) {
                    attachmentProcessor.discard(attachments);
                }
            }
        });

        return emitter;
    }
}