package vn.gastroai.be.application.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.OwnedResourceLoader;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.rag.RagAnswer;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.chat.ChatMessage;
import vn.gastroai.be.domain.chat.ChatSession;
import vn.gastroai.be.domain.chat.MessageRating;
import vn.gastroai.be.infrastructure.persistence.postgres.ChatMessageRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.ChatSessionRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MessageRatingRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.util.List;

/**
 * Quản lý lịch sử phiên chat và đánh giá phản hồi AI.
 * <p>
 * Luồng cơ bản:
 * <ol>
 *   <li>FE gửi câu hỏi → ChatController gọi ChatService.ask() như cũ.</li>
 *   <li>ChatController gọi thêm ChatHistoryService.saveExchange() để lưu cả tin nhắn
 *       patient lẫn câu trả lời assistant vào DB.</li>
 *   <li>FE hiển thị nút 👍/👎 dưới câu trả lời → gọi POST /api/v1/chat/messages/{id}/rating.</li>
 *   <li>ChatController gọi ChatHistoryService.rateMessage() để UPSERT đánh giá.</li>
 * </ol>
 */
@Service
public class ChatHistoryService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageRatingRepository ratingRepository;
    private final PatientRepository patientRepository;
    private final ObjectMapper objectMapper;

    public ChatHistoryService(ChatSessionRepository sessionRepository,
                              ChatMessageRepository messageRepository,
                              MessageRatingRepository ratingRepository,
                              PatientRepository patientRepository,
                              ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.ratingRepository = ratingRepository;
        this.patientRepository = patientRepository;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────── Session ────────────────────────────────

    /**
     * Lưu cả cặp (câu hỏi patient + câu trả lời assistant) vào phiên chat.
     * Nếu sessionId == null → tạo phiên mới, đặt title = 60 ký tự đầu của câu hỏi.
     * Trả về SavedExchange chứa sessionId và messageId của câu trả lời assistant
     * (FE cần messageId để gọi rating sau).
     */
    @Transactional("postgresTransactionManager")
    public SavedExchange saveExchange(Long patientId, Long sessionId,
                                     String question, RagAnswer ragAnswer, boolean emergency,
                                     List<String> matchedGroups) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay benh nhan"));

        // Lấy hoặc tạo phiên
        ChatSession session;
        if (sessionId != null) {
            session = OwnedResourceLoader.loadOwned(sessionRepository.findById(sessionId),
                    s -> s.getPatient().getId().equals(patientId),
                    "Khong tim thay phien chat hoac ban khong co quyen truy cap");
        } else {
            String title = question.length() > 60 ? question.substring(0, 60) + "…" : question;
            session = sessionRepository.save(new ChatSession(patient, title));
        }

        // Lưu tin nhắn của patient
        messageRepository.save(new ChatMessage(session, "patient", question, null, null, false, null));

        // Serialize sources + relatedQuestions
        String sourcesJson = toJson(ragAnswer.sources());
        String questionsJson = toJson(ragAnswer.relatedQuestions());
        String matchedGroupsJson = toJson(matchedGroups);

        // Lưu câu trả lời assistant
        ChatMessage assistantMsg = messageRepository.save(
                new ChatMessage(session, "assistant", ragAnswer.answer(),
                        sourcesJson, questionsJson, emergency, matchedGroupsJson));

        // Cập nhật updatedAt của session để sort đúng "mới nhất trước"
        session.setUpdatedAt(assistantMsg.getCreatedAt());
        sessionRepository.save(session);

        return new SavedExchange(session.getId(), assistantMsg.getId());
    }

    /** Danh sách phiên chat phân trang, mới nhất trước, của 1 bệnh nhân. */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public Page<ChatSession> listSessions(Long patientId, int page, int size) {
        return sessionRepository.findByPatientId(patientId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    /** Toàn bộ tin nhắn trong 1 phiên (kiểm tra quyền sở hữu). */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<ChatMessage> listMessages(Long patientId, Long sessionId) {
        ChatSession session = OwnedResourceLoader.loadOwned(sessionRepository.findById(sessionId),
                s -> s.getPatient().getId().equals(patientId),
                "Khong tim thay phien chat hoac ban khong co quyen truy cap");
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
    }

    // ─────────────────────────────── Rating ─────────────────────────────────

    /**
     * UPSERT đánh giá (HELPFUL / UNHELPFUL) của bệnh nhân cho 1 câu trả lời assistant.
     * Kiểm tra: message phải thuộc phiên của đúng bệnh nhân + sender phải là 'assistant'.
     */
    @Transactional("postgresTransactionManager")
    public void rateMessage(Long patientId, Long messageId, String rating) {
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tin nhan"));

        if (!message.getSession().getPatient().getId().equals(patientId)) {
            throw new ResourceNotFoundException("Khong tim thay tin nhan hoac ban khong co quyen truy cap");
        }
        if (!"assistant".equals(message.getSender())) {
            throw new IllegalArgumentException("Chỉ được đánh giá câu trả lời của AI");
        }
        if (!"HELPFUL".equals(rating) && !"UNHELPFUL".equals(rating)) {
            throw new IllegalArgumentException("Rating phải là HELPFUL hoặc UNHELPFUL");
        }

        // UPSERT: tìm rating cũ nếu có, cập nhật; nếu không có thì tạo mới
        MessageRating mr = ratingRepository.findByMessageId(messageId)
                .orElseGet(() -> new MessageRating(message, rating));
        mr.setRating(rating);
        mr.setRatedAt(java.time.Instant.now());
        ratingRepository.save(mr);
    }

    // ─────────────────────────────── Helpers ────────────────────────────────

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /** Kết quả trả về sau khi lưu một lượt trao đổi chat. */
    public record SavedExchange(Long sessionId, Long assistantMessageId) {}
}
