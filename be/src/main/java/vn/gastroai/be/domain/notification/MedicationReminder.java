package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;
import java.time.LocalTime;

/** UC0015 - lich nhac uong 1 loai thuoc, lap lai hang ngay vao 1 khung gio co dinh. */
@Entity
@Table(name = "medication_reminders")
@Getter
@Setter
@NoArgsConstructor
public class MedicationReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    @Column(length = 200)
    private String dosage;

    @Column(name = "time_of_day", nullable = false)
    private LocalTime timeOfDay;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public MedicationReminder(Patient patient, String medicineName, String dosage, LocalTime timeOfDay, boolean active) {
        this.patient = patient;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.timeOfDay = timeOfDay;
        this.active = active;
    }
}
