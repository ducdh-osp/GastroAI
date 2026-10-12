package vn.gastroai.be.application.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.patient.FoodCorrelationItem;
import vn.gastroai.be.api.patient.FoodSensitivityProfileResponse;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.application.support.VietnamDateRange;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UC0022 - Đối chiếu nhật ký ăn uống và nhật ký Bristol theo mốc thời gian để gợi ý món ăn có
 * khả năng liên quan tới triệu chứng. Làm bằng rule/thống kê đơn giản (đếm tần suất + tỷ lệ),
 * KHÔNG dùng ML - đúng phạm vi masterplan ghi cho UC này (tham khảo FODMAP Snap/SiboSafe).
 *
 * Đây là gợi ý sơ bộ, không phải chẩn đoán: 1 bữa ăn được coi là "có triệu chứng" nếu HOẶC
 * bệnh nhân tự ghi symptomsAfterMeal cho đúng bữa đó, HOẶC có bản ghi Bristol bất thường
 * (táo bón loại 1-2, tiêu chảy loại 6-7) trong vòng SYMPTOM_WINDOW sau bữa ăn - cách 2 không
 * chắc chắn do đúng bữa đó gây ra (ăn nhiều bữa/ngày), nên kết quả chỉ mang tính gợi ý, có
 * ghi rõ tỷ lệ nền (baseline) để so sánh thay vì khẳng định nhân quả.
 */
@Service
public class FoodSymptomCorrelationService {

    /** Thời gian tiêu hoá thông thường 24-72h - lấy mốc giữa, đủ rộng để bắt được phần lớn
     * phản ứng thật mà không kéo dài tới mức trộn lẫn quá nhiều bữa ăn khác nhau. */
    private static final Duration SYMPTOM_WINDOW = Duration.ofHours(48);

    /** Loại 1-2 (táo bón) và 6-7 (tiêu chảy) theo thang Bristol - xem BRISTOL_TYPE_LABELS ở
     * FE, loại 3-5 coi là trong khoảng bình thường nên không tính là "có triệu chứng". */
    private static final Set<Integer> ABNORMAL_BRISTOL_TYPES = Set.of(1, 2, 6, 7);

    /** Cần ít nhất chừng này bữa ăn đã ghi mới đủ để phân tích có ý nghĩa - dưới mức này trả
     * hasEnoughData=false thay vì đưa ra con số dễ gây hiểu lầm từ quá ít dữ liệu. */
    private static final int MIN_MEALS_FOR_ANALYSIS = 15;

    /** 1 từ khoá phải xuất hiện trong ít nhất chừng này bữa mới được đưa vào kết quả - tránh
     * "ăn đúng 1 lần, đúng lần đó có triệu chứng" bị tính thành tương quan 100%. */
    private static final int MIN_OCCURRENCES = 3;

    private static final int TOP_N = 10;

    private final FoodDiaryEntryRepository foodDiaryEntryRepository;
    private final BristolLogRepository bristolLogRepository;

    public FoodSymptomCorrelationService(FoodDiaryEntryRepository foodDiaryEntryRepository,
                                          BristolLogRepository bristolLogRepository) {
        this.foodDiaryEntryRepository = foodDiaryEntryRepository;
        this.bristolLogRepository = bristolLogRepository;
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public FoodSensitivityProfileResponse analyze(Long patientId, int days) {
        VietnamDateRange.Range range = VietnamDateRange.recentDaysIncludingToday(days);

        List<FoodDiaryEntry> meals = foodDiaryEntryRepository
                .findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(
                        patientId, range.fromInclusive(), range.toExclusive());

        if (meals.size() < MIN_MEALS_FOR_ANALYSIS) {
            return new FoodSensitivityProfileResponse(false, meals.size(), days, 0, List.of());
        }

        // Lay them Bristol log vuot qua moc ket thuc 1 khoang SYMPTOM_WINDOW de khong bo sot
        // trieu chung cua nhung bua an gan cuoi ky phan tich.
        List<BristolLog> bristolLogs = bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                patientId, range.fromInclusive(), range.toExclusive().plus(SYMPTOM_WINDOW));
        List<Instant> abnormalBristolTimes = bristolLogs.stream()
                .filter(log -> ABNORMAL_BRISTOL_TYPES.contains(log.getBristolType()))
                .map(BristolLog::getLoggedAt)
                .sorted()
                .toList();

        Map<String, long[]> stats = new HashMap<>(); // token -> [timesEaten, timesWithSymptom]
        int symptomMealsCount = 0;

        for (FoodDiaryEntry meal : meals) {
            boolean symptomatic = isSelfReportedSymptom(meal)
                    || hasAbnormalBristolWithin(meal.getEatenAt(), abnormalBristolTimes);
            if (symptomatic) symptomMealsCount++;

            Set<String> tokens = VietnameseText.tokenize(meal.getDescription());
            for (String token : tokens) {
                long[] counters = stats.computeIfAbsent(token, t -> new long[2]);
                counters[0]++;
                if (symptomatic) counters[1]++;
            }
        }

        double baselineRate = 100.0 * symptomMealsCount / meals.size();

        List<FoodCorrelationItem> topCorrelations = stats.entrySet().stream()
                .filter(e -> e.getValue()[0] >= MIN_OCCURRENCES)
                .map(e -> new FoodCorrelationItem(
                        e.getKey(),
                        e.getValue()[0],
                        e.getValue()[1],
                        100.0 * e.getValue()[1] / e.getValue()[0]))
                .sorted((a, b) -> {
                    int byRate = Double.compare(b.correlationPercent(), a.correlationPercent());
                    return byRate != 0 ? byRate : Long.compare(b.timesEaten(), a.timesEaten());
                })
                .limit(TOP_N)
                .toList();

        return new FoodSensitivityProfileResponse(true, meals.size(), days, baselineRate, topCorrelations);
    }

    private boolean isSelfReportedSymptom(FoodDiaryEntry meal) {
        return meal.getSymptomsAfterMeal() != null && !meal.getSymptomsAfterMeal().isBlank();
    }

    private boolean hasAbnormalBristolWithin(Instant mealTime, List<Instant> sortedAbnormalBristolTimes) {
        Instant windowEnd = mealTime.plus(SYMPTOM_WINDOW);
        for (Instant bristolTime : sortedAbnormalBristolTimes) {
            if (bristolTime.isBefore(mealTime)) continue;
            if (bristolTime.isAfter(windowEnd)) break;
            return true;
        }
        return false;
    }
}
