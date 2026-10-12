package vn.gastroai.be.application.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.patient.AdherenceDayPoint;
import vn.gastroai.be.api.patient.AdherenceResponse;
import vn.gastroai.be.application.support.VietnamDateRange;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** UC0025 - tinh streak (so ngay lien tiep co ghi nhat ky) va ty le tuan thu tu nhat ky
 * an uong + nhat ky Bristol. Mot ngay "co ghi" = co it nhat 1 ban ghi cua 1 trong 2 loai,
 * tinh theo ngay Viet Nam. Cac ban ghi da xoa mem (UC0020) bi @SQLRestriction loai san
 * khoi cac truy van derived nen khong duoc tinh. */
@Service
public class AdherenceService {

    /** Cua so tra cuu toi da cho streak - khop gioi han days cua cac API xu huong khac. */
    static final int STREAK_LOOKBACK_DAYS = 365;

    private final FoodDiaryEntryRepository foodDiaryEntryRepository;
    private final BristolLogRepository bristolLogRepository;

    public AdherenceService(FoodDiaryEntryRepository foodDiaryEntryRepository,
                            BristolLogRepository bristolLogRepository) {
        this.foodDiaryEntryRepository = foodDiaryEntryRepository;
        this.bristolLogRepository = bristolLogRepository;
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public AdherenceResponse compute(Long patientId, int windowDays) {
        VietnamDateRange.Range lookback = VietnamDateRange.recentDaysIncludingToday(STREAK_LOOKBACK_DAYS);

        Map<LocalDate, Long> foodByDate = new HashMap<>();
        foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(
                        patientId, lookback.fromInclusive(), lookback.toExclusive())
                .forEach(e -> foodByDate.merge(toVietnamDate(e.getEatenAt()), 1L, Long::sum));

        Map<LocalDate, Long> bristolByDate = new HashMap<>();
        bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                        patientId, lookback.fromInclusive(), lookback.toExclusive())
                .forEach(l -> bristolByDate.merge(toVietnamDate(l.getLoggedAt()), 1L, Long::sum));

        LocalDate today = lookback.endExclusiveDate().minusDays(1);
        return build(windowDays, today, foodByDate, bristolByDate);
    }

    /** Tach rieng, thuan tuy (khong DB, khong dong ho he thong) de test don vi duoc. */
    static AdherenceResponse build(int windowDays, LocalDate today,
                                   Map<LocalDate, Long> foodByDate, Map<LocalDate, Long> bristolByDate) {
        LocalDate lookbackStart = today.minusDays(STREAK_LOOKBACK_DAYS - 1L);

        // Streak hien tai: dem lui tu hom nay. Neu hom nay chua ghi thi chua tinh la dut -
        // ngay con dang dien ra, nguoi dung van con thoi gian ghi - nen bat dau tu hom qua.
        LocalDate cursor = isLogged(today, foodByDate, bristolByDate) ? today : today.minusDays(1);
        int currentStreak = 0;
        while (!cursor.isBefore(lookbackStart) && isLogged(cursor, foodByDate, bristolByDate)) {
            currentStreak++;
            cursor = cursor.minusDays(1);
        }

        int longestStreak = 0;
        int run = 0;
        for (LocalDate d = lookbackStart; !d.isAfter(today); d = d.plusDays(1)) {
            if (isLogged(d, foodByDate, bristolByDate)) {
                run++;
                longestStreak = Math.max(longestStreak, run);
            } else {
                run = 0;
            }
        }

        LocalDate windowStart = today.minusDays(windowDays - 1L);
        List<AdherenceDayPoint> days = new ArrayList<>();
        int loggedDays = 0;
        for (LocalDate d = windowStart; !d.isAfter(today); d = d.plusDays(1)) {
            long food = foodByDate.getOrDefault(d, 0L);
            long bristol = bristolByDate.getOrDefault(d, 0L);
            boolean logged = food + bristol > 0;
            if (logged) {
                loggedDays++;
            }
            days.add(new AdherenceDayPoint(d, food, bristol, logged));
        }
        double percent = windowDays == 0 ? 0 : Math.round(loggedDays * 1000.0 / windowDays) / 10.0;

        return new AdherenceResponse(windowDays, currentStreak, longestStreak, loggedDays, windowDays,
                percent, isLogged(today, foodByDate, bristolByDate), days);
    }

    private static boolean isLogged(LocalDate d, Map<LocalDate, Long> food, Map<LocalDate, Long> bristol) {
        return food.getOrDefault(d, 0L) + bristol.getOrDefault(d, 0L) > 0;
    }

    private static LocalDate toVietnamDate(Instant instant) {
        return instant.atZone(VietnamDateRange.ZONE).toLocalDate();
    }
}
