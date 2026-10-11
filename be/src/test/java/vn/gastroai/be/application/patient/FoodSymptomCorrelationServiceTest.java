package vn.gastroai.be.application.patient;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.patient.FoodCorrelationItem;
import vn.gastroai.be.api.patient.FoodSensitivityProfileResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoodSymptomCorrelationServiceTest {

    private final FoodDiaryEntryRepository foodDiaryEntryRepository = mock(FoodDiaryEntryRepository.class);
    private final BristolLogRepository bristolLogRepository = mock(BristolLogRepository.class);
    private final FoodSymptomCorrelationService service =
            new FoodSymptomCorrelationService(foodDiaryEntryRepository, bristolLogRepository);

    private final Patient patient = new Patient();
    private final Instant now = Instant.now();

    @Test
    void returnsNotEnoughDataWhenFewerThanFifteenMealsLogged() {
        List<FoodDiaryEntry> meals = List.of(
                meal("Com ga", now, null),
                meal("Pho bo", now, null));
        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(anyLong(), any(), any()))
                .thenReturn(meals);
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(anyLong(), any(), any()))
                .thenReturn(List.of());

        FoodSensitivityProfileResponse result = service.analyze(1L, 90);

        assertFalse(result.hasEnoughData());
        assertEquals(2, result.totalMealsAnalyzed());
        assertTrue(result.topCorrelations().isEmpty());
    }

    @Test
    void ranksKeywordsByCorrelationAndExcludesRareOnes() {
        List<FoodDiaryEntry> meals = new ArrayList<>();
        List<BristolLog> bristolLogs = new ArrayList<>();

        // "sua" (milk): 4 bua, 3 co trieu chung -> 75%.
        Instant milkMeal1 = now.minus(10, ChronoUnit.DAYS);
        meals.add(meal("Sua chua", milkMeal1, "dau bung")); // tu bao cao trieu chung
        Instant milkMeal2 = now.minus(9, ChronoUnit.DAYS);
        meals.add(meal("Sua tuoi", milkMeal2, null)); // trieu chung qua Bristol bat thuong
        bristolLogs.add(bristolLog(milkMeal2.plus(10, ChronoUnit.HOURS), 7));
        meals.add(meal("Sua dau nanh", now.minus(8, ChronoUnit.DAYS), "day hoi")); // tu bao cao
        meals.add(meal("Sua chua", now.minus(7, ChronoUnit.DAYS), null)); // khong trieu chung

        // "com"/"ga" (rice/chicken): 10 bua, 2 co trieu chung -> 20%.
        for (int i = 0; i < 10; i++) {
            String symptoms = i < 2 ? "kho tieu" : null;
            Instant eatenAt = now.minus(6, ChronoUnit.DAYS).minus(i * 3L, ChronoUnit.HOURS);
            meals.add(meal("Com ga", eatenAt, symptoms));
        }

        // "trung" (egg): chi 2 bua, duoi nguong MIN_OCCURRENCES=3 -> phai bi loai khoi ket qua.
        meals.add(meal("Trung chien", now.minus(2, ChronoUnit.DAYS), null));
        meals.add(meal("Trung rang", now.minus(1, ChronoUnit.DAYS), null));

        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(anyLong(), any(), any()))
                .thenReturn(meals);
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(anyLong(), any(), any()))
                .thenReturn(bristolLogs);

        FoodSensitivityProfileResponse result = service.analyze(1L, 90);

        assertTrue(result.hasEnoughData());
        assertEquals(16, result.totalMealsAnalyzed());
        // 3 (sua) + 2 (com ga) = 5 trong 16 bua co trieu chung.
        assertEquals(31.25, result.baselineSymptomRatePercent(), 0.01);

        assertTrue(result.topCorrelations().stream().noneMatch(i -> i.foodKeyword().equals("trung")),
                "trung chi xuat hien 2 lan, duoi MIN_OCCURRENCES nen khong duoc vao ket qua");

        FoodCorrelationItem top = result.topCorrelations().get(0);
        assertEquals("sua", top.foodKeyword());
        assertEquals(4, top.timesEaten());
        assertEquals(3, top.timesWithSymptom());
        assertEquals(75.0, top.correlationPercent(), 0.01);

        Optional<FoodCorrelationItem> com = result.topCorrelations().stream()
                .filter(i -> i.foodKeyword().equals("com")).findFirst();
        assertTrue(com.isPresent());
        assertEquals(10, com.get().timesEaten());
        assertEquals(2, com.get().timesWithSymptom());
        assertEquals(20.0, com.get().correlationPercent(), 0.01);
    }

    @Test
    void normalBristolTypeDoesNotCountAsSymptomatic() {
        List<FoodDiaryEntry> meals = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            meals.add(meal("Chao dau xanh", now.minus(i, ChronoUnit.DAYS), null));
        }
        Instant afterFirstMeal = meals.get(0).getEatenAt().plus(5, ChronoUnit.HOURS);
        // Loai 4 la "ly tuong" theo thang Bristol - KHONG duoc tinh la trieu chung.
        List<BristolLog> bristolLogs = List.of(bristolLog(afterFirstMeal, 4));

        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(anyLong(), any(), any()))
                .thenReturn(meals);
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(anyLong(), any(), any()))
                .thenReturn(bristolLogs);

        FoodSensitivityProfileResponse result = service.analyze(1L, 90);

        assertEquals(0.0, result.baselineSymptomRatePercent(), 0.01);
    }

    @Test
    void bristolLogMoreThanFortyEightHoursAfterMealIsNotCountedAsSymptomatic() {
        List<FoodDiaryEntry> meals = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            meals.add(meal("Chao dau xanh", now.minus(20 + i, ChronoUnit.DAYS), null));
        }
        Instant tooLate = meals.get(0).getEatenAt().plus(72, ChronoUnit.HOURS);
        List<BristolLog> bristolLogs = List.of(bristolLog(tooLate, 7));

        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(anyLong(), any(), any()))
                .thenReturn(meals);
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(anyLong(), any(), any()))
                .thenReturn(bristolLogs);

        FoodSensitivityProfileResponse result = service.analyze(1L, 90);

        assertEquals(0.0, result.baselineSymptomRatePercent(), 0.01);
    }

    private FoodDiaryEntry meal(String description, Instant eatenAt, String symptomsAfterMeal) {
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, eatenAt, description, null);
        entry.setSymptomsAfterMeal(symptomsAfterMeal);
        return entry;
    }

    private BristolLog bristolLog(Instant loggedAt, int bristolType) {
        return new BristolLog(patient, loggedAt, bristolType, null);
    }
}
