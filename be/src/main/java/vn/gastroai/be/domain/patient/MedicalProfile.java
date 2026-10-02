package vn.gastroai.be.domain.patient;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;
import java.time.LocalDate;

/**
 * UC0009/UC0010 - ho so benh ly ca nhan cua 1 benh nhan. 1 dong/1 patient (unique join
 * column patient_id).
 * allergies/chronicConditions/pastSurgeries/currentMedications/dietaryRestrictions luu
 * JSON array of strings dang TEXT (serialize/deserialize o tang service, giong cach
 * ChatHistoryService lam voi sources/relatedQuestions). currentMedications chi la thuoc
 * benh nhan tu khai khong can lich nhac trong app; no khong dong bo voi MedicationReminder.
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

    /**
     * Optimistic-lock token. The API returns this value and requires it on updates so a
     * form loaded in an older tab cannot silently overwrite a newer medical profile.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, unique = true)
    private Patient patient;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** "MALE" | "FEMALE" | "OTHER", de trong neu khong khai bao. */
    @Column(length = 10)
    private String gender;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "chronic_conditions", columnDefinition = "TEXT")
    private String chronicConditions;

    @Column(name = "past_surgeries", columnDefinition = "TEXT")
    private String pastSurgeries;

    @Column(name = "current_medications", columnDefinition = "TEXT")
    private String currentMedications;

    @Column(name = "dietary_restrictions", columnDefinition = "TEXT")
    private String dietaryRestrictions;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
