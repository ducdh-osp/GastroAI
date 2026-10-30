package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "email_outbox")
@Getter
@Setter
@NoArgsConstructor
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_code", nullable = false, length = 60)
    private EmailTemplateCode templateCode;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "encrypted_payload", nullable = false, columnDefinition = "TEXT")
    private String encryptedPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailOutboxStatus status = EmailOutboxStatus.PENDING;

    @Column(name = "dedupe_key", nullable = false, unique = true, length = 190)
    private String dedupeKey;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "medication_reminder_id")
    private Long medicationReminderId;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    @Column(name = "expected_token_hash", length = 64)
    private String expectedTokenHash;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public EmailOutbox(EmailTemplateCode templateCode, String recipientEmail,
                       String encryptedPayload, String dedupeKey, Instant nextAttemptAt) {
        this.templateCode = templateCode;
        this.recipientEmail = recipientEmail;
        this.encryptedPayload = encryptedPayload;
        this.dedupeKey = dedupeKey;
        this.nextAttemptAt = nextAttemptAt;
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
