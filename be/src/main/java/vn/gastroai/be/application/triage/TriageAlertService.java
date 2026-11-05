package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertAction;
import vn.gastroai.be.domain.triage.TriageAlertActionType;
import vn.gastroai.be.domain.triage.TriageAlertActivityEvent;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertOutcome;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertActionRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertMessageRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TriageAlertService {

    private static final Logger log = LoggerFactory.getLogger(TriageAlertService.class);

    private static final String TX = "postgresTransactionManager";

    private static final Duration FALSE_ALARM_WINDOW = Duration.ofMinutes(30);

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NOTE_LENGTH = 1000;
    private static final int MIN_REASON_LENGTH = 3;

    private final TriageAlertRepository triageAlertRepository;
    private final TriageAlertActionRepository actionRepository;
    private final TriageAlertMessageRepository messageRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public TriageAlertService(
            TriageAlertRepository triageAlertRepository,
            TriageAlertActionRepository actionRepository,
            TriageAlertMessageRepository messageRepository,
            PatientRepository patientRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.triageAlertRepository = triageAlertRepository;
        this.actionRepository = actionRepository;
        this.messageRepository = messageRepository;
        this.patientRepository = patientRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Gop trung theo thu tu: (1) canh bao NEW/IN_PROGRESS gan nhat (khong gioi han thoi gian), (2) canh bao gan
     * nhat bi danh dau bao nham trong 30 phut, (3) tao moi. Chi (3) phat TriageAlertEvent.
     */
    @Transactional(TX)
    public Long createAndPublish(
            Long patientId,
            Long sessionId,
            Long messageId,
            String messageContent,
            List<String> matchedGroups) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy bệnh nhân id=" + patientId));

        Instant now = Instant.now();

        Optional<TriageAlert> openAlert = triageAlertRepository
                .findLatestOpenForPatient(patientId, PageRequest.of(0, 1))
                .stream().findFirst();
        if (openAlert.isPresent()) {
            return mergeRepeat(openAlert.get(), messageContent, matchedGroups, now,
                    TriageAlertActionType.REPEATED);
        }

        Optional<TriageAlert> recentFalseAlarm = triageAlertRepository
                .findFirstByPatient_IdOrderByOccurredAtDescIdDesc(patientId)
                .filter(alert -> alert.getStatus() == TriageAlertStatus.RESOLVED
                        && alert.getOutcome() == TriageAlertOutcome.FALSE_ALARM
                        && alert.getResolvedAt() != null
                        && alert.getResolvedAt().isAfter(now.minus(FALSE_ALARM_WINDOW)));
        if (recentFalseAlarm.isPresent()) {
            return mergeRepeat(recentFalseAlarm.get(), messageContent, matchedGroups, now,
                    TriageAlertActionType.REPEATED_AFTER_FALSE_ALARM);
        }

        TriageAlert alert = triageAlertRepository.save(new TriageAlert(
                patient, sessionId, messageId, messageContent, toJson(matchedGroups), now));
        recordAction(alert.getId(), TriageAlertActionType.CREATED, null, null, null);

        eventPublisher.publishEvent(new TriageAlertEvent(
                alert.getId(),
                patientId,
                patient.getFullName(),
                patient.getPhone(),
                sessionId,
                messageId,
                messageContent,
                matchedGroups,
                alert.getStatus().name(),
                now));

        return alert.getId();
    }

    private Long mergeRepeat(TriageAlert alert, String messageContent, List<String> matchedGroups,
            Instant now, TriageAlertActionType actionType) {
        Set<String> groups = new LinkedHashSet<>(fromJson(alert.getMatchedGroups()));
        if (matchedGroups != null) {
            groups.addAll(matchedGroups);
        }
        alert.setMatchedGroups(toJson(List.copyOf(groups)));
        alert.setLastMessageContent(messageContent);
        alert.setRepeatCount(alert.getRepeatCount() + 1);
        alert.setLastRepeatedAt(now);
        triageAlertRepository.save(alert);

        recordAction(alert.getId(), actionType, null, null, null);
        eventPublisher.publishEvent(new TriageAlertActivityEvent(alert.getId(), actionType.name(), now));
        return alert.getId();
    }

    /**
     * Gan phien chat cho canh bao vua tao. Canh bao da co phien (vd vua gop vao canh bao cu)
     * thi giu nguyen, khong phat lai su kien.
     */
    @Transactional(TX)
    public void linkConversation(Long alertId, Long sessionId, Long messageId) {
        Optional<TriageAlert> maybeAlert = triageAlertRepository.findById(alertId);

        if (maybeAlert.isEmpty()) {
            log.warn("Khong the gan sessionId/messageId: canh bao Triage id={} khong con ton tai",
                    alertId);
            return;
        }

        TriageAlert alert = maybeAlert.get();
        if (alert.getSessionId() != null || alert.getMessageId() != null) {
            return;
        }
        alert.setSessionId(sessionId);
        alert.setMessageId(messageId);
        triageAlertRepository.save(alert);

        if (alert.getRepeatCount() > 0) {
            return;
        }

        eventPublisher.publishEvent(new TriageAlertEvent(
                alert.getId(),
                alert.getPatient().getId(),
                alert.getPatient().getFullName(),
                alert.getPatient().getPhone(),
                sessionId,
                messageId,
                alert.getMessageContent(),
                fromJson(alert.getMatchedGroups()),
                alert.getStatus().name(),
                alert.getOccurredAt()));
    }

    /** view: OPEN (NEW + IN_PROGRESS, mac dinh), CLOSED (RESOLVED), ALL. */
    @Transactional(value = TX, readOnly = true)
    public Page<TriageAlertResponse> list(String view, int page, int size) {
        Set<TriageAlertStatus> statuses = switch (view == null ? "OPEN" : view.trim().toUpperCase()) {
            case "OPEN", "" -> EnumSet.of(TriageAlertStatus.NEW, TriageAlertStatus.IN_PROGRESS);
            case "CLOSED" -> EnumSet.of(TriageAlertStatus.RESOLVED);
            case "ALL" -> EnumSet.allOf(TriageAlertStatus.class);
            default -> throw new IllegalArgumentException("Bộ lọc không hợp lệ: " + view);
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE));
        Page<TriageAlert> result = triageAlertRepository.findPageByStatuses(statuses, pageable);
        return new PageImpl<>(toResponses(result.getContent()), pageable, result.getTotalElements());
    }

    /**
     * Chi tiet cho CMS. Chi nguoi dang tiep nhan xem moi danh dau da doc tin cua benh nhan;
     * nguoi khac (ke ca ADMIN) xem khong lam mat trang thai chua doc.
     */
    @Transactional(TX)
    public TriageAlertDetailResponse getDetail(Long alertId, Long viewerId, String viewerType) {
        TriageAlert alert = findAlertOrThrow(alertId);
        boolean viewerIsClaimer = viewerId.equals(alert.getClaimedById())
                && viewerType.equals(alert.getClaimedByType());
        if (viewerIsClaimer) {
            // Query @Modifying xoa persistence context -> doc lai entity sau UPDATE.
            messageRepository.markPatientMessagesRead(alertId, Instant.now());
            alert = findAlertOrThrow(alertId);
        }

        List<TriageAlertDetailResponse.ActionItem> actions = actionRepository
                .findByAlertIdOrderByCreatedAtAscIdAsc(alertId).stream()
                .map(action -> new TriageAlertDetailResponse.ActionItem(
                        action.getId(),
                        action.getActionType().name(),
                        action.getActorType(),
                        action.getActorId(),
                        action.getDetail(),
                        action.getCreatedAt()))
                .toList();

        List<TriageAlertDetailResponse.MessageItem> messages = messageRepository
                .findByAlertIdOrderByCreatedAtAscIdAsc(alertId).stream()
                .map(message -> new TriageAlertDetailResponse.MessageItem(
                        message.getId(),
                        message.getSenderType(),
                        message.getSenderId(),
                        message.getContent(),
                        message.getReadAt(),
                        message.getCreatedAt()))
                .toList();

        return new TriageAlertDetailResponse(toResponse(alert), actions, messages);
    }

    @Transactional(TX)
    public TriageAlertResponse claim(Long alertId, Long claimerId, String claimerType) {
        Instant now = Instant.now();
        int claimedRows = triageAlertRepository.claimIfNew(alertId, claimerId, claimerType, now);

        if (claimedRows == 1) {
            TriageAlert alert = findAlertOrThrow(alertId);
            recordAction(alertId, TriageAlertActionType.CLAIMED, claimerType, claimerId, null);
            broadcastStatusChange(alert, now, null, null);
            return toResponse(alert);
        }

        TriageAlert alert = findAlertOrThrow(alertId);

        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Cảnh báo id=" + alertId + " đã được xử lý xong, không thể tiếp nhận lại");
        }

        if (alert.getStatus() == TriageAlertStatus.IN_PROGRESS
                && claimerId.equals(alert.getClaimedById())
                && claimerType.equals(alert.getClaimedByType())) {
            return toResponse(alert);
        }

        throw new IllegalStateException(
                "Cảnh báo đã được " + claimerLabel(alert) + " #" + alert.getClaimedById() + " tiếp nhận");
    }

    /** Tra canh bao ve NEW de nguoi khac tiep nhan. ADMIN duoc tra lai ho. */
    @Transactional(TX)
    public TriageAlertResponse release(Long alertId, Long actorId, String actorType) {
        TriageAlert before = findAlertOrThrow(alertId);
        requireHandler(before, actorId, actorType, "trả lại");
        String previousClaimerType = before.getClaimedByType();
        Long previousClaimerId = before.getClaimedById();

        Instant now = Instant.now();
        int rows = triageAlertRepository.releaseIfClaimedBy(alertId, actorId, actorType, now);
        if (rows == 0 && "ADMIN".equals(actorType)) {
            rows = triageAlertRepository.releaseAsAdmin(alertId, now);
        }
        if (rows == 0) {
            throw concurrentUpdate(alertId);
        }

        boolean onBehalf = !actorId.equals(previousClaimerId) || !actorType.equals(previousClaimerType);
        String detail = onBehalf
                ? "Admin trả lại hộ, người tiếp nhận: " + previousClaimerType + " #" + previousClaimerId
                : null;
        recordAction(alertId, TriageAlertActionType.RELEASED, actorType, actorId, detail);

        TriageAlert alert = findAlertOrThrow(alertId);
        broadcastStatusChange(alert, now, null, null);
        return toResponse(alert);
    }

    /**
     * Danh dau xu ly xong (outcome HANDLED). Can it nhat 1 tin nhan cua nhan vien gui benh nhan.
     * ADMIN co quyen "dong ho" canh bao IN_PROGRESS cua nguoi khac - claimedById/claimedByType
     * giu nguyen de biet ai thuc su xu ly, chi resolvedByType ghi la ADMIN.
     */
    @Transactional(TX)
    public TriageAlertResponse resolve(Long alertId, Long resolverId, String resolverType, String note) {
        String normalizedNote = normalizeOptional(note);
        if (normalizedNote != null && normalizedNote.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú tối đa " + MAX_NOTE_LENGTH + " ký tự");
        }

        TriageAlert alert = findAlertOrThrow(alertId);
        requireHandler(alert, resolverId, resolverType, "đánh dấu xử lý xong");
        if (messageRepository.countStaffMessages(alertId) == 0) {
            throw new IllegalStateException(
                    "Cần gửi ít nhất 1 tin nhắn cho bệnh nhân trước khi đánh dấu xử lý xong");
        }

        return close(alert, resolverId, resolverType, TriageAlertOutcome.HANDLED, normalizedNote,
                TriageAlertActionType.RESOLVED);
    }

    /** Dong canh bao voi outcome FALSE_ALARM; ly do bat buoc. */
    @Transactional(TX)
    public TriageAlertResponse markFalseAlarm(Long alertId, Long actorId, String actorType, String reason) {
        String normalizedReason = normalizeOptional(reason);
        if (normalizedReason == null
                || normalizedReason.length() < MIN_REASON_LENGTH
                || normalizedReason.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException(
                    "Lý do báo nhầm phải từ " + MIN_REASON_LENGTH + " đến " + MAX_NOTE_LENGTH + " ký tự");
        }

        TriageAlert alert = findAlertOrThrow(alertId);
        requireHandler(alert, actorId, actorType, "đánh dấu báo nhầm");

        return close(alert, actorId, actorType, TriageAlertOutcome.FALSE_ALARM, normalizedReason,
                TriageAlertActionType.FALSE_ALARM);
    }

    private TriageAlertResponse close(TriageAlert alert, Long actorId, String actorType,
            TriageAlertOutcome outcome, String note, TriageAlertActionType actionType) {
        Long alertId = alert.getId();
        Instant now = Instant.now();
        int rows = triageAlertRepository.resolveIfClaimedBy(alertId, actorId, actorType, outcome, note, now);
        if (rows == 0 && "ADMIN".equals(actorType)) {
            rows = triageAlertRepository.resolveAsAdminOverride(alertId, actorId, outcome, note, now);
            if (rows == 1) {
                log.warn("Admin #{} dong ho canh bao #{} dang do {} #{} tiep nhan",
                        actorId, alertId, alert.getClaimedByType(), alert.getClaimedById());
            }
        }
        if (rows == 0) {
            throw concurrentUpdate(alertId);
        }

        recordAction(alertId, actionType, actorType, actorId, note);
        TriageAlert closed = findAlertOrThrow(alertId);
        broadcastStatusChange(closed, now, actorId, actorType);
        return toResponse(closed);
    }

    /**
     * Kiem tra canh bao dang IN_PROGRESS va nguoi goi la nguoi tiep nhan (hoac ADMIN).
     * Dung chung cho tra lai / dong / nhan tin benh nhan.
     */
    static void requireHandler(TriageAlert alert, Long actorId, String actorType, String actionLabel) {
        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            throw new IllegalStateException("Cảnh báo id=" + alert.getId() + " đã được đóng trước đó");
        }
        if (alert.getStatus() == TriageAlertStatus.NEW) {
            throw new IllegalStateException(
                    "Cảnh báo id=" + alert.getId() + " phải được tiếp nhận trước khi " + actionLabel);
        }
        boolean isClaimer = actorId.equals(alert.getClaimedById())
                && actorType.equals(alert.getClaimedByType());
        if (!isClaimer && !"ADMIN".equals(actorType)) {
            throw new IllegalStateException(
                    "Chỉ " + claimerLabel(alert) + " #" + alert.getClaimedById()
                            + " (người đã tiếp nhận) mới được " + actionLabel + " cảnh báo này");
        }
    }

    private static String claimerLabel(TriageAlert alert) {
        return "DOCTOR".equals(alert.getClaimedByType()) ? "Bác sĩ" : "Admin";
    }

    private static IllegalStateException concurrentUpdate(Long alertId) {
        return new IllegalStateException(
                "Cảnh báo id=" + alertId + " vừa được người khác cập nhật, vui lòng tải lại");
    }

    private TriageAlert findAlertOrThrow(Long alertId) {
        return triageAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy cảnh báo Triage id=" + alertId));
    }

    private void recordAction(Long alertId, TriageAlertActionType type, String actorType, Long actorId,
            String detail) {
        actionRepository.save(new TriageAlertAction(alertId, type, actorType, actorId, detail));
    }

    private void broadcastStatusChange(
            TriageAlert alert, Instant changedAt, Long resolvedById, String resolvedByType) {
        eventPublisher.publishEvent(new TriageAlertStatusChangedEvent(
                alert.getId(),
                alert.getStatus().name(),
                alert.getClaimedById(),
                alert.getClaimedByType(),
                resolvedById,
                resolvedByType,
                changedAt));
    }

    private TriageAlertResponse toResponse(TriageAlert alert) {
        return toResponses(List.of(alert)).get(0);
    }

    /** Dem so tin/so lan bao nham cho ca trang bang 3 truy van gop, tranh N+1. */
    private List<TriageAlertResponse> toResponses(List<TriageAlert> alerts) {
        if (alerts.isEmpty()) {
            return List.of();
        }
        List<Long> alertIds = alerts.stream().map(TriageAlert::getId).toList();
        Set<Long> patientIds = alerts.stream().map(alert -> alert.getPatient().getId()).collect(Collectors.toSet());

        Map<Long, Long> unread = messageRepository.countUnreadPatientMessagesByAlert(alertIds).stream()
                .collect(Collectors.toMap(
                        TriageAlertMessageRepository.AlertMessageCount::getAlertId,
                        TriageAlertMessageRepository.AlertMessageCount::getTotal));
        Map<Long, Long> staff = messageRepository.countStaffMessagesByAlert(alertIds).stream()
                .collect(Collectors.toMap(
                        TriageAlertMessageRepository.AlertMessageCount::getAlertId,
                        TriageAlertMessageRepository.AlertMessageCount::getTotal));
        Map<Long, Long> falseAlarms = triageAlertRepository.countFalseAlarmsByPatient(patientIds).stream()
                .collect(Collectors.toMap(
                        TriageAlertRepository.PatientAlertCount::getPatientId,
                        TriageAlertRepository.PatientAlertCount::getTotal));

        return alerts.stream().map(alert -> {
            // "Truoc day": khong tinh chinh canh bao nay neu no cung la bao nham.
            long previousFalseAlarms = falseAlarms.getOrDefault(alert.getPatient().getId(), 0L)
                    - (alert.getOutcome() == TriageAlertOutcome.FALSE_ALARM ? 1 : 0);
            return new TriageAlertResponse(
                    alert.getId(),
                    alert.getPatient().getId(),
                    alert.getPatient().getFullName(),
                    alert.getPatient().getPhone(),
                    alert.getSessionId(),
                    alert.getMessageId(),
                    alert.getMessageContent(),
                    fromJson(alert.getMatchedGroups()),
                    alert.getStatus().name(),
                    alert.getClaimedById(),
                    alert.getClaimedByType(),
                    alert.getClaimedAt(),
                    alert.getResolvedAt(),
                    alert.getResolvedById(),
                    alert.getResolvedByType(),
                    alert.getOccurredAt(),
                    alert.getOutcome() == null ? null : alert.getOutcome().name(),
                    alert.getResolutionNote(),
                    alert.getRepeatCount(),
                    alert.getLastMessageContent(),
                    alert.getLastRepeatedAt(),
                    unread.getOrDefault(alert.getId(), 0L),
                    staff.getOrDefault(alert.getId(), 0L),
                    Math.max(previousFalseAlarms, 0));
        }).toList();
    }

    private static String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toJson(List<String> value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }
}
