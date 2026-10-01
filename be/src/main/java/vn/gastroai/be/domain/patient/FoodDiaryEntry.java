package vn.gastroai.be.domain.patient;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;

/** UC0011/UC0012 - 1 lan an duoc ghi nhan trong nhat ky an uong cua benh nhan. */
@Entity
@Table(name = "food_diary_entries")
@Getter
@Setter
@NoArgsConstructor
public class FoodDiaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "eaten_at", nullable = false)
    private Instant eatenAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType = MealType.OTHER;

    @Column(name = "symptoms_after_meal", columnDefinition = "TEXT")
    private String symptomsAfterMeal;

    @Column(name = "symptom_onset_minutes")
    private Integer symptomOnsetMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public FoodDiaryEntry(Patient patient, Instant eatenAt, String description, String notes) {
        this(patient, eatenAt, description, MealType.OTHER, null, null, notes);
    }

    public FoodDiaryEntry(Patient patient, Instant eatenAt, String description, MealType mealType,
                          String symptomsAfterMeal, Integer symptomOnsetMinutes, String notes) {
        this.patient = patient;
        this.eatenAt = eatenAt;
        this.description = description;
        this.mealType = mealType;
        this.symptomsAfterMeal = symptomsAfterMeal;
        this.symptomOnsetMinutes = symptomOnsetMinutes;
        this.notes = notes;
    }
}
