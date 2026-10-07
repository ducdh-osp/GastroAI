package vn.gastroai.be.application.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.chat.ChatSourceResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.chat.ChatAttachment;
import vn.gastroai.be.domain.chat.ChatMessage;
import vn.gastroai.be.domain.chat.ChatSession;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatExportServiceTest {

    private final ChatHistoryService chatHistoryService = mock(ChatHistoryService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ChatExportService service = new ChatExportService(chatHistoryService, objectMapper);

    private static final Long PATIENT_ID = 1L;
    private static final Long SESSION_ID = 10L;

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setFullName("Nguyễn Văn A");
        return patient;
    }

    private ChatSession session(Patient patient) {
        ChatSession session = new ChatSession(patient, "Đau bụng sau khi ăn");
        session.setId(SESSION_ID);
        session.setCreatedAt(Instant.parse("2026-09-01T08:00:00Z"));
        return session;
    }

    private ChatMessage message(ChatSession session, long id, String sender, String content,
                                 String sourcesJson, boolean emergency) {
        ChatMessage message = new ChatMessage(session, sender, content, sourcesJson, null, emergency, null);
        message.setId(id);
        message.setCreatedAt(Instant.parse("2026-09-01T08:05:00Z"));
        return message;
    }

    private String extractText(byte[] pdf) throws IOException {
        try (var document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void exportProducesPdfWithMessagesAndNoMarkdown() throws IOException {
        Patient patient = patient();
        ChatSession session = session(patient);
        ChatMessage patientMsg = message(session, 100L, "patient", "Tôi bị đau bụng sau khi ăn", null, false);
        ChatMessage aiMsg = message(session, 101L, "assistant", "**Lưu ý:** uống nhiều nước", null, false);

        when(chatHistoryService.getOwnedSession(PATIENT_ID, SESSION_ID)).thenReturn(session);
        when(chatHistoryService.listMessagesForSession(session)).thenReturn(List.of(patientMsg, aiMsg));
        when(chatHistoryService.listAttachments(List.of(100L, 101L))).thenReturn(List.of());

        String text = extractText(service.exportSession(PATIENT_ID, SESSION_ID));

        assertTrue(text.contains("Bạn"));
        assertTrue(text.contains("Trợ lý GastroAI"));
        assertTrue(text.contains("Tôi bị đau bụng sau khi ăn"));
        assertTrue(text.contains("Lưu ý: uống nhiều nước"));
        assertFalse(text.contains("**"));
    }

    @Test
    void exportIncludesWarningForEmergencyMessage() throws IOException {
        Patient patient = patient();
        ChatSession session = session(patient);
        ChatMessage aiMsg = message(session, 101L, "assistant", "Bạn cần đi khám ngay", null, true);

        when(chatHistoryService.getOwnedSession(PATIENT_ID, SESSION_ID)).thenReturn(session);
        when(chatHistoryService.listMessagesForSession(session)).thenReturn(List.of(aiMsg));
        when(chatHistoryService.listAttachments(List.of(101L))).thenReturn(List.of());

        String text = extractText(service.exportSession(PATIENT_ID, SESSION_ID));

        assertTrue(text.contains("Cảnh báo"));
    }

    @Test
    void exportIncludesAttachmentFileName() throws IOException {
        Patient patient = patient();
        ChatSession session = session(patient);
        ChatMessage patientMsg = message(session, 100L, "patient", "Đây là ảnh nội soi của tôi", null, false);
        ChatAttachment attachment = new ChatAttachment(patientMsg, "stored-123.png", "anh-noi-soi.png", "image/png", 2048);

        when(chatHistoryService.getOwnedSession(PATIENT_ID, SESSION_ID)).thenReturn(session);
        when(chatHistoryService.listMessagesForSession(session)).thenReturn(List.of(patientMsg));
        when(chatHistoryService.listAttachments(List.of(100L))).thenReturn(List.of(attachment));

        String text = extractText(service.exportSession(PATIENT_ID, SESSION_ID));

        assertTrue(text.contains("anh-noi-soi.png"));
    }

    @Test
    void exportIncludesSourceDocumentTitle() throws IOException {
        Patient patient = patient();
        ChatSession session = session(patient);
        String sourcesJson = objectMapper.writeValueAsString(
                List.of(new ChatSourceResponse("Tài liệu ABC", "snippet không in", "https://example.com")));
        ChatMessage aiMsg = message(session, 101L, "assistant", "Trả lời có nguồn", sourcesJson, false);

        when(chatHistoryService.getOwnedSession(PATIENT_ID, SESSION_ID)).thenReturn(session);
        when(chatHistoryService.listMessagesForSession(session)).thenReturn(List.of(aiMsg));
        when(chatHistoryService.listAttachments(List.of(101L))).thenReturn(List.of());

        String text = extractText(service.exportSession(PATIENT_ID, SESSION_ID));

        assertTrue(text.contains("Tài liệu ABC"));
        assertFalse(text.contains("snippet không in"));
    }

    @Test
    void exportSanitizesEmojiWithoutThrowing() throws IOException {
        Patient patient = patient();
        ChatSession session = session(patient);
        ChatMessage aiMsg = message(session, 101L, "assistant", "Chúc bạn mau khỏe 😊", null, false);

        when(chatHistoryService.getOwnedSession(PATIENT_ID, SESSION_ID)).thenReturn(session);
        when(chatHistoryService.listMessagesForSession(session)).thenReturn(List.of(aiMsg));
        when(chatHistoryService.listAttachments(List.of(101L))).thenReturn(List.of());

        String text = extractText(service.exportSession(PATIENT_ID, SESSION_ID));

        assertTrue(text.contains("Chúc bạn mau khỏe"));
    }

    @Test
    void exportEmptySessionShowsPlaceholderText() throws IOException {
        Patient patient = patient();
        ChatSession session = session(patient);

        when(chatHistoryService.getOwnedSession(PATIENT_ID, SESSION_ID)).thenReturn(session);
        when(chatHistoryService.listMessagesForSession(session)).thenReturn(List.of());
        when(chatHistoryService.listAttachments(List.of())).thenReturn(List.of());

        String text = extractText(service.exportSession(PATIENT_ID, SESSION_ID));

        assertTrue(text.contains("Phiên chat chưa có tin nhắn nào."));
    }

    @Test
    void exportPropagatesExceptionWhenNotOwner() {
        when(chatHistoryService.getOwnedSession(PATIENT_ID, SESSION_ID))
                .thenThrow(new ResourceNotFoundException("Khong tim thay phien chat hoac ban khong co quyen truy cap"));

        assertThrows(ResourceNotFoundException.class, () -> service.exportSession(PATIENT_ID, SESSION_ID));
    }
}