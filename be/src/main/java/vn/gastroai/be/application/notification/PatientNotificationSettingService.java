package vn.gastroai.be.application.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.notification.PatientNotificationSettingRequest;
import vn.gastroai.be.api.notification.PatientNotificationSettingResponse;
import vn.gastroai.be.domain.notification.PatientNotificationSetting;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientNotificationSettingRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UC0073 - Quan ly cai dat thong bao ca nhan cua benh nhan.
 */
@Service
public class PatientNotificationSettingService {

    private final PatientNotificationSettingRepository repository;

    public PatientNotificationSettingService(PatientNotificationSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public PatientNotificationSettingResponse getSettings(Long patientId) {
        return repository.findById(patientId)
                .map(this::toResponse)
                .orElseGet(() -> new PatientNotificationSettingResponse(true, true, true, true, Instant.now()));
    }

    @Transactional("postgresTransactionManager")
    public PatientNotificationSettingResponse updateSettings(Long patientId, PatientNotificationSettingRequest request) {
        PatientNotificationSetting setting = repository.findById(patientId)
                .orElseGet(() -> new PatientNotificationSetting(patientId));

        setting.setMedicationReminderEnabled(request.medicationReminderEnabled());
        setting.setJournalReminderEnabled(request.journalReminderEnabled());
        setting.setSystemBroadcastEnabled(request.systemBroadcastEnabled());
        setting.setSecurityAlertEnabled(request.securityAlertEnabled());

        setting = repository.save(setting);
        return toResponse(setting);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public boolean isMedicationReminderEnabled(Long patientId) {
        return repository.findById(patientId)
                .map(PatientNotificationSetting::isMedicationReminderEnabled)
                .orElse(true);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public boolean isJournalReminderEnabled(Long patientId) {
        return repository.findById(patientId)
                .map(PatientNotificationSetting::isJournalReminderEnabled)
                .orElse(true);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public boolean isSystemBroadcastEnabled(Long patientId) {
        return repository.findById(patientId)
                .map(PatientNotificationSetting::isSystemBroadcastEnabled)
                .orElse(true);
    }

    /**
     * Batch-check settings for a list of candidate patient IDs to avoid N+1 queries.
     * Retrieves all existing settings in batches of 1000 and filters in-memory.
     * Patients without saved settings default to enabled (opt-in).
     */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<Long> filterSystemBroadcastEnabledPatientIds(List<Long> candidateIds) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Boolean> settingsMap = new HashMap<>();
        int batchSize = 1000;
        for (int i = 0; i < candidateIds.size(); i += batchSize) {
            List<Long> batch = candidateIds.subList(i, Math.min(i + batchSize, candidateIds.size()));
            for (PatientNotificationSetting setting : repository.findAllById(batch)) {
                settingsMap.put(setting.getPatientId(), setting.isSystemBroadcastEnabled());
            }
        }

        return candidateIds.stream()
                .filter(patientId -> settingsMap.getOrDefault(patientId, true))
                .toList();
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public boolean isSecurityAlertEnabled(Long patientId) {
        return repository.findById(patientId)
                .map(PatientNotificationSetting::isSecurityAlertEnabled)
                .orElse(true);
    }

    private PatientNotificationSettingResponse toResponse(PatientNotificationSetting setting) {
        return new PatientNotificationSettingResponse(
                setting.isMedicationReminderEnabled(),
                setting.isJournalReminderEnabled(),
                setting.isSystemBroadcastEnabled(),
                setting.isSecurityAlertEnabled(),
                setting.getUpdatedAt()
        );
    }
}
