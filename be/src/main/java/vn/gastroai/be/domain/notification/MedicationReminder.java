package vn.gastroai.be.domain.notification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** UC0015 - lich nhac uong 1 loai thuoc, lap lai hang ngay vao 1 khung gio co dinh.
 * Day la nguon du lieu cho cac lieu can nhac trong app, doc lap voi danh sach thuoc tu khai
 * khong can nhac trong MedicalProfile.currentMedications. */
@Entity
@Table(name = "medication_reminders")
@Getter
@Setter
@NoArgsConstructor
public class MedicationReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reject stale updates from a second browser tab instead of silently losing changes. */
    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "medicine_name", nullable = false, length = 200)
    private String medicineName;

    @Column(length = 200)
    private String dosage;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "medication_reminder_times", joinColumns = @JoinColumn(name = "reminder_id"))
    @Column(name = "time_of_day", nullable = false)
    @OrderColumn(name = "sort_order")
    private List<LocalTime> timesOfDay = new ArrayList<>();

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String instructions;

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
        this(patient, medicineName, dosage, List.of(timeOfDay), null, null, null, active);
    }

    public MedicationReminder(Patient patient, String medicineName, String dosage, List<LocalTime> timesOfDay,
                              LocalDate startDate, LocalDate endDate, String instructions, boolean active) {
        this.patient = patient;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.timesOfDay = new ArrayList<>(timesOfDay);
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructions = instructions;
        this.active = active;
    }

    public LocalTime getTimeOfDay() {
        return timesOfDay.isEmpty() ? null : timesOfDay.get(0);
    }

    public void setTimeOfDay(LocalTime timeOfDay) {
        this.timesOfDay = new ArrayList<>(List.of(timeOfDay));
    }
}
