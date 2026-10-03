package vn.gastroai.be.api.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.chat.ChatHistoryService;
import vn.gastroai.be.api.support.AuthenticatedRequest;
import vn.gastroai.be.domain.chat.ChatAttachment;
import vn.gastroai.be.domain.chat.ChatMessage;
import vn.gastroai.be.domain.chat.ChatSession;
import vn.gastroai.be.infrastructure.filestorage.ChatAttachmentStorage;
import vn.gastroai.be.infrastructure.persistence.postgres.ChatAttachmentRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MessageRatingRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vn.gastroai.be.domain.chat.MessageRating;

/**
 * Lịch sử phiên chat của bệnh nhân:
 *   GET /api/v1/chat/sessions          — danh sách phiên (phân trang, mới nhất trước)
 *   GET /api/v1/chat/sessions/{id}     — toàn bộ tin nhắn trong 1 phiên
 *   DELETE /api/v1/chat/sessions/{id}  — xóa phiên của bệnh nhân đang đăng nhập
 */
@RestController
@RequestMapping("/api/v1/chat/sessions")
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    private final MessageRatingRepository ratingRepository;
    private final ChatAttachmentRepository attachmentRepository;
    private final ChatAttachmentStorage attachmentStorage;
    private final ObjectMapper objectMapper;

    public ChatHistoryController(ChatHistoryService chatHistoryService,
                                 MessageRatingRepository ratingRepository,
                                 ChatAttachmentRepository attachmentRepository,
                                 ChatAttachmentStorage attachmentStorage,
                                 ObjectMapper objectMapper) {
        this.chatHistoryService = chatHistoryService;
        this.ratingRepository = ratingRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentStorage = attachmentStorage;
        this.objectMapper = objectMapper;
    }

    /** Danh sách phiên chat của bệnh nhân đang đăng nhập, mới nhất trước. */
    @GetMapping
    public ChatSessionListResponse listSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
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
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        List<ChatMessage> messages = chatHistoryService.listMessages(patientId, sessionId);
        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();
        Map<Long, String> ratingsByMessageId = ratingRepository.findByMessageIdIn(messageIds).stream()
                .collect(Collectors.toMap(r -> r.getMessage().getId(), MessageRating::getRating));
        Map<Long, List<ChatAttachmentResponse>> attachmentsByMessageId = attachmentRepository
                .findByMessageIdIn(messageIds).stream()
                .collect(Collectors.groupingBy(a -> a.getMessage().getId(),
                        Collectors.mapping(a -> toAttachmentResponse(sessionId, a), Collectors.toList())));
        return messages.stream().map(msg -> toDetail(msg, ratingsByMessageId, attachmentsByMessageId)).toList();
    }

    /**
     * Tải lại nội dung 1 file đính kèm (ảnh/tài liệu bệnh nhân đã gửi kèm tin nhắn) - dùng cho
     * link trong lịch sử chat, vì URL blob: tạo khi gửi chỉ sống trong phiên trình duyệt đó,
     * mất ngay khi tải lại trang. getOwnedAttachment() đã kiểm tra attachment thuộc đúng
     * phiên của bệnh nhân này trước khi đọc file từ đĩa.
     */
    @GetMapping("/{sessionId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long sessionId,
            @PathVariable Long attachmentId,
            Principal principal, Authentication authentication) throws IOException {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        ChatAttachment attachment = chatHistoryService.getOwnedAttachment(patientId, sessionId, attachmentId);
        byte[] content = Files.readAllBytes(attachmentStorage.resolve(attachment.getStoredName()));
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(attachment.getOriginalFilename())
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(content);
    }

    /** Xóa phiên chat và toàn bộ tin nhắn/đánh giá đi kèm của bệnh nhân hiện tại. */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long sessionId,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        chatHistoryService.deleteSession(patientId, sessionId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────── helpers ────────────────────────────────

    private ChatMessageDetail toDetail(ChatMessage msg, Map<Long, String> ratingsByMessageId,
                                        Map<Long, List<ChatAttachmentResponse>> attachmentsByMessageId) {
        List<ChatSourceResponse> sources = parseJson(msg.getSources(),
                new TypeReference<List<ChatSourceResponse>>() {});
        List<String> related = parseJson(msg.getRelatedQuestions(),
                new TypeReference<List<String>>() {});
        List<String> matchedGroups = parseJson(msg.getMatchedGroups(),
                new TypeReference<List<String>>() {});
        String rating = ratingsByMessageId.get(msg.getId());
        List<ChatAttachmentResponse> attachments = attachmentsByMessageId.getOrDefault(msg.getId(), List.of());
        return new ChatMessageDetail(msg.getId(), msg.getSender(), msg.getContent(),
                msg.getCreatedAt(), msg.isEmergency(), sources, related, matchedGroups, rating, attachments);
    }

    /**
     * url la duong dan TUONG DOI so voi API root (khong co prefix /api/v1) - FE phai goi qua
     * apiClient (axios, tu gan header Authorization) roi tu doc blob, KHONG dung truc tiep
     * lam <img src>/<a href> vi endpoint nay yeu cau JWT ma browser load anh/link thuong
     * khong gui header do (xem downloadAttachment() ben duoi, ChatHistoryController).
     */
    private ChatAttachmentResponse toAttachmentResponse(Long sessionId, ChatAttachment attachment) {
        String url = "/chat/sessions/" + sessionId + "/attachments/" + attachment.getId();
        return new ChatAttachmentResponse(attachment.getId(), attachment.getOriginalFilename(),
                attachment.getContentType(), attachment.getSizeBytes(), url);
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
