package vn.gastroai.be.application.triage;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.triage.StructuredTriageResult;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.ActivityImpact;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.Duration;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.PatientGroup;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.PrimarySymptom;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.Progression;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.SeverityLevel;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.WarningSign;

import java.util.Set;
import vn.gastroai.be.domain.triage.TriageResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test thuần logic (không @SpringBootTest, không cần GEMINI_API_KEY/DB) vì TriageService chỉ
 * so khớp string, không gọi API/DB nào.
 */
class TriageServiceTest {

    private final TriageService triageService = new TriageService(new HardcodedTriageKeywordSource());

    @Test
    void matchesEmergencyKeywordRegardlessOfDiacritics() {
        TriageResult result = triageService.check("Tôi bị đau bụng dữ dội quá, không đứng thẳng được");
        assertTrue(result.emergency());
        assertTrue(result.matchedGroups().contains("DAU_BUNG_CAP_TINH"));
    }

    @Test
    void matchesEvenWithoutVietnameseDiacritics() {
        TriageResult result = triageService.check("di ngoai ra mau tu sang gio");
        assertTrue(result.emergency());
        assertTrue(result.matchedGroups().contains("XUAT_HUYET_TIEU_HOA"));
    }

    @Test
    void matchesMultipleGroupsWhenBothPresent() {
        TriageResult result = triageService.check("Đau bụng dữ dội kèm khó thở, người nhà đang chóng mặt");
        assertTrue(result.emergency());
        assertTrue(result.matchedGroups().contains("DAU_BUNG_CAP_TINH"));
        assertTrue(result.matchedGroups().contains("KEM_HO_HAP_TIM_MACH"));
    }

    @Test
    void doesNotFlagOrdinaryQuestion() {
        TriageResult result = triageService.check("Ăn nhiều rau có tốt cho tiêu hóa không?");
        assertFalse(result.emergency());
        assertEquals(0, result.matchedGroups().size());
    }

    @Test
    void alarmFeatureGroupMatchesButDoesNotFlagEmergency() {
        TriageResult result = triageService.check("Gần đây tôi tự nhiên sụt cân không rõ nguyên nhân");
        assertFalse(result.emergency());
        assertTrue(result.matchedGroups().contains("SUT_CAN_KHONG_RO_NGUYEN_NHAN"));
    }

    @Test
    void difficultSwallowingAlarmGroupDoesNotFlagEmergencyEither() {
        TriageResult result = triageService.check("Tôi bị khó nuốt mấy ngày nay");
        assertFalse(result.emergency());
        assertTrue(result.matchedGroups().contains("KHO_NUOT_DAU_KHI_NUOT"));
    }

    @Test
    void emergencyGroupStillWinsWhenMixedWithAlarmFeatureGroup() {
        TriageResult result = triageService.check(
                "Tôi khó nuốt vài ngày nay và hôm nay thì đau bụng dữ dội dột ngột");
        assertTrue(result.emergency());
        assertTrue(result.matchedGroups().contains("KHO_NUOT_DAU_KHI_NUOT"));
        assertTrue(result.matchedGroups().contains("DAU_BUNG_CAP_TINH"));
    }

    @Test
    void emergencyWarningOverridesEveryPatientGroupAndReportedSeverity() {
        for (PatientGroup group : PatientGroup.values()) {
            SymptomAssessmentInput input = input(SeverityLevel.MILD, ActivityImpact.NONE,
                    Progression.STABLE, Duration.ONE_TO_THREE_DAYS, group,
                    Set.of(WarningSign.BLOOD_IN_VOMIT));

            StructuredTriageResult result = triageService.assess(input);

            assertTrue(result.emergency());
            assertEquals(SeverityLevel.SEVERE, result.severityLevel());
            assertTrue(result.reasonCodes().contains("WARNING_SIGNS_PRESENT"));
        }
    }

    @Test
    void nonAdultAndUnsureGroupsRemainUndeterminedForNonEmergencyInput() {
        for (PatientGroup group : Set.of(PatientGroup.UNDER_18,
                PatientGroup.PREGNANT_OR_RECENTLY_POSTPARTUM, PatientGroup.UNSURE)) {
            StructuredTriageResult result = triageService.assess(input(SeverityLevel.SEVERE,
                    ActivityImpact.PREVENTS_NORMAL_ACTIVITY, Progression.WORSENING,
                    Duration.ONE_TO_THREE_DAYS, group, Set.of()));

            assertEquals(SeverityLevel.UNDETERMINED, result.severityLevel());
            assertTrue(result.requiresClinicianReview());
            assertTrue(result.reasonCodes().contains("UNSUPPORTED_PATIENT_GROUP"));
        }
    }

    @Test
    void mildWithWorseningProgressionEscalatesToModerate() {
        StructuredTriageResult result = triageService.assess(input(SeverityLevel.MILD,
                ActivityImpact.NONE, Progression.WORSENING, Duration.ONE_TO_THREE_DAYS,
                PatientGroup.ADULT, Set.of()));

        assertEquals(SeverityLevel.MODERATE, result.severityLevel());
        assertTrue(result.reasonCodes().contains("IMPACT_OR_WORSENING_ESCALATION"));
    }

    @Test
    void mildWithIndependentActivityLimitationEscalatesToModerate() {
        StructuredTriageResult result = triageService.assess(input(SeverityLevel.MILD,
                ActivityImpact.SOME_LIMITATION, Progression.STABLE, Duration.ONE_TO_THREE_DAYS,
                PatientGroup.ADULT, Set.of()));

        assertEquals(SeverityLevel.MODERATE, result.severityLevel());
        assertTrue(result.reasonCodes().contains("IMPACT_OR_WORSENING_ESCALATION"));
    }

    @Test
    void nonEmergencyWarningSignRequiresClinicianReviewAndEscalatesMild() {
        StructuredTriageResult result = triageService.assess(input(SeverityLevel.MILD,
                ActivityImpact.NONE, Progression.STABLE, Duration.ONE_TO_THREE_DAYS,
                PatientGroup.ADULT, Set.of(WarningSign.HIGH_FEVER_WITH_ABDOMINAL_PAIN)));

        assertFalse(result.emergency());
        assertEquals(SeverityLevel.MODERATE, result.severityLevel());
        assertTrue(result.requiresClinicianReview());
        assertTrue(result.reasonCodes().contains("NON_EMERGENCY_WARNING_SIGNS_PRESENT"));
    }

    @Test
    void unsureDurationRequiresClinicianReview() {
        StructuredTriageResult result = triageService.assess(input(SeverityLevel.MILD,
                ActivityImpact.NONE, Progression.STABLE, Duration.UNSURE,
                PatientGroup.ADULT, Set.of()));

        assertTrue(result.requiresClinicianReview());
        assertTrue(result.reasonCodes().contains("DURATION_UNCLEAR"));
    }

    @Test
    void newlyAddedReviewWarningsDoNotTriggerEmergency() {
        StructuredTriageResult result = triageService.assess(input(SeverityLevel.MILD,
                ActivityImpact.NONE, Progression.STABLE, Duration.ONE_TO_THREE_DAYS,
                PatientGroup.ADULT, Set.of(WarningSign.UNEXPLAINED_WEIGHT_LOSS,
                        WarningSign.DIFFICULT_OR_PAINFUL_SWALLOWING)));

        assertFalse(result.emergency());
        assertTrue(result.requiresClinicianReview());
        assertEquals(SeverityLevel.MODERATE, result.severityLevel());
        assertEquals(2, result.matchedGroups().size());
    }

    private static SymptomAssessmentInput input(SeverityLevel severity, ActivityImpact impact,
            Progression progression, Duration duration, PatientGroup group,
            Set<WarningSign> warningSigns) {
        return new SymptomAssessmentInput(PrimarySymptom.ABDOMINAL_PAIN, null, duration, severity,
                impact, progression, group, warningSigns);
    }
}
