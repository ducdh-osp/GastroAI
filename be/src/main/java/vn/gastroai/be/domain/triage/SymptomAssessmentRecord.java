package vn.gastroai.be.domain.triage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.gastroai.be.domain.auth.Patient;

import java.time.Instant;

/** Immutable patient-owned record of one UC0039/0040 symptom assessment. */
@Entity
@Table(name = "symptom_assessments")
@Getter
@Setter
@NoArgsConstructor
public class SymptomAssessmentRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "primary_symptom", nullable = false, length = 40)
    private String primarySymptom;

    @Column(name = "primary_symptom_detail", length = 500)
    private String primarySymptomDetail;

    @Column(nullable = false, length = 40)
    private String duration;

    @Column(name = "reported_severity", nullable = false, length = 20)
    private String reportedSeverity;

    @Column(name = "activity_impact", nullable = false, length = 40)
    private String activityImpact;

    @Column(nullable = false, length = 20)
    private String progression;

    @Column(name = "patient_group", nullable = false, length = 50)
    private String patientGroup;

    @Column(name = "warning_signs", nullable = false, columnDefinition = "TEXT")
    private String warningSigns;

    @Column(name = "severity_level", nullable = false, length = 20)
    private String severityLevel;

    @Column(nullable = false)
    private boolean emergency;

    @Column(name = "matched_groups", nullable = false, columnDefinition = "TEXT")
    private String matchedGroups;

    @Column(name = "reason_codes", nullable = false, columnDefinition = "TEXT")
    private String reasonCodes;

    @Column(name = "requires_clinician_review", nullable = false)
    private boolean requiresClinicianReview;

    @Column(name = "assessed_at", nullable = false, updatable = false)
    private Instant assessedAt = Instant.now();
}
