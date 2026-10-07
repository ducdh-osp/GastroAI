package vn.gastroai.be.application.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.chat.ChatSourceResponse;
import vn.gastroai.be.application.support.VietnamDateRange;
import vn.gastroai.be.domain.chat.ChatAttachment;
import vn.gastroai.be.domain.chat.ChatMessage;
import vn.gastroai.be.domain.chat.ChatSession;
import vn.gastroai.be.infrastructure.pdf.PdfDocumentBuilder;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** UC0027 - Xuat 1 phien chat (cau hoi + tra loi AI) ra PDF, dung lai PdfDocumentBuilder. */
@Service
public class ChatExportService {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(VietnamDateRange.ZONE);

    private final ChatHistoryService chatHistoryService;
    private final ObjectMapper objectMapper;

    public ChatExportService(ChatHistoryService chatHistoryService, ObjectMapper objectMapper) {
        this.chatHistoryService = chatHistoryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public byte[] exportSession(Long patientId, Long sessionId) {
        ChatSession session = chatHistoryService.getOwnedSession(patientId, sessionId);
        List<ChatMessage> messages = chatHistoryService.listMessages(patientId, sessionId);
        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();
        List<ChatAttachment> attachments = chatHistoryService.listAttachments(messageIds);
        Map<Long, List<String>> attachmentNamesByMessageId = attachments.stream()
                .collect(Collectors.groupingBy(a -> a.getMessage().getId(),
                        Collectors.mapping(ChatAttachment::getOriginalFilename, Collectors.toList())));

        PdfDocumentBuilder builder = new PdfDocumentBuilder();
        builder.title("PHIÊN TƯ VẤN SỨC KHỎE");
        builder.subtitle(session.getTitle() + " · Bắt đầu " + DATE_TIME_FORMAT.format(session.getCreatedAt())
                + " · Xuất lúc " + DATE_TIME_FORMAT.format(Instant.now()));
        builder.keyValue("Bệnh nhân", session.getPatient().getFullName());
        builder.keyValue("Số tin nhắn", String.valueOf(messages.size()));

        if (messages.isEmpty()) {
            builder.paragraph("Phiên chat chưa có tin nhắn nào.");
        } else {
            for (ChatMessage message : messages) {
                appendMessage(builder, message, attachmentNamesByMessageId);
            }
        }

        builder.note("Nội dung do trợ lý AI GastroAI tạo, chỉ mang tính tham khảo, "
                + "không thay thế chẩn đoán của bác sĩ.");
        return builder.build();
    }

    private void appendMessage(PdfDocumentBuilder builder, ChatMessage message,
                                Map<Long, List<String>> attachmentNamesByMessageId) {
        boolean isPatient = "patient".equals(message.getSender());
        String speaker = isPatient ? "Bạn" : "Trợ lý GastroAI";
        builder.messageHeader(speaker, DATE_TIME_FORMAT.format(message.getCreatedAt()));

        String content = isPatient ? message.getContent() : MarkdownText.toPlain(message.getContent());
        builder.paragraph(content);

        List<String> fileNames = attachmentNamesByMessageId.get(message.getId());
        if (fileNames != null && !fileNames.isEmpty()) {
            builder.keyValue("Tệp đính kèm", String.join(", ", fileNames));
        }

        if (message.isEmergency()) {
            builder.warning("Cảnh báo: phát hiện dấu hiệu cần đi khám ngay. "
                    + "Nếu đang nguy hiểm, gọi 115 hoặc đến cơ sở y tế gần nhất.");
        }

        if (!isPatient) {
            appendSources(builder, message.getSources());
        }
    }

    private void appendSources(PdfDocumentBuilder builder, String sourcesJson) {
        if (sourcesJson == null || sourcesJson.isBlank()) return;
        List<ChatSourceResponse> sources;
        try {
            sources = objectMapper.readValue(sourcesJson, new TypeReference<List<ChatSourceResponse>>() {});
        } catch (Exception e) {
            return;
        }
        if (sources.isEmpty()) return;
        String joined = sources.stream()
                .map(s -> s.sourceUrl() == null || s.sourceUrl().isBlank()
                        ? s.documentTitle()
                        : s.documentTitle() + " (" + s.sourceUrl() + ")")
                .collect(Collectors.joining("; "));
        builder.keyValue("Nguồn tham khảo", joined);
    }
}