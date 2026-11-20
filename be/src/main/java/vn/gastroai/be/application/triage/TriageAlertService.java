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
import vn.gastroai.be.application.notification.AppNotificationService;
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
import vn.gastroai.be.domain.triage.TriageContactResult;
import vn.gastroai.be.domain.triage.TriagePatientResponse;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicalProfileRepository;
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

    /** actorType cua cac buoc he thong tu lam (bo hen gio, failsafe) tren timeline. */
    public static final String ACTOR_SYSTEM = "SYSTEM";
    public static final String ACTOR_PATIENT = "PATIENT";

    /**
     * Ket qua benh nhan tra loi "Ban co on khong?". alreadyClosed = ca da dong tu truoc
     * (benh nhan bam lai tren thong bao cu) - FE bao "Ca nay da duoc xu ly".
     */
    public record CheckInResult(Long alertId, String response, boolean alreadyClosed) {
    }

    private final TriageAlertRepository triageAlertRepository;
    private final TriageAlertActionRepository actionRepository;
    private final TriageAlertMessageRepository messageRepository;
    private final PatientRepository patientRepository;
    private final MedicalProfileRepository medicalProfileRepository;
    private final AppNotificationService appNotificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public TriageAlertService(
            TriageAlertRepository triageAlertRepository,
            TriageAlertActionRepository actionRepository,
            TriageAlertMessageRepository messageRepository,
            PatientRepository patientRepository,
            MedicalProfileRepository medicalProfileRepository,
            AppNotificationService appNotificationService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.triageAlertRepository = triageAlertRepository;
        this.actionRepository = actionRepository;
        this.messageRepository = messageRepository;
        this.patientRepository = patientRepository;
        this.medicalProfileRepository = medicalProfileRepository;
        this.appNotificationService = appNotificationService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Gop trung theo thu tu: (1) canh bao con mo gan nhat (khong gioi han thoi gian), (2) canh bao gan
     * nhat bi danh dau bao nham trong 30 phut, (3) tao moi. Chi (3) phat TriageAlertEvent va hoi tham
     * benh nhan "Ban co on khong?" (SCR-AD-13 buoc 1).
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
        // SCR-AD-13 buoc 1: hoi tham ngay, hien popup co 2 nut [An toan] [Can ho tro] tren app benh nhan.
        appNotificationService.recordTriageCheckIn(patientId, alert.getId(), false);
        recordAction(alert.getId(), TriageAlertActionType.CHECK_IN_SENT, ACTOR_SYSTEM, null, null);

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

    /**
     * view theo 3 tab SCR-AD-13: INTERVENTION (Can can thiep), AUTO (He thong dang tu xu ly),
     * CLOSED (Da xu ly); OPEN = ca 2 trang thai con mo (mac dinh), ALL = tat ca.
     */
    @Transactional(value = TX, readOnly = true)
    public Page<TriageAlertResponse> list(String view, int page, int size) {
        Set<TriageAlertStatus> statuses = switch (view == null ? "OPEN" : view.trim().toUpperCase()) {
            case "OPEN", "" -> EnumSet.of(TriageAlertStatus.AUTO_HANDLING, TriageAlertStatus.NEEDS_INTERVENTION);
            case "INTERVENTION" -> EnumSet.of(TriageAlertStatus.NEEDS_INTERVENTION);
            case "AUTO" -> EnumSet.of(TriageAlertStatus.AUTO_HANDLING);
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

        TriageAlertDetailResponse.EmergencyContact contact = null;
        // SCR-AD-13 buoc 3: moi Admin dang truc deu can thay so nguoi than de goi, khong chi nguoi tiep nhan.
        if ("ADMIN".equals(viewerType) && alert.getStatus() == TriageAlertStatus.NEEDS_INTERVENTION) {
            contact = medicalProfileRepository.findByPatientId(alert.getPatient().getId())
                    .filter(profile -> profile.getEmergencyContactPhone() != null)
                    .map(profile -> new TriageAlertDetailResponse.EmergencyContact(
                            profile.getEmergencyContactName(), profile.getEmergencyContactPhone(),
                            profile.getEmergencyContactRelationship()))
                    .orElse(null);
        }
        return new TriageAlertDetailResponse(toResponse(alert), actions, messages, contact);
    }

    // ===== Bo hen gio SCR-AD-13 (goi tu TriageEscalationScheduler) =====

    /**
     * Buoc 3: benh nhan im lang qua moc -> "Can can thiep", bao toi moi Admin dang truc.
     * Tra ve rong neu ca khong con o "dang tu xu ly" (benh nhan vua tra loi / da dong).
     */
    @Transactional(TX)
    public Optional<TriageAlertResponse> escalateIfStillSilent(Long alertId, Duration waited) {
        return escalate(alertId, ACTOR_SYSTEM, null,
                "Bệnh nhân không phản hồi sau " + waited.toMinutes() + " phút");
    }

    /**
     * Buoc 4 (failsafe): qua thoi han ma khong Admin nao ra tay -> bao dong lai moi Admin va nhac lai
     * benh nhan. Ca VAN o "Can can thiep" toi khi co Admin xu ly - failsafe la lop them, khong dong ca.
     */
    @Transactional(TX)
    public Optional<TriageAlertResponse> runFailsafeIfUnacknowledged(Long alertId, Duration waited) {
        Instant now = Instant.now();
        if (triageAlertRepository.markFailsafeIfDue(alertId, now) == 0) {
            return Optional.empty();
        }
        TriageAlert alert = findAlertOrThrow(alertId);
        Long patientId = alert.getPatient().getId();
        boolean hasContact = medicalProfileRepository.findByPatientId(patientId)
                .map(profile -> profile.getEmergencyContactPhone() != null)
                .orElse(false);

        String detail = "Không có Admin nào xác nhận trong " + waited.toMinutes()
                + " phút - đã báo động lại toàn bộ Admin và nhắc lại bệnh nhân"
                + (hasContact ? "" : ". Bệnh nhân chưa khai liên hệ khẩn cấp");
        recordAction(alertId, TriageAlertActionType.FAILSAFE, ACTOR_SYSTEM, null, detail);
        appNotificationService.recordTriageNeedsIntervention(
                alertId, alert.getPatient().getFullName(), "QUÁ HẠN - chưa Admin nào xử lý", true);
        appNotificationService.recordTriageCheckIn(patientId, alertId, true);
        eventPublisher.publishEvent(new TriageAlertActivityEvent(alertId, "FAILSAFE", now));
        return Optional.of(toResponse(alert));
    }

    // ===== Benh nhan tra loi "Ban co on khong?" =====

    /**
     * [An toan] -> dong ca (ket qua PATIENT_SAFE), ke ca khi da sang "Can can thiep".
     * [Can ho tro] -> chuyen "Can can thiep" ngay, khong cho het moc - benh nhan da chu dong xin giup.
     * Ca da dong thi bo qua, tra ve alreadyClosed = true (bam lai nut tren thong bao cu).
     */
    @Transactional(TX)
    public CheckInResult respondToCheckIn(Long patientId, Long alertId, TriagePatientResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("Vui lòng chọn An toàn hoặc Cần hỗ trợ");
        }
        TriageAlert alert = triageAlertRepository.findById(alertId)
                .filter(found -> patientId.equals(found.getPatient().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cảnh báo"));
        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            return new CheckInResult(alertId, response.name(), true);
        }
        boolean wasEscalated = alert.getStatus() == TriageAlertStatus.NEEDS_INTERVENTION;
        // Doc truoc: cac query UPDATE ben duoi xoa persistence context, entity nay se bi tach ra.
        String patientName = alert.getPatient().getFullName();
        Instant now = Instant.now();
        triageAlertRepository.recordPatientResponse(alertId, response, now);

        if (response == TriagePatientResponse.SAFE) {
            String note = wasEscalated
                    ? "Bệnh nhân báo an toàn (sau khi đã chuyển Admin)"
                    : "Bệnh nhân báo an toàn";
            recordAction(alertId, TriageAlertActionType.PATIENT_SAFE, ACTOR_PATIENT, patientId, null);
            if (triageAlertRepository.closeIfOpen(alertId, patientId, ACTOR_PATIENT,
                    TriageAlertOutcome.PATIENT_SAFE, note, now) == 1) {
                appNotificationService.markTriageAlertNotificationsRead(alertId);
                broadcastStatusChange(findAlertOrThrow(alertId), now, patientId, ACTOR_PATIENT);
            }
            return new CheckInResult(alertId, response.name(), false);
        }

        recordAction(alertId, TriageAlertActionType.PATIENT_NEEDS_HELP, ACTOR_PATIENT, patientId, null);
        // Chua "Can can thiep" thi chuyen ngay. Da o "Can can thiep" (hoac bo hen gio vua chuyen dung
        // luc benh nhan bam) thi bao dong lai de Admin biet benh nhan vua chu dong xin giup.
        boolean escalatedNow = !wasEscalated
                && escalate(alertId, ACTOR_PATIENT, patientId, "Bệnh nhân bấm Cần hỗ trợ").isPresent();
        if (!escalatedNow) {
            appNotificationService.recordTriageNeedsIntervention(
                    alertId, patientName, "Bệnh nhân bấm Cần hỗ trợ", true);
            eventPublisher.publishEvent(new TriageAlertActivityEvent(alertId, "PATIENT_NEEDS_HELP", now));
        }
        return new CheckInResult(alertId, response.name(), false);
    }

    private Optional<TriageAlertResponse> escalate(Long alertId, String actorType, Long actorId, String reason) {
        Instant now = Instant.now();
        if (triageAlertRepository.escalateIfAuto(alertId, now) == 0) {
            return Optional.empty();
        }
        recordAction(alertId, TriageAlertActionType.ESCALATED, actorType, actorId, reason);
        TriageAlert alert = findAlertOrThrow(alertId);
        appNotificationService.recordTriageNeedsIntervention(
                alertId, alert.getPatient().getFullName(), reason, false);
        broadcastStatusChange(alert, now, null, null);
        return Optional.of(toResponse(alert));
    }

    // ===== Admin (buoc 3) =====

    /**
     * "Toi dang lien he": danh dau de 2 Admin khong cung goi 1 nguoi than. Tu nguyen - Admin khac van
     * xu ly duoc (SCR-AD-13: 1 Admin bat ky). Bam o ca "dang tu xu ly" = can thiep som.
     */
    @Transactional(TX)
    public TriageAlertResponse claim(Long alertId, Long claimerId, String claimerType) {
        Instant now = Instant.now();
        TriageAlertStatus before = findAlertOrThrow(alertId).getStatus();
        int claimedRows = triageAlertRepository.claimIfOpen(alertId, claimerId, claimerType, now);

        if (claimedRows == 1) {
            TriageAlert alert = findAlertOrThrow(alertId);
            String detail = before == TriageAlertStatus.AUTO_HANDLING ? "Can thiệp sớm, trước mốc tự động" : null;
            recordAction(alertId, TriageAlertActionType.CLAIMED, claimerType, claimerId, detail);
            broadcastStatusChange(alert, now, null, null);
            return toResponse(alert);
        }

        TriageAlert alert = findAlertOrThrow(alertId);
        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Cảnh báo id=" + alertId + " đã được xử lý xong, không thể tiếp nhận lại");
        }
        if (claimerId.equals(alert.getClaimedById()) && claimerType.equals(alert.getClaimedByType())) {
            return toResponse(alert);
        }
        throw new IllegalStateException(
                claimerLabel(alert) + " #" + alert.getClaimedById() + " đang liên hệ cho ca này");
    }

    /** Bo "Toi dang lien he" - dong ho failsafe tinh lai tu luc tra. ADMIN duoc tra lai ho. */
    @Transactional(TX)
    public TriageAlertResponse release(Long alertId, Long actorId, String actorType) {
        TriageAlert before = findAlertOrThrow(alertId);
        if (before.getStatus() != TriageAlertStatus.NEEDS_INTERVENTION || before.getClaimedById() == null) {
            throw new IllegalStateException("Cảnh báo id=" + alertId + " chưa có ai tiếp nhận để trả lại");
        }
        boolean isClaimer = actorId.equals(before.getClaimedById()) && actorType.equals(before.getClaimedByType());
        if (!isClaimer && !"ADMIN".equals(actorType)) {
            throw new IllegalStateException(
                    "Chỉ " + claimerLabel(before) + " #" + before.getClaimedById() + " mới được trả lại ca này");
        }
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

        String detail = isClaimer
                ? null
                : "Admin trả lại hộ, người tiếp nhận: " + previousClaimerType + " #" + previousClaimerId;
        recordAction(alertId, TriageAlertActionType.RELEASED, actorType, actorId, detail);

        TriageAlert alert = findAlertOrThrow(alertId);
        broadcastStatusChange(alert, now, null, null);
        return toResponse(alert);
    }

    /**
     * Ghi ket qua lien he (goi nguoi than / benh nhan / 115). He thong khong tu goi duoc ai, nen day la
     * cach SCR-AD-13 "xac nhan escalate" duoc ghi lai. Ghi xong la Admin da ra tay -> failsafe dung.
     */
    @Transactional(TX)
    public TriageAlertResponse recordContact(Long alertId, Long actorId, String actorType,
                                             TriageContactResult result, String note) {
        if (result == null) {
            throw new IllegalArgumentException("Vui lòng chọn kết quả liên hệ");
        }
        String normalizedNote = normalizeOptional(note);
        if (normalizedNote != null && normalizedNote.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú tối đa " + MAX_NOTE_LENGTH + " ký tự");
        }
        TriageAlert alert = findAlertOrThrow(alertId);
        requireOpen(alert);
        TriageAlertStatus before = alert.getStatus();

        Instant now = Instant.now();
        if (triageAlertRepository.acknowledgeIfOpen(alertId, now) == 0) {
            throw concurrentUpdate(alertId);
        }
        String detail = result.label() + (normalizedNote == null ? "" : " · " + normalizedNote);
        recordAction(alertId, TriageAlertActionType.CONTACT_RECORDED, actorType, actorId, detail);

        TriageAlert updated = findAlertOrThrow(alertId);
        if (before != updated.getStatus()) {
            broadcastStatusChange(updated, now, null, null);
        } else {
            eventPublisher.publishEvent(new TriageAlertActivityEvent(alertId, "CONTACT_RECORDED", now));
        }
        return toResponse(updated);
    }

    /**
     * Dong ca (outcome HANDLED). Can da ghi it nhat 1 ket qua lien he (goi nguoi than / benh nhan / 115)
     * - khong dong ca ma khong de lai dau vet da lam gi.
     */
    @Transactional(TX)
    public TriageAlertResponse resolve(Long alertId, Long resolverId, String resolverType, String note) {
        String normalizedNote = normalizeOptional(note);
        if (normalizedNote != null && normalizedNote.length() > MAX_NOTE_LENGTH) {
            throw new IllegalArgumentException("Ghi chú tối đa " + MAX_NOTE_LENGTH + " ký tự");
        }

        TriageAlert alert = findAlertOrThrow(alertId);
        requireOpen(alert);
        boolean contacted = actionRepository.countByAlertIdAndActionType(
                alertId, TriageAlertActionType.CONTACT_RECORDED) > 0;
        if (!contacted) {
            throw new IllegalStateException("Cần ghi kết quả liên hệ trước khi đóng ca");
        }

        return close(alert, resolverId, resolverType, TriageAlertOutcome.HANDLED, normalizedNote,
                TriageAlertActionType.RESOLVED);
    }

    /** Dong canh bao voi outcome FALSE_ALARM; ly do bat buoc. Dung duoc o ca 2 trang thai con mo. */
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
        requireOpen(alert);

        return close(alert, actorId, actorType, TriageAlertOutcome.FALSE_ALARM, normalizedReason,
                TriageAlertActionType.FALSE_ALARM);
    }

    private TriageAlertResponse close(TriageAlert alert, Long actorId, String actorType,
            TriageAlertOutcome outcome, String note, TriageAlertActionType actionType) {
        Long alertId = alert.getId();
        Instant now = Instant.now();
        if (triageAlertRepository.closeIfOpen(alertId, actorId, actorType, outcome, note, now) == 0) {
            throw concurrentUpdate(alertId);
        }

        recordAction(alertId, actionType, actorType, actorId, note);
        appNotificationService.markTriageAlertNotificationsRead(alertId);
        TriageAlert closed = findAlertOrThrow(alertId);
        broadcastStatusChange(closed, now, actorId, actorType);
        return toResponse(closed);
    }

    /** Ca da dong thi khong thao tac duoc nua. */
    static void requireOpen(TriageAlert alert) {
        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            throw new IllegalStateException("Cảnh báo id=" + alert.getId() + " đã được đóng trước đó");
        }
    }

    /**
     * Nhan tin cho benh nhan: ca con mo; neu dang co nguoi tiep nhan thi chi nguoi do hoac ADMIN.
     * (SCR-AD-13 khong bat buoc tiep nhan truoc - 1 Admin bat ky deu xu ly duoc.)
     */
    static void requireHandler(TriageAlert alert, Long actorId, String actorType, String actionLabel) {
        requireOpen(alert);
        boolean claimedByOther = alert.getClaimedById() != null
                && (!actorId.equals(alert.getClaimedById()) || !actorType.equals(alert.getClaimedByType()));
        if (claimedByOther && !"ADMIN".equals(actorType)) {
            throw new IllegalStateException(
                    "Chỉ " + claimerLabel(alert) + " #" + alert.getClaimedById()
                            + " (người đang liên hệ) mới được " + actionLabel + " ca này");
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
                    Math.max(previousFalseAlarms, 0),
                    alert.getNeedsInterventionAt(),
                    alert.getAcknowledgedAt(),
                    alert.getFailsafeAt(),
                    alert.getPatientResponse() == null ? null : alert.getPatientResponse().name(),
                    alert.getPatientRespondedAt());
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
