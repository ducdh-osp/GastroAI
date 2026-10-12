package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0025 - streak va ty le tuan thu ghi nhat ky.
 * currentStreak/longestStreak tinh tren toan bo 365 ngay gan nhat; totalDays/loggedDays/
 * adherencePercent/days chi tinh tren cua so `windowDays` nguoi dung chon. */
public record AdherenceResponse(
        int windowDays,
        int currentStreak,
        int longestStreak,
        int loggedDays,
        int totalDays,
        double adherencePercent,
        boolean loggedToday,
        List<AdherenceDayPoint> days
) {
}
