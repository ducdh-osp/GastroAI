package vn.gastroai.be.application.triage;

import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.triage.TriageResult;
import vn.gastroai.be.domain.triage.StructuredTriageResult;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.ActivityImpact;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.PatientGroup;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.Progression;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.SeverityLevel;
import vn.gastroai.be.domain.triage.SymptomAssessmentInput.WarningSign;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * UC0034 - sàng lọc an toàn tầng 1: đối sánh từ khóa dấu hiệu cấp cứu trên câu hỏi/tin nhắn
 * của người dùng, ưu tiên recall cao (thà báo nhầm còn hơn bỏ sót ca thật) nên dùng substring
 * match đơn giản, không dùng NLP/ML - dễ audit, dễ giải thích khi bảo vệ.
 *
 * Chưa nối vào luồng chat thật (UC0017) - đó là bước tích hợp làm sau, khi controller chat đã
 * có. Gọi check() độc lập là dùng/test được ngay.
 */
@Service
public class TriageService {
    private final TriageKeywordSource keywordSource;

    private static final Map<WarningSign, String> WARNING_GROUPS = Map.ofEntries(
            Map.entry(WarningSign.BLOOD_IN_VOMIT, "XUAT_HUYET_TIEU_HOA"),
            Map.entry(WarningSign.BLACK_OR_BLOODY_STOOL, "XUAT_HUYET_TIEU_HOA"),
            Map.entry(WarningSign.SUDDEN_SEVERE_ABDOMINAL_PAIN, "DAU_BUNG_CAP_TINH"),
            Map.entry(WarningSign.RIGID_OR_TENDER_ABDOMEN, "DAU_BUNG_CAP_TINH"),
            Map.entry(WarningSign.UNABLE_TO_PASS_STOOL_OR_GAS, "TAC_RUOT"),
            Map.entry(WarningSign.UNABLE_TO_URINATE, "KHONG_THE_DI_TIEU"),
            Map.entry(WarningSign.BREATHING_DIFFICULTY_OR_CHEST_PAIN, "KEM_HO_HAP_TIM_MACH"),
            Map.entry(WarningSign.FAINTING_OR_CONFUSION, "SOC_MAT_MAU"),
            Map.entry(WarningSign.HIGH_FEVER_WITH_ABDOMINAL_PAIN, "NHIEM_TRUNG_NANG"),
            Map.entry(WarningSign.PAIN_RADIATING_TO_BACK_OR_SHOULDER, "DAU_LAN_CO_QUAN_KHAC"),
            Map.entry(WarningSign.DIABETES_WITH_VOMITING, "TIEU_DUONG_KEM_NON"),
            Map.entry(WarningSign.JAUNDICE_WITH_ABDOMINAL_PAIN, "VANG_DA_KEM_DAU_BUNG"),
            Map.entry(WarningSign.SEVERE_DEHYDRATION, "MAT_NUOC_NANG"));

    private static final Set<WarningSign> EMERGENCY_SIGNS = Set.of(
            WarningSign.BLOOD_IN_VOMIT,
            WarningSign.BLACK_OR_BLOODY_STOOL,
            WarningSign.SUDDEN_SEVERE_ABDOMINAL_PAIN,
            WarningSign.RIGID_OR_TENDER_ABDOMEN,
            WarningSign.UNABLE_TO_PASS_STOOL_OR_GAS,
            WarningSign.UNABLE_TO_URINATE,
            WarningSign.BREATHING_DIFFICULTY_OR_CHEST_PAIN,
            WarningSign.FAINTING_OR_CONFUSION,
            WarningSign.DIABETES_WITH_VOMITING,
            WarningSign.SEVERE_DEHYDRATION);

    public TriageService(TriageKeywordSource keywordSource) {
        this.keywordSource = keywordSource;
    }

    public TriageResult check(String message) {
        String normalized = normalize(message);
        List<String> matchedGroups = new ArrayList<>();

        for (Map.Entry<String, List<String>> group : keywordSource.keywordsByGroup().entrySet()) {
            boolean matched = group.getValue().stream().anyMatch(normalized::contains);
            if (matched) {
                matchedGroups.add(group.getKey());
            }
        }

        return matchedGroups.isEmpty() ? TriageResult.safe() : new TriageResult(true, matchedGroups);
    }

    /**
     * First version of UC0040: combines patient-reported severity and functional impact with
     * the existing high-sensitivity warning-sign policy. Clinical thresholds must be reviewed
     * before this result is used as a clinical decision or recommendation.
     */
    public StructuredTriageResult assess(SymptomAssessmentInput input) {
        Set<WarningSign> warningSigns = new LinkedHashSet<>();
        if (input.warningSigns() != null) {
            input.warningSigns().stream().filter(Objects::nonNull).forEach(warningSigns::add);
        }
        Set<String> matchedGroups = new LinkedHashSet<>();
        for (WarningSign sign : warningSigns) {
            String group = WARNING_GROUPS.get(sign);
            if (group != null) matchedGroups.add(group);
        }

        boolean emergency = warningSigns.stream().anyMatch(EMERGENCY_SIGNS::contains);
        List<String> reasons = new ArrayList<>();
        SeverityLevel severity;
        boolean needsClinicianReview = false;

        if (emergency) {
            severity = SeverityLevel.SEVERE;
            reasons.add("WARNING_SIGNS_PRESENT");
        } else if (input.patientGroup() != PatientGroup.ADULT) {
            severity = SeverityLevel.UNDETERMINED;
            needsClinicianReview = true;
            reasons.add("UNSUPPORTED_PATIENT_GROUP");
            if (!warningSigns.isEmpty()) {
                reasons.add("NON_EMERGENCY_WARNING_SIGNS_PRESENT");
            }
        } else {
            severity = input.reportedSeverity();
            reasons.add("SELF_REPORTED_SEVERITY");

            if (!warningSigns.isEmpty()) {
                needsClinicianReview = true;
                reasons.add("NON_EMERGENCY_WARNING_SIGNS_PRESENT");
                if (severity == SeverityLevel.MILD || severity == SeverityLevel.UNDETERMINED) {
                    severity = SeverityLevel.MODERATE;
                }
            }

            if (severity == SeverityLevel.MILD
                    && (input.activityImpact() != ActivityImpact.NONE
                    || input.progression() == Progression.WORSENING)) {
                severity = SeverityLevel.MODERATE;
                reasons.add("IMPACT_OR_WORSENING_ESCALATION");
            }

            if (severity == SeverityLevel.SEVERE) {
                needsClinicianReview = true;
                reasons.add("SEVERE_SELF_REPORTED_SYMPTOMS");
            }

            if (severity == SeverityLevel.UNDETERMINED) {
                needsClinicianReview = true;
                reasons.add("NEEDS_MORE_INFORMATION");
            }

            if (input.duration() == SymptomAssessmentInput.Duration.UNSURE) {
                needsClinicianReview = true;
                reasons.add("DURATION_UNCLEAR");
            }
        }

        return new StructuredTriageResult(severity, emergency, List.copyOf(matchedGroups),
                List.copyOf(reasons), needsClinicianReview);
    }

    /**
     * Chuẩn hóa về chữ thường, bỏ dấu tiếng Việt (vd "Đau Bụng Dữ Dội" -> "dau bung du doi")
     * để so khớp không phụ thuộc người dùng có gõ dấu hay không. "đ"/"Đ" không tách được dấu
     * qua NFD (là ký tự Unicode riêng, không phải "d" + dấu gạch) nên phải thay thủ công.
     */
    private static String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
