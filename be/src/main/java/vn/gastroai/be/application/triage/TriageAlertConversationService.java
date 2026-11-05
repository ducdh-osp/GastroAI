package vn.gastroai.be.application.triage;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertAction;
import vn.gastroai.be.domain.triage.TriageAlertActionType;
import vn.gastroai.be.domain.triage.TriageAlertActivityEvent;
import vn.gastroai.be.domain.triage.TriageAlertMessage;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertActionRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertMessageRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Trao doi 2 chieu nhan vien <-> benh nhan trong 1 canh bao (tach khoi chatbot). */
@Service
public class TriageAlertConversationService {

    private static final String TX = "postgresTransactionManager";
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int PREVIEW_LENGTH = 80;

    public record StaffMessageResult(
            Long alertId,
            Long patientId,
            TriageAlertDetailResponse.MessageItem message) {
    }

    public record PatientMessageItem(
            Long id,
            String senderLabel,
            boolean mine,
            String content,
            Instant createdAt) {
    }

    public record PatientConversationView(
            Long id,
            String status,
            Instant occurredAt,
            List<PatientMessageItem> messages,
            boolean canReply) {
    }

    public record PatientConversationSummary(
            Long id,
            Instant occurredAt,
            String status,
            String lastMessagePreview,
            Instant lastMessageAt,
            long unreadCount) {
    }

    private final TriageAlertRepository triageAlertRepository;
    private final TriageAlertMessageRepository messageRepository;
    private final TriageAlertActionRepository actionRepository;
    private final AppNotificationService appNotificationService;
    private final ApplicationEventPublisher eventPublisher;

    public TriageAlertConversationService(
            TriageAlertRepository triageAlertRepository,
            TriageAlertMessageRepository messageRepository,
            TriageAlertActionRepository actionRepository,
            AppNotificationService appNotificationService,
            ApplicationEventPublisher eventPublisher) {
        this.triageAlertRepository = triageAlertRepository;
        this.messageRepository = messageRepository;
        this.actionRepository = actionRepository;
        this.appNotificationService = appNotificationService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(TX)
    public StaffMessageResult sendStaffMessage(Long alertId, Long staffId, String staffType, String content) {
        String text = normalizeContent(content);
        TriageAlert alert = triageAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy cảnh báo Triage id=" + alertId));
        TriageAlertService.requireHandler(alert, staffId, staffType, "nhắn tin cho bệnh nhân");
        Long patientId = alert.getPatient().getId();

        TriageAlertMessage message = messageRepository.save(
                new TriageAlertMessage(alertId, staffType, staffId, text));
        actionRepository.save(new TriageAlertAction(
                alertId, TriageAlertActionType.MESSAGE_SENT, staffType, staffId, null));
        appNotificationService.recordTriageDoctorMessage(patientId, alertId, message.getId(), text);
        publishActivity(alertId, "MESSAGE_SENT");

        return new StaffMessageResult(alertId, patientId, new TriageAlertDetailResponse.MessageItem(
                message.getId(),
                message.getSenderType(),
                message.getSenderId(),
                message.getContent(),
                message.getReadAt(),
                message.getCreatedAt()));
    }

    /** Danh sach cuoc trao doi (canh bao da co tin cua nhan vien), tin moi nhat len dau. */
    @Transactional(value = TX, readOnly = true)
    public List<PatientConversationSummary> listForPatient(Long patientId) {
        List<TriageAlert> alerts = triageAlertRepository.findWithStaffMessagesForPatient(patientId);
        if (alerts.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TriageAlertMessage>> messagesByAlert = messageRepository
                .findByAlertIdInOrderByCreatedAtAscIdAsc(alerts.stream().map(TriageAlert::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(TriageAlertMessage::getAlertId));

        return alerts.stream()
                .map(alert -> {
                    List<TriageAlertMessage> messages = messagesByAlert.getOrDefault(alert.getId(), List.of());
                    TriageAlertMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
                    long unread = messages.stream()
                            .filter(message -> !message.fromPatient() && message.getReadAt() == null)
                            .count();
                    return new PatientConversationSummary(
                            alert.getId(),
                            alert.getOccurredAt(),
                            alert.getStatus().name(),
                            last == null ? null : preview(last.getContent()),
                            last == null ? alert.getOccurredAt() : last.getCreatedAt(),
                            unread);
                })
                .sorted(Comparator.comparing(PatientConversationSummary::lastMessageAt).reversed())
                .toList();
    }

    private static String preview(String content) {
        return content.length() > PREVIEW_LENGTH ? content.substring(0, PREVIEW_LENGTH) : content;
    }

    /** Benh nhan mo trang trao doi: danh dau da xem tin cua nhan vien. */
    @Transactional(TX)
    public PatientConversationView getForPatient(Long patientId, Long alertId) {
        TriageAlert alert = findOwnedAlert(patientId, alertId);
        // Doc truoc khi UPDATE: query @Modifying xoa persistence context.
        TriageAlertStatus status = alert.getStatus();
        Instant occurredAt = alert.getOccurredAt();

        int marked = messageRepository.markStaffMessagesRead(alertId, Instant.now());
        if (marked > 0) {
            actionRepository.save(new TriageAlertAction(
                    alertId, TriageAlertActionType.PATIENT_READ, TriageAlertMessage.SENDER_PATIENT, patientId, null));
            publishActivity(alertId, "PATIENT_READ");
        }

        List<TriageAlertMessage> messages = messageRepository.findByAlertIdOrderByCreatedAtAscIdAsc(alertId);
        boolean hasStaffMessage = messages.stream().anyMatch(message -> !message.fromPatient());
        boolean canReply = status != TriageAlertStatus.RESOLVED && hasStaffMessage;

        return new PatientConversationView(
                alertId,
                status.name(),
                occurredAt,
                messages.stream().map(TriageAlertConversationService::toPatientItem).toList(),
                canReply);
    }

    /** Benh nhan chi duoc tra loi sau khi nhan vien da nhan it nhat 1 tin. */
    @Transactional(TX)
    public PatientMessageItem replyAsPatient(Long patientId, Long alertId, String content) {
        String text = normalizeContent(content);
        TriageAlert alert = findOwnedAlert(patientId, alertId);
        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            throw new IllegalStateException("Cảnh báo đã được đóng, không thể gửi thêm tin nhắn");
        }
        if (messageRepository.countStaffMessages(alertId) == 0) {
            throw new IllegalStateException("Bác sĩ chưa liên hệ nên bạn chưa thể gửi tin nhắn tại đây");
        }

        TriageAlertMessage message = messageRepository.save(
                new TriageAlertMessage(alertId, TriageAlertMessage.SENDER_PATIENT, patientId, text));
        actionRepository.save(new TriageAlertAction(
                alertId, TriageAlertActionType.PATIENT_REPLIED, TriageAlertMessage.SENDER_PATIENT, patientId, null));
        publishActivity(alertId, "PATIENT_MESSAGE");
        return toPatientItem(message);
    }

    private TriageAlert findOwnedAlert(Long patientId, Long alertId) {
        return triageAlertRepository.findById(alertId)
                .filter(alert -> patientId.equals(alert.getPatient().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cảnh báo"));
    }

    private void publishActivity(Long alertId, String type) {
        eventPublisher.publishEvent(new TriageAlertActivityEvent(alertId, type, Instant.now()));
    }

    private static PatientMessageItem toPatientItem(TriageAlertMessage message) {
        String label = switch (message.getSenderType()) {
            case TriageAlertMessage.SENDER_PATIENT -> "Bạn";
            case "DOCTOR" -> "Bác sĩ";
            default -> "Nhân viên y tế";
        };
        return new PatientMessageItem(
                message.getId(), label, message.fromPatient(), message.getContent(), message.getCreatedAt());
    }

    private static String normalizeContent(String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Nội dung tin nhắn không được để trống");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Nội dung tin nhắn tối đa " + MAX_CONTENT_LENGTH + " ký tự");
        }
        return text;
    }
}
