package vn.gastroai.be.application.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.notification.PatientNotificationSettingRequest;
import vn.gastroai.be.api.notification.PatientNotificationSettingResponse;
import vn.gastroai.be.domain.notification.PatientNotificationSetting;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientNotificationSettingRepository;

import java.time.Instant;

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
