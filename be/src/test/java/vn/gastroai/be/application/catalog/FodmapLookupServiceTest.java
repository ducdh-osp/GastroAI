package vn.gastroai.be.application.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.gastroai.be.domain.catalog.FodmapFood;
import vn.gastroai.be.domain.catalog.FodmapLevel;
import vn.gastroai.be.domain.catalog.FodmapUnknownName;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapFoodRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapUnknownNameRepository;

@ExtendWith(MockitoExtension.class)
class FodmapLookupServiceTest {

    @Mock private FodmapFoodRepository foodRepository;
    @Mock private FodmapUnknownNameRepository unknownNameRepository;

    private FodmapLookupService service;

    private final FodmapFood onion = food(1L, "Hành tây", "hanh tay", "onion", FodmapLevel.HIGH, "FRUCTAN");
    private final FodmapFood beef = food(2L, "Thịt bò", "thit bo", "bò, beef", FodmapLevel.LOW, null);
    private final FodmapFood noodles = food(3L, "Bánh phở", "banh pho", "sợi phở", FodmapLevel.LOW, null);

    @BeforeEach
    void setUp() {
        service = new FodmapLookupService(foodRepository, unknownNameRepository);
    }

    @Test
    void matchesNameIgnoringCaseAndAccents() {
        stubFoods(onion);

        List<FodmapIngredientResult> results = service.lookup(List.of("HÀNH TÂY ", "hanh tay"));

        assertEquals(2, results.size());
        for (FodmapIngredientResult result : results) {
            assertTrue(result.found());
            assertEquals("Hành tây", result.matchedName());
            assertEquals(FodmapLevel.HIGH, result.level());
            assertEquals(List.of("FRUCTAN"), result.groups());
        }
        assertEquals("HÀNH TÂY", results.get(0).inputName());
    }

    @Test
    void matchesAlias() {
        stubFoods(onion);

        FodmapIngredientResult result = service.lookup(List.of("Onion")).get(0);

        assertEquals("Hành tây", result.matchedName());
        assertEquals(1L, result.foodId());
    }

    @Test
    void nameWinsOverAliasOfAnotherFood() {
        FodmapFood orange = food(10L, "Cam", "cam", null, FodmapLevel.LOW, null);
        FodmapFood other = food(11L, "Món khác", "mon khac", "cam", FodmapLevel.HIGH, null);
        stubFoods(other, orange);

        FodmapIngredientResult result = service.lookup(List.of("cam")).get(0);

        assertEquals("Cam", result.matchedName());
    }

    @Test
    void keepsInputOrder() {
        stubFoods(onion, beef, noodles);

        List<FodmapIngredientResult> results = service.lookup(List.of("thịt bò", "bánh phở", "hành tây"));

        assertEquals(List.of("Thịt bò", "Bánh phở", "Hành tây"),
                results.stream().map(FodmapIngredientResult::matchedName).toList());
    }

    @Test
    void unknownNameIsRecordedAsNew() {
        stubFoods(onion);
        when(unknownNameRepository.findByNormalizedName("pho bo")).thenReturn(Optional.empty());

        FodmapIngredientResult result = service.lookup(List.of("Phở bò")).get(0);

        assertFalse(result.found());
        assertEquals(FodmapLevel.UNKNOWN, result.level());
        assertEquals("Chưa có dữ liệu", result.levelLabel());

        ArgumentCaptor<FodmapUnknownName> captor = ArgumentCaptor.forClass(FodmapUnknownName.class);
        verify(unknownNameRepository).save(captor.capture());
        assertEquals("pho bo", captor.getValue().getNormalizedName());
        assertEquals("Phở bò", captor.getValue().getSampleName());
        assertEquals(1, captor.getValue().getHitCount());
    }

    @Test
    void existingUnknownNameIncreasesHitCount() {
        stubFoods(onion);
        FodmapUnknownName existing = new FodmapUnknownName("que", "Quế");
        when(unknownNameRepository.findByNormalizedName("que")).thenReturn(Optional.of(existing));

        service.lookup(List.of("quế"));

        assertEquals(2, existing.getHitCount());
        verify(unknownNameRepository).save(existing);
    }

    @Test
    void sameUnknownNameInOneCallIsCountedOnce() {
        stubFoods(onion);
        when(unknownNameRepository.findByNormalizedName("que")).thenReturn(Optional.empty());

        service.lookup(List.of("quế", "Quế", "que"));

        verify(unknownNameRepository, times(1)).findByNormalizedName("que");
        verify(unknownNameRepository, times(1)).save(any(FodmapUnknownName.class));
    }

    @Test
    void recordingFailureDoesNotBreakLookup() {
        stubFoods(onion);
        when(unknownNameRepository.findByNormalizedName(anyString()))
                .thenThrow(new RuntimeException("DB lỗi"));

        List<FodmapIngredientResult> results = service.lookup(List.of("quế", "hành tây"));

        assertEquals(2, results.size());
        assertEquals(FodmapLevel.UNKNOWN, results.get(0).level());
        assertEquals(FodmapLevel.HIGH, results.get(1).level());
    }

    @Test
    void nullOrBlankInputReturnsEmpty() {
        assertTrue(service.lookup(null).isEmpty());
        assertTrue(service.lookup(List.of()).isEmpty());
        assertTrue(service.lookup(Arrays.asList(null, "", "   ")).isEmpty());

        verifyNoInteractions(foodRepository, unknownNameRepository);
    }

    @Test
    void limitsToMaxNames() {
        stubFoods(beef);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < FodmapLookupService.MAX_NAMES + 5; i++) {
            names.add("thịt bò");
        }

        List<FodmapIngredientResult> results = service.lookup(names);

        assertEquals(FodmapLookupService.MAX_NAMES, results.size());
        verify(unknownNameRepository, never()).save(any());
    }

    @Test
    void dishIsHighWithTriggerAndIncomplete() {
        stubFoods(onion, beef, noodles);
        when(unknownNameRepository.findByNormalizedName("que")).thenReturn(Optional.empty());

        FodmapDishResult dish = service.lookupDish("Phở bò",
                List.of("bánh phở", "thịt bò", "hành tây", "onion", "quế"));

        assertEquals("Phở bò", dish.dishName());
        assertEquals(FodmapLevel.HIGH, dish.overallLevel());
        assertEquals("Cao", dish.overallLabel());
        assertEquals(List.of("Hành tây"), dish.triggers());
        assertFalse(dish.complete());
        assertEquals(5, dish.ingredients().size());
        assertEquals(FodmapDishResult.DISCLAIMER, dish.disclaimer());
    }

    @Test
    void dishAllLowHasNoTriggersAndIsComplete() {
        stubFoods(onion, beef, noodles);

        FodmapDishResult dish = service.lookupDish("Phở bò không hành", List.of("bánh phở", "thịt bò"));

        assertEquals(FodmapLevel.LOW, dish.overallLevel());
        assertTrue(dish.triggers().isEmpty());
        assertTrue(dish.complete());
    }

    @Test
    void dishAllUnknownIsUnknown() {
        stubFoods(onion);
        when(unknownNameRepository.findByNormalizedName(anyString())).thenReturn(Optional.empty());

        FodmapDishResult dish = service.lookupDish("Món lạ", List.of("quế", "hồi"));

        assertEquals(FodmapLevel.UNKNOWN, dish.overallLevel());
        assertTrue(dish.triggers().isEmpty());
        assertFalse(dish.complete());
    }

    private void stubFoods(FodmapFood... foods) {
        when(foodRepository.findByActiveTrue()).thenReturn(List.of(foods));
    }

    private static FodmapFood food(Long id, String name, String normalizedName, String aliases,
                                   FodmapLevel level, String groups) {
        FodmapFood food = new FodmapFood();
        food.setId(id);
        food.setName(name);
        food.setNormalizedName(normalizedName);
        food.setAliases(aliases);
        food.setLevel(level);
        food.setGroups(groups);
        return food;
    }
}