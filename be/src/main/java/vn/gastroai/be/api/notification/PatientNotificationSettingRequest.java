package vn.gastroai.be.api.notification;

import jakarta.validation.constraints.NotNull;

public record PatientNotificationSettingRequest(
        @NotNull(message = "medicationReminderEnabled khong duoc de trong")
        Boolean medicationReminderEnabled,

        @NotNull(message = "journalReminderEnabled khong duoc de trong")
        Boolean journalReminderEnabled,

        @NotNull(message = "systemBroadcastEnabled khong duoc de trong")
        Boolean systemBroadcastEnabled,

        @NotNull(message = "securityAlertEnabled khong duoc de trong")
        Boolean securityAlertEnabled
) {
}
