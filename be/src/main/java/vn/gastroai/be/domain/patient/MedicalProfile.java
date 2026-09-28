package vn.gastroai.be.domain.patient;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;

/**
 * UC0009/UC0010 - ho so benh ly ca nhan cua 1 benh nhan (tien su benh, di ung, thuoc dang
 * dung). 1 dong/1 patient (unique join column patient_id).
 * allergies/currentMedications luu JSON array of strings dang TEXT (serialize/deserialize
 * o tang service, giong cach ChatHistoryService lam voi sources/relatedQuestions).
 */
@Entity
@Table(name = "medical_profiles")
@Getter
@Setter
@NoArgsConstructor
public class MedicalProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    private Patient patient;

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "current_medications", columnDefinition = "TEXT")
    private String currentMedications;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public MedicalProfile(Patient patient, String medicalHistory, String allergies, String currentMedications) {
        this.patient = patient;
        this.medicalHistory = medicalHistory;
        this.allergies = allergies;
        this.currentMedications = currentMedications;
    }
}
