package vn.gastroai.be.application.cms.dashboard;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.dashboard.*;
import vn.gastroai.be.domain.triage.TriageAlert;
import vn.gastroai.be.domain.triage.TriageAlertStatus;
import vn.gastroai.be.infrastructure.persistence.postgres.*;
import vn.gastroai.be.infrastructure.persistence.postgres.projection.DailyChatStatProjection;
import vn.gastroai.be.infrastructure.persistence.postgres.projection.SymptomStatProjection;
import vn.gastroai.be.infrastructure.persistence.postgres.projection.TriageStatusStatProjection;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardAdminService {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 365;
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final PatientRepository patientRepository;
    private final TriageAlertRepository triageAlertRepository;
    private final SymptomAssessmentRecordRepository symptomAssessmentRecordRepository;
    private final MessageRatingRepository messageRatingRepository;
    private final ObjectMapper objectMapper;

    public DashboardAdminService(
            ChatMessageRepository chatMessageRepository,
            ChatSessionRepository chatSessionRepository,
            PatientRepository patientRepository,
            TriageAlertRepository triageAlertRepository,
            SymptomAssessmentRecordRepository symptomAssessmentRecordRepository,
            MessageRatingRepository messageRatingRepository,
            ObjectMapper objectMapper) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.patientRepository = patientRepository;
        this.triageAlertRepository = triageAlertRepository;
        this.symptomAssessmentRecordRepository = symptomAssessmentRecordRepository;
        this.messageRatingRepository = messageRatingRepository;
        this.objectMapper = objectMapper;
    }

    public DashboardStatsResponse getStats(int days) {
        if (days < MIN_DAYS || days > MAX_DAYS) {
            throw new IllegalArgumentException(
                    "Số ngày thống kê phải nằm trong khoảng " + MIN_DAYS + ".." + MAX_DAYS);
        }

        LocalDate today = LocalDate.now(VIETNAM_ZONE);
        LocalDate startLocalDate = today.minusDays(days - 1L);
        Instant startDate = startLocalDate.atStartOfDay(VIETNAM_ZONE).toInstant();
        Instant endDate = today.plusDays(1).atStartOfDay(VIETNAM_ZONE).toInstant();

        Instant startOfToday = today.atStartOfDay(VIETNAM_ZONE).toInstant();

        // 1. Summary - loc theo dung khoang ngay [startDate, endDate) da chon tren dashboard
        long totalPatients = patientRepository.countActivePatientsBetween(startDate, endDate);
        long totalSessions = chatSessionRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startDate, endDate);
        long totalMessages = chatMessageRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startDate, endDate);
        long todayMessages = chatMessageRepository.countByCreatedAtGreaterThanEqual(startOfToday);

        long totalAlerts = triageAlertRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(startDate, endDate);
        // SCR-AD-13: giu ten truong cu cho FE/xuat file, doi nghia theo 3 trang thai moi -
        // newAlerts = ca can can thiep (viec Admin phai lam), inProgressAlerts = he thong dang tu xu ly.
        long newAlerts = triageAlertRepository.countByStatusAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                TriageAlertStatus.NEEDS_INTERVENTION, startDate, endDate);
        long inProgressAlerts = triageAlertRepository.countByStatusAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                TriageAlertStatus.AUTO_HANDLING, startDate, endDate);
        long resolvedAlerts = triageAlertRepository.countByStatusAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                TriageAlertStatus.RESOLVED, startDate, endDate);

        long totalAssessments = symptomAssessmentRecordRepository.countByAssessedAtGreaterThanEqualAndAssessedAtLessThan(
                startDate, endDate);
        long emergencyAssessments = symptomAssessmentRecordRepository.countByEmergencyTrueAndAssessedAtGreaterThanEqualAndAssessedAtLessThan(
                startDate, endDate);

        long helpfulRatings = messageRatingRepository.countByRatingAndRatedAtGreaterThanEqualAndRatedAtLessThan(
                "HELPFUL", startDate, endDate);
        long unhelpfulRatings = messageRatingRepository.countByRatingAndRatedAtGreaterThanEqualAndRatedAtLessThan(
                "UNHELPFUL", startDate, endDate);
        double satisfactionRate = (helpfulRatings + unhelpfulRatings > 0)
                ? Math.round(((double) helpfulRatings / (helpfulRatings + unhelpfulRatings) * 100.0) * 10.0) / 10.0
                : 0.0;

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                totalPatients,
                totalSessions,
                totalMessages,
                todayMessages,
                totalAlerts,
                newAlerts,
                inProgressAlerts,
                resolvedAlerts,
                totalAssessments,
                emergencyAssessments,
                helpfulRatings,
                unhelpfulRatings,
                satisfactionRate
        );

        // 2. Daily chats (fill missing days with 0)
        List<DailyChatStatProjection> projections = chatMessageRepository.findDailyChatStats(startDate, endDate);
        Map<String, DailyChatStatProjection> mapByDay = new HashMap<>();
        for (DailyChatStatProjection p : projections) {
            mapByDay.put(p.getDay(), p);
        }

        List<DailyChatMetric> dailyChats = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            String dayStr = startLocalDate.plusDays(i).format(DATE_FORMATTER);
            DailyChatStatProjection proj = mapByDay.get(dayStr);
            if (proj != null) {
                dailyChats.add(new DailyChatMetric(
                        dayStr,
                        proj.getTotalMessages(),
                        proj.getEmergencyMessages(),
                        proj.getPatientMessages(),
                        proj.getBotMessages()
                ));
            } else {
                dailyChats.add(new DailyChatMetric(dayStr, 0L, 0L, 0L, 0L));
            }
        }

        // 3. Top symptoms
        List<SymptomStatProjection> symptomProjections = symptomAssessmentRecordRepository.findSymptomStats(startDate, endDate);
        long totalSymptomAssessments = symptomProjections.stream().mapToLong(SymptomStatProjection::getTotalCount).sum();
        List<SymptomMetric> topSymptoms = new ArrayList<>();
        for (SymptomStatProjection sp : symptomProjections) {
            double pct = totalSymptomAssessments > 0
                    ? Math.round(((double) sp.getTotalCount() / totalSymptomAssessments * 100.0) * 10.0) / 10.0
                    : 0.0;
            topSymptoms.add(new SymptomMetric(
                    sp.getSymptom(),
                    mapSymptomName(sp.getSymptom()),
                    sp.getTotalCount(),
                    sp.getEmergencyCount(),
                    pct
            ));
        }

        // 4. Triage groups (parsed from alerts in date range [startDate, endDate))
        List<TriageAlert> alertsInRange = triageAlertRepository
                .findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(startDate, endDate);
        Map<String, Long> groupCounts = new HashMap<>();
        for (TriageAlert alert : alertsInRange) {
            List<String> groups = parseJsonList(alert.getMatchedGroups());
            for (String g : groups) {
                if (g != null && !g.isBlank()) {
                    groupCounts.put(g, groupCounts.getOrDefault(g, 0L) + 1L);
                }
            }
        }
        long totalGroupOccurrences = groupCounts.values().stream().mapToLong(Long::longValue).sum();
        List<TriageGroupMetric> triageGroups = new ArrayList<>();
        groupCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    double pct = totalGroupOccurrences > 0
                            ? Math.round(((double) entry.getValue() / totalGroupOccurrences * 100.0) * 10.0) / 10.0
                            : 0.0;
                    triageGroups.add(new TriageGroupMetric(
                            entry.getKey(),
                            mapTriageGroupName(entry.getKey()),
                            entry.getValue(),
                            pct
                    ));
                });

        // 5. Triage statuses
        List<TriageStatusStatProjection> statusProjections = triageAlertRepository.findStatusStats(startDate, endDate);
        long totalStatusAlerts = statusProjections.stream().mapToLong(TriageStatusStatProjection::getCount).sum();
        List<TriageStatusMetric> triageStatuses = new ArrayList<>();
        for (TriageStatusStatProjection st : statusProjections) {
            double pct = totalStatusAlerts > 0
                    ? Math.round(((double) st.getCount() / totalStatusAlerts * 100.0) * 10.0) / 10.0
                    : 0.0;
            triageStatuses.add(new TriageStatusMetric(
                    st.getStatus(),
                    mapStatusLabel(st.getStatus()),
                    st.getCount(),
                    pct
            ));
        }

        return new DashboardStatsResponse(
                summary,
                dailyChats,
                topSymptoms,
                triageGroups,
                triageStatuses,
                days,
                startLocalDate.format(DATE_FORMATTER),
                today.format(DATE_FORMATTER)
        );
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String mapSymptomName(String code) {
        if (code == null) return "Không xác định";
        return switch (code.toUpperCase()) {
            case "HEARTBURN" -> "Ợ nóng / Trào ngược dạ dày";
            case "BLOATING" -> "Đầy hơi / Chướng bụng";
            case "CONSTIPATION" -> "Táo bón";
            case "DIARRHEA" -> "Tiêu chảy";
            case "ABDOMINAL_PAIN" -> "Đau bụng";
            case "NAUSEA" -> "Buồn nôn / Nôn";
            case "VOMITING" -> "Nôn";
            case "INDIGESTION" -> "Khó tiêu chức năng";
            case "OTHER" -> "Khác";
            default -> code;
        };
    }

    private String mapTriageGroupName(String code) {
        if (code == null) return "Không xác định";
        return switch (code.toUpperCase()) {
            case "XUAT_HUYET_TIEU_HOA" -> "Xuất huyết tiêu hóa";
            case "DAU_BUNG_CAP_TINH" -> "Đau bụng cấp tính";
            case "TAC_RUOT" -> "Tắc ruột";
            case "KHONG_THE_DI_TIEU" -> "Không thể đi tiểu";
            case "SOC_MAT_MAU" -> "Sốc mất máu / Mất nước nặng";
            case "NHIEM_TRUNG_NANG", "SOT_CAO_KEM_DAU_BUNG" -> "Nhiễm trùng nặng / Sốt cao kèm đau bụng";
            case "DAU_LAN_CO_QUAN_KHAC" -> "Đau lan ra cơ quan khác";
            case "KEM_HO_HAP_TIM_MACH", "KHO_THO_DAU_NGUC" -> "Kèm triệu chứng hô hấp/tim mạch";
            case "TIEU_DUONG_KEM_NON" -> "Tiểu đường kèm nôn nhiều";
            case "VANG_DA_KEM_DAU_BUNG" -> "Vàng da kèm đau bụng";
            case "MAT_NUOC_NANG" -> "Mất nước nặng do tiêu chảy kéo dài";
            case "SUT_CAN_KHONG_RO_NGUYEN_NHAN", "SUT_CAN_NHANH" -> "Sụt cân không rõ nguyên nhân";
            case "KHO_NUOT_DAU_KHI_NUOT" -> "Khó nuốt / Đau khi nuốt";
            default -> code;
        };
    }

    private String mapStatusLabel(String status) {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase()) {
            case "AUTO_HANDLING" -> "Hệ thống đang tự xử lý";
            case "NEEDS_INTERVENTION" -> "Cần can thiệp";
            case "RESOLVED" -> "Đã xử lý";
            default -> status;
        };
    }
}
