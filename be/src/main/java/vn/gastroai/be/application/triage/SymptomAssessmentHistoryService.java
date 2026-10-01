package vn.gastroai.be.application.triage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.patient.SymptomAssessmentHistoryResponse;
import vn.gastroai.be.api.patient.SymptomAssessmentResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.triage.StructuredTriageResult;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput;
import vn.gastroai.be.domain.triage.SymptomAssessmentRecord;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.SymptomAssessmentRecordRepository;

import java.util.List;

@Service
public class SymptomAssessmentHistoryService {
    private final SymptomAssessmentRecordRepository recordRepository;
    private final PatientRepository patientRepository;
    private final TriageService triageService;
    private final ObjectMapper objectMapper;

    public SymptomAssessmentHistoryService(SymptomAssessmentRecordRepository recordRepository,
            PatientRepository patientRepository, TriageService triageService, ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.patientRepository = patientRepository;
        this.triageService = triageService;
        this.objectMapper = objectMapper;
    }

    @Transactional("postgresTransactionManager")
    public SymptomAssessmentResponse assessAndSave(Long patientId, SymptomAssessmentInput input) {
        StructuredTriageResult result = triageService.assess(input);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay benh nhan"));

        SymptomAssessmentRecord record = new SymptomAssessmentRecord();
        record.setPatient(patient);
        record.setPrimarySymptom(input.primarySymptom().name());
        record.setPrimarySymptomDetail(input.primarySymptomDetail());
        record.setDuration(input.duration().name());
        record.setReportedSeverity(input.reportedSeverity().name());
        record.setActivityImpact(input.activityImpact().name());
        record.setProgression(input.progression().name());
        record.setPatientGroup(input.patientGroup().name());
        record.setWarningSigns(writeJson(input.warningSigns() == null ? List.of()
                : input.warningSigns().stream().filter(java.util.Objects::nonNull).map(Enum::name).toList()));
        record.setSeverityLevel(result.severityLevel().name());
        record.setEmergency(result.emergency());
        record.setMatchedGroups(writeJson(result.matchedGroups()));
        record.setReasonCodes(writeJson(result.reasonCodes()));
        record.setRequiresClinicianReview(result.requiresClinicianReview());

        SymptomAssessmentRecord saved = recordRepository.save(record);
        return new SymptomAssessmentResponse(saved.getId(), saved.getAssessedAt(), result.severityLevel(),
                result.emergency(), result.matchedGroups(), result.reasonCodes(), result.requiresClinicianReview());
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<SymptomAssessmentHistoryResponse> listForPatient(Long patientId) {
        return recordRepository.findTop100ByPatientIdOrderByAssessedAtDesc(patientId).stream()
                .map(record -> new SymptomAssessmentHistoryResponse(record.getId(), record.getPrimarySymptom(),
                        record.getPrimarySymptomDetail(), record.getDuration(), record.getReportedSeverity(),
                        record.getActivityImpact(), record.getProgression(), record.getPatientGroup(),
                        readJson(record.getWarningSigns()), record.getSeverityLevel(), record.isEmergency(),
                        readJson(record.getMatchedGroups()), readJson(record.getReasonCodes()),
                        record.isRequiresClinicianReview(), record.getAssessedAt()))
                .toList();
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Khong the luu du lieu danh gia trieu chung", e);
        }
    }

    private List<String> readJson(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Du lieu danh gia trieu chung khong hop le", e);
        }
    }
}
