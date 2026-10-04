package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.SymptomAssessmentResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput;
import vn.gastroai.be.domain.triage.SymptomAssessmentRecord;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.SymptomAssessmentRecordRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SymptomAssessmentHistoryServiceTest {
    private final SymptomAssessmentRecordRepository recordRepository = mock(SymptomAssessmentRecordRepository.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final TriageAlertService triageAlertService = mock(TriageAlertService.class);
    private final SymptomAssessmentHistoryService service = new SymptomAssessmentHistoryService(
            recordRepository, patientRepository, new TriageService(new HardcodedTriageKeywordSource()),
            triageAlertService, new ObjectMapper());

    @Test
    void assessmentIsSavedWithPatientAndOtherSymptomDetails() {
        Patient patient = new Patient();
        patient.setId(42L);
        when(patientRepository.findById(42L)).thenReturn(Optional.of(patient));
        when(recordRepository.save(any(SymptomAssessmentRecord.class))).thenAnswer(invocation -> {
            SymptomAssessmentRecord record = invocation.getArgument(0);
            record.setId(7L);
            record.setAssessedAt(Instant.parse("2026-10-01T10:00:00Z"));
            return record;
        });

        SymptomAssessmentInput input = new SymptomAssessmentInput(
                SymptomAssessmentInput.PrimarySymptom.OTHER,
                "Đau âm ỉ vùng bụng dưới sau ăn",
                SymptomAssessmentInput.Duration.ONE_TO_THREE_DAYS,
                SymptomAssessmentInput.SeverityLevel.MILD,
                SymptomAssessmentInput.ActivityImpact.SOME_LIMITATION,
                SymptomAssessmentInput.Progression.STABLE,
                SymptomAssessmentInput.PatientGroup.ADULT,
                Set.of(SymptomAssessmentInput.WarningSign.JAUNDICE_WITH_ABDOMINAL_PAIN));

        SymptomAssessmentResponse response = service.assessAndSave(42L, input);

        assertEquals(7L, response.id());
        assertNotNull(response.assessedAt());
        assertTrue(response.requiresClinicianReview());
        verify(recordRepository).save(any(SymptomAssessmentRecord.class));
    }

    @Test
    void historyQueryIsScopedToPatientAndMapsSavedDetail() {
        SymptomAssessmentRecord record = new SymptomAssessmentRecord();
        record.setId(7L);
        record.setPrimarySymptom("OTHER");
        record.setPrimarySymptomDetail("Đau âm ỉ vùng bụng dưới sau ăn");
        record.setDuration("ONE_TO_THREE_DAYS");
        record.setReportedSeverity("MILD");
        record.setActivityImpact("SOME_LIMITATION");
        record.setProgression("STABLE");
        record.setPatientGroup("ADULT");
        record.setWarningSigns("[\"JAUNDICE_WITH_ABDOMINAL_PAIN\"]");
        record.setSeverityLevel("MODERATE");
        record.setEmergency(false);
        record.setMatchedGroups("[\"VANG_DA_KEM_DAU_BUNG\"]");
        record.setReasonCodes("[\"NON_EMERGENCY_WARNING_SIGNS_PRESENT\"]");
        record.setRequiresClinicianReview(true);
        record.setAssessedAt(Instant.parse("2026-10-01T10:00:00Z"));
        when(recordRepository.findTop100ByPatientIdOrderByAssessedAtDesc(42L)).thenReturn(List.of(record));

        var history = service.listForPatient(42L);

        assertEquals(1, history.size());
        assertEquals("Đau âm ỉ vùng bụng dưới sau ăn", history.getFirst().primarySymptomDetail());
        assertEquals(List.of("JAUNDICE_WITH_ABDOMINAL_PAIN"), history.getFirst().warningSigns());
        assertEquals("MODERATE", history.getFirst().severityLevel());
        assertFalse(history.getFirst().emergency());
        verify(recordRepository).findTop100ByPatientIdOrderByAssessedAtDesc(42L);
    }
}
