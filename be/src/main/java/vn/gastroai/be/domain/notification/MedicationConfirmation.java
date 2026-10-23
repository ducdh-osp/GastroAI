package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** UC0016 - mot lan benh nhan bam "da uong thuoc" cho mot lieu cua MedicationReminder.
 * Scheduler dung ban ghi nay de khong nhac lai lieu da duoc xac nhan trong ngay. */
@Entity
@Table(name = "medication_confirmations")
@Getter
@Setter
@NoArgsConstructor
public class MedicationConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_id", nullable = false)
    private MedicationReminder reminder;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt = Instant.now();

    /**
     * Local calendar date of this daily dose in Vietnam. Legacy duplicate rows may have a
     * null value; every new confirmation supplies one and is protected by a unique index.
     */
    @Column(name = "confirmation_date")
    private LocalDate confirmationDate;

    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;

    public MedicationConfirmation(MedicationReminder reminder, Instant confirmedAt, LocalDate confirmationDate) {
        this(reminder, confirmedAt, confirmationDate, reminder.getTimeOfDay());
    }

    public MedicationConfirmation(MedicationReminder reminder, Instant confirmedAt,
                                  LocalDate confirmationDate, LocalTime scheduledTime) {
        this.reminder = reminder;
        this.confirmedAt = confirmedAt;
        this.confirmationDate = confirmationDate;
        this.scheduledTime = scheduledTime;
    }
}
