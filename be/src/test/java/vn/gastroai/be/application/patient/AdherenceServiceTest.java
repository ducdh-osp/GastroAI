package vn.gastroai.be.application.patient;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.AdherenceResponse;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdherenceServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    void noData_everythingZero() {
        AdherenceResponse r = AdherenceService.build(30, TODAY, Map.of(), Map.of());
        assertEquals(0, r.currentStreak());
        assertEquals(0, r.longestStreak());
        assertEquals(0, r.loggedDays());
        assertEquals(0.0, r.adherencePercent());
        assertEquals(30, r.days().size());
        assertFalse(r.loggedToday());
    }

    @Test
    void streakIncludesToday_andCombinesBothLogTypes() {
        Map<LocalDate, Long> food = new HashMap<>();
        Map<LocalDate, Long> bristol = new HashMap<>();
        food.put(TODAY, 2L);
        bristol.put(TODAY.minusDays(1), 1L);
        food.put(TODAY.minusDays(2), 1L);
        // TODAY-3 bi bo trong -> streak dut
        food.put(TODAY.minusDays(4), 1L);

        AdherenceResponse r = AdherenceService.build(7, TODAY, food, bristol);
        assertEquals(3, r.currentStreak());
        assertEquals(3, r.longestStreak());
        assertEquals(4, r.loggedDays());
        assertEquals(57.1, r.adherencePercent());
        assertTrue(r.loggedToday());
    }

    @Test
    void todayNotYetLogged_doesNotBreakStreak() {
        Map<LocalDate, Long> food = new HashMap<>();
        food.put(TODAY.minusDays(1), 1L);
        food.put(TODAY.minusDays(2), 1L);

        AdherenceResponse r = AdherenceService.build(30, TODAY, food, Map.of());
        assertEquals(2, r.currentStreak());
        assertFalse(r.loggedToday());
    }

    @Test
    void missedYesterday_resetsCurrentStreak_butKeepsLongest() {
        Map<LocalDate, Long> food = new HashMap<>();
        for (int i = 5; i <= 9; i++) {
            food.put(TODAY.minusDays(i), 1L);
        }

        AdherenceResponse r = AdherenceService.build(30, TODAY, food, Map.of());
        assertEquals(0, r.currentStreak());
        assertEquals(5, r.longestStreak());
    }

    @Test
    void streakCanExceedWindow() {
        Map<LocalDate, Long> food = new HashMap<>();
        for (int i = 0; i < 40; i++) {
            food.put(TODAY.minusDays(i), 1L);
        }

        AdherenceResponse r = AdherenceService.build(7, TODAY, food, Map.of());
        assertEquals(40, r.currentStreak());
        assertEquals(7, r.loggedDays());
        assertEquals(100.0, r.adherencePercent());
    }
}
