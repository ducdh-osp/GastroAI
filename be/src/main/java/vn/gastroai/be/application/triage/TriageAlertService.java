package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertEvent;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.domain.triage.TriageAlertStatusChangedEvent;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageAlertRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class TriageAlertService {

    private static final Logger log = LoggerFactory.getLogger(TriageAlertService.class);

    private static final Duration DEDUPE_WINDOW = Duration.ofMinutes(5);

    private final TriageAlertRepository triageAlertRepository;
    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public TriageAlertService(
            TriageAlertRepository triageAlertRepository,
            PatientRepository patientRepository,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.triageAlertRepository = triageAlertRepository;
        this.patientRepository = patientRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Long createAndPublish(
            Long patientId,
            Long sessionId,
            Long messageId,
            String messageContent,
            List<String> matchedGroups) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy bệnh nhân id=" + patientId));

        Instant occurredAt = Instant.now();

        Optional<TriageAlert> existingAlert = triageAlertRepository
                .findFirstByPatient_IdAndStatusNotAndOccurredAtAfterOrderByOccurredAtDesc(
                        patientId, TriageAlertStatus.RESOLVED, occurredAt.minus(DEDUPE_WINDOW));

        TriageAlert alert;
        if (existingAlert.isPresent()) {
            alert = existingAlert.get();
            alert.setMessageContent(messageContent);
            alert.setMatchedGroups(toJson(matchedGroups));
            alert.setOccurredAt(occurredAt);
        } else {
            alert = new TriageAlert(
                    patient, sessionId, messageId, messageContent,
                    toJson(matchedGroups), occurredAt);
        }
        alert = triageAlertRepository.save(alert);

        TriageAlertEvent event = new TriageAlertEvent(
                alert.getId(),
                patientId,
                patient.getFullName(),
                patient.getPhone(),
                sessionId,
                messageId,
                messageContent,
                matchedGroups,
                alert.getStatus().name(),
                occurredAt);

        eventPublisher.publishEvent(event);

        return alert.getId();
    }

    @Transactional
    public void linkConversation(Long alertId, Long sessionId, Long messageId) {
        Optional<TriageAlert> maybeAlert = triageAlertRepository.findById(alertId);

        if (maybeAlert.isEmpty()) {
            log.warn("Khong the gan sessionId/messageId: canh bao Triage id={} khong con ton tai",
                    alertId);
            return;
        }

        TriageAlert alert = maybeAlert.get();
        alert.setSessionId(sessionId);
        alert.setMessageId(messageId);
        triageAlertRepository.save(alert);

        TriageAlertEvent event = new TriageAlertEvent(
                alert.getId(),
                alert.getPatient().getId(),
                alert.getPatient().getFullName(),
                alert.getPatient().getPhone(),
                sessionId,
                messageId,
                alert.getMessageContent(),
                fromJson(alert.getMatchedGroups()),
                alert.getStatus().name(),
                alert.getOccurredAt());

        eventPublisher.publishEvent(event);
    }

    @Transactional(readOnly = true)
    public List<TriageAlertResponse> listRecent() {
        return triageAlertRepository.findTop50ByOrderByOccurredAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TriageAlertResponse claim(Long alertId, Long claimerId, String claimerType) {
        Instant now = Instant.now();
        int claimedRows = triageAlertRepository.claimIfNew(alertId, claimerId, claimerType, now);

        if (claimedRows == 1) {
            TriageAlert alert = findAlertOrThrow(alertId);
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

        String claimerLabel = "DOCTOR".equals(alert.getClaimedByType()) ? "Bác sĩ" : "Admin";
        throw new IllegalStateException(
                "Cảnh báo đã được " + claimerLabel + " #" + alert.getClaimedById() + " tiếp nhận");
    }

    @Transactional
    public TriageAlertResponse resolve(Long alertId, Long resolverId, String resolverType) {
        Instant now = Instant.now();
        int resolvedRows = triageAlertRepository.resolveIfInProgress(alertId, resolverId, resolverType, now);

        if (resolvedRows == 1) {
            TriageAlert alert = findAlertOrThrow(alertId);
            broadcastStatusChange(alert, now, resolverId, resolverType);
            return toResponse(alert);
        }

        TriageAlert alert = findAlertOrThrow(alertId);

        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Cảnh báo id=" + alertId + " đã được xử lý xong trước đó");
        }

        throw new IllegalStateException(
                "Cảnh báo id=" + alertId + " phải được tiếp nhận trước khi đánh dấu đã xử lý");
    }

    private TriageAlert findAlertOrThrow(Long alertId) {
        return triageAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy cảnh báo Triage id=" + alertId));
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
                alert.getOccurredAt());
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