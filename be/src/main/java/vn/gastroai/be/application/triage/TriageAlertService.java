package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static final Duration DEDUPE_WINDOW = Duration.ofMinutes(5);

    private final TriageAlertRepository triageAlertRepository;
    private final PatientRepository patientRepository;
    private final TriageAlertPublisher triageAlertPublisher;
    private final ObjectMapper objectMapper;

    public TriageAlertService(
            TriageAlertRepository triageAlertRepository,
            PatientRepository patientRepository,
            TriageAlertPublisher triageAlertPublisher,
            ObjectMapper objectMapper) {
        this.triageAlertRepository = triageAlertRepository;
        this.patientRepository = patientRepository;
        this.triageAlertPublisher = triageAlertPublisher;
        this.objectMapper = objectMapper;
    }

   
    @Transactional
    public void createAndPublish(
            Long patientId,
            Long sessionId,
            Long messageId,
            String messageContent,
            List<String> matchedGroups) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Khong tim thay benh nhan id=" + patientId));

        Instant occurredAt = Instant.now();

      
        Optional<TriageAlert> existingAlert = triageAlertRepository
                .findFirstByPatient_IdAndStatusNotAndOccurredAtAfterOrderByOccurredAtDesc(
                        patientId, TriageAlertStatus.RESOLVED, occurredAt.minus(DEDUPE_WINDOW));

        TriageAlert alert;
        if (existingAlert.isPresent()) {
            alert = existingAlert.get();
            alert.setSessionId(sessionId);
            alert.setMessageId(messageId);
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

        triageAlertPublisher.publish(event);
    }

    @Transactional(readOnly = true)
    public List<TriageAlertResponse> listRecent() {
        return triageAlertRepository.findTop50ByOrderByOccurredAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    
    @Transactional
    public TriageAlertResponse claim(Long alertId, Long claimerId, String claimerType) {
        TriageAlert alert = findAlertOrThrow(alertId);

        if (alert.getStatus() == TriageAlertStatus.RESOLVED) {
            throw new IllegalStateException(
                    "Canh bao id=" + alertId + " da duoc xu ly xong, khong the tiep nhan lai");
        }

        alert.setStatus(TriageAlertStatus.IN_PROGRESS);
        alert.setClaimedById(claimerId);
        alert.setClaimedByType(claimerType);
        alert.setClaimedAt(Instant.now());
        triageAlertRepository.save(alert);

        broadcastStatusChange(alert);
        return toResponse(alert);
    }

    @Transactional
    public TriageAlertResponse resolve(Long alertId) {
        TriageAlert alert = findAlertOrThrow(alertId);

        alert.setStatus(TriageAlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        triageAlertRepository.save(alert);

        broadcastStatusChange(alert);
        return toResponse(alert);
    }

    private TriageAlert findAlertOrThrow(Long alertId) {
        return triageAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Khong tim thay canh bao Triage id=" + alertId));
    }

    private void broadcastStatusChange(TriageAlert alert) {
        triageAlertPublisher.publishStatusChange(new TriageAlertStatusChangedEvent(
                alert.getId(),
                alert.getStatus().name(),
                alert.getClaimedById(),
                alert.getClaimedByType(),
                alert.getUpdatedAt()));
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