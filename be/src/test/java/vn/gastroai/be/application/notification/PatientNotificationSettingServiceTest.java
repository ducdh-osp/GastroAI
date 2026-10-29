package vn.gastroai.be.application.notification;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.notification.PatientNotificationSettingRequest;
import vn.gastroai.be.api.notification.PatientNotificationSettingResponse;
import vn.gastroai.be.domain.notification.PatientNotificationSetting;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientNotificationSettingRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatientNotificationSettingServiceTest {

    private final PatientNotificationSettingRepository repository = mock(PatientNotificationSettingRepository.class);
    private final PatientNotificationSettingService service = new PatientNotificationSettingService(repository);

    @Test
    void getSettingsReturnsDefaultsWhenNotPersisted() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        PatientNotificationSettingResponse response = service.getSettings(10L);

        assertTrue(response.medicationReminderEnabled());
        assertTrue(response.journalReminderEnabled());
        assertTrue(response.systemBroadcastEnabled());
        assertTrue(response.securityAlertEnabled());
    }

    @Test
    void updateSettingsPersistsAndReturnsNewValues() {
        PatientNotificationSetting setting = new PatientNotificationSetting(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(setting));
        when(repository.save(any(PatientNotificationSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientNotificationSettingRequest request = new PatientNotificationSettingRequest(
                false, true, false, true);

        PatientNotificationSettingResponse response = service.updateSettings(10L, request);

        assertFalse(response.medicationReminderEnabled());
        assertTrue(response.journalReminderEnabled());
        assertFalse(response.systemBroadcastEnabled());
        assertTrue(response.securityAlertEnabled());
        verify(repository).save(setting);
    }

    @Test
    void isMedicationReminderEnabledReflectsDatabaseState() {
        PatientNotificationSetting setting = new PatientNotificationSetting(10L, false, true, true, true);
        when(repository.findById(10L)).thenReturn(Optional.of(setting));

        assertFalse(service.isMedicationReminderEnabled(10L));
        assertTrue(service.isJournalReminderEnabled(10L));
    }
}
