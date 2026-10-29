package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * UC0073 - Cai dat thong bao ca nhan cho benh nhan: bat/tat tung loai nhac nho
 * theo y nguoi dung (uong thuoc, nhat ky, broadcast he thong, bao mat).
 */
@Entity
@Table(name = "patient_notification_settings")
@Getter
@Setter
@NoArgsConstructor
public class PatientNotificationSetting {

    @Id
    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "medication_reminder_enabled", nullable = false)
    private boolean medicationReminderEnabled = true;

    @Column(name = "journal_reminder_enabled", nullable = false)
    private boolean journalReminderEnabled = true;

    @Column(name = "system_broadcast_enabled", nullable = false)
    private boolean systemBroadcastEnabled = true;

    @Column(name = "security_alert_enabled", nullable = false)
    private boolean securityAlertEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public PatientNotificationSetting(Long patientId) {
        this.patientId = patientId;
        this.medicationReminderEnabled = true;
        this.journalReminderEnabled = true;
        this.systemBroadcastEnabled = true;
        this.securityAlertEnabled = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public PatientNotificationSetting(Long patientId, boolean medicationReminderEnabled,
                                      boolean journalReminderEnabled, boolean systemBroadcastEnabled,
                                      boolean securityAlertEnabled) {
        this.patientId = patientId;
        this.medicationReminderEnabled = medicationReminderEnabled;
        this.journalReminderEnabled = journalReminderEnabled;
        this.systemBroadcastEnabled = systemBroadcastEnabled;
        this.securityAlertEnabled = securityAlertEnabled;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }
}
