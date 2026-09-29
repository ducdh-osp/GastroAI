package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** UC0016 - 1 lan benh nhan bam "da uong thuoc" cho 1 MedicationReminder. GD4 chi la log
 * don gian (test thu cong), chua noi voi notification/scheduler that (GD7). */
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

    public MedicationConfirmation(MedicationReminder reminder) {
        this.reminder = reminder;
    }
}
