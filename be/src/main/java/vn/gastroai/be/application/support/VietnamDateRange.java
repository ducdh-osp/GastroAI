package vn.gastroai.be.application.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Calendar-day ranges used by patient trends, consistently in Vietnam time. */
public final class VietnamDateRange {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private VietnamDateRange() {
    }

    public static Range recentDaysIncludingToday(int days) {
        LocalDate today = Instant.now().atZone(ZONE).toLocalDate();
        LocalDate start = today.minusDays(days - 1L);
        return new Range(start, today.plusDays(1),
                start.atStartOfDay(ZONE).toInstant(),
                today.plusDays(1).atStartOfDay(ZONE).toInstant());
    }

    public record Range(LocalDate startDate, LocalDate endExclusiveDate,
                        Instant fromInclusive, Instant toExclusive) {
    }
}
