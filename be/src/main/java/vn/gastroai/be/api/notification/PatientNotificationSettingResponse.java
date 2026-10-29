package vn.gastroai.be.api.notification;

import java.time.Instant;

public record PatientNotificationSettingResponse(
        boolean medicationReminderEnabled,
        boolean journalReminderEnabled,
        boolean systemBroadcastEnabled,
        boolean securityAlertEnabled,
        Instant updatedAt
) {
}
