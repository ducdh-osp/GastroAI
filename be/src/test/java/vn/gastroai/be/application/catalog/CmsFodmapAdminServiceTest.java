package vn.gastroai.be.application.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.gastroai.be.api.cms.catalog.FodmapFoodRequest;
import vn.gastroai.be.api.cms.catalog.FodmapFoodResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.catalog.FodmapFood;
import vn.gastroai.be.domain.catalog.FodmapLevel;
import vn.gastroai.be.domain.catalog.FodmapUnknownName;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapFoodRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapUnknownNameRepository;

@ExtendWith(MockitoExtension.class)
class CmsFodmapAdminServiceTest {

    @Mock private FodmapFoodRepository foodRepository;
    @Mock private FodmapUnknownNameRepository unknownNameRepository;

    private CmsFodmapAdminService service;

    private FodmapFood onion;
    private FodmapFood beef;

    @BeforeEach
    void setUp() {
        service = new CmsFodmapAdminService(foodRepository, unknownNameRepository);
        onion = food(1L, "Hành tây", "hanh tay", "onion", FodmapLevel.HIGH, "FRUCTAN");
        beef = food(2L, "Thịt bò", "thit bo", "bò, beef", FodmapLevel.LOW, null);
    }

    @Test
    void createCleansDataAndRemovesFromUnknown() {
        when(foodRepository.findAll()).thenReturn(List.of(onion, beef));
        when(foodRepository.save(any(FodmapFood.class))).thenAnswer(invocation -> {
            FodmapFood saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        FodmapFoodResponse response = service.create(request(
                "  Rau muống ",
                List.of("water spinach", "Rau Muong", "WATER SPINACH", " "),
                FodmapLevel.LOW,
                List.of("fructan", "FRUCTAN"),
                "   ",
                " Rau xào ",
                " Monash "));

        assertEquals(99L, response.id());
        assertEquals("Rau muống", response.name());
        assertEquals(List.of("water spinach"), response.aliases());
        assertEquals(List.of("FRUCTAN"), response.groups());
        assertNull(response.servingNote());
        assertEquals("Rau xào", response.note());
        assertEquals("Monash", response.source());

        ArgumentCaptor<FodmapFood> captor = ArgumentCaptor.forClass(FodmapFood.class);
        verify(foodRepository).save(captor.capture());
        assertEquals("rau muong", captor.getValue().getNormalizedName());

        verify(unknownNameRepository).deleteByNormalizedName("rau muong");
        verify(unknownNameRepository).deleteByNormalizedName("water spinach");
    }

    @Test
    void rejectsUnknownLevel() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(request("Quế", null, FodmapLevel.UNKNOWN, null, null, null, "Nguồn")));

        assertEquals("Mức FODMAP không hợp lệ", ex.getMessage());
        verify(foodRepository, never()).save(any());
    }

    @Test
    void mediumRequiresServingNote() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(request("Quả bơ", null, FodmapLevel.MEDIUM, null, "  ", null, "Nguồn")));

        assertEquals("Món mức Trung bình phải có khẩu phần tham khảo", ex.getMessage());
    }

    @Test
    void rejectsInvalidGroup() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.create(request("Kẹo", null, FodmapLevel.HIGH, List.of("SUGAR"), null, null, "Nguồn")));

        assertTrue(ex.getMessage().contains("SUGAR"));
    }

    @Test
    void rejectsNameWithoutLetters() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request("!!", null, FodmapLevel.LOW, null, null, null, "Nguồn")));
    }

    @Test
    void rejectsDuplicateNameIgnoringAccents() {
        when(foodRepository.findAll()).thenReturn(List.of(onion, beef));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request("hanh TAY", null, FodmapLevel.HIGH, null, null, null, "Nguồn")));

        assertEquals("Tên «hanh TAY» đã được dùng cho món «Hành tây»", ex.getMessage());
        verify(foodRepository, never()).save(any());
    }

    @Test
    void rejectsAliasUsedByAnotherFood() {
        when(foodRepository.findAll()).thenReturn(List.of(onion, beef));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.create(request("Củ hành", List.of("Onion"), FodmapLevel.HIGH, null, null, null, "Nguồn")));

        assertEquals("Tên «Onion» đã được dùng cho món «Hành tây»", ex.getMessage());
    }

    @Test
    void updateMayKeepOwnNames() {
        when(foodRepository.findById(1L)).thenReturn(Optional.of(onion));
        when(foodRepository.findAll()).thenReturn(List.of(onion, beef));

        FodmapFoodResponse response = service.update(1L, request(
                "Hành tây", List.of("onion", "hành"), FodmapLevel.HIGH, List.of("FRUCTAN"), null, null, "Monash"));

        assertEquals(List.of("onion", "hành"), response.aliases());
        assertEquals("onion, hành", onion.getAliases());
        verify(unknownNameRepository).deleteByNormalizedName("hanh");
    }

    @Test
    void toggleActiveFlips() {
        when(foodRepository.findById(1L)).thenReturn(Optional.of(onion));

        assertFalse(service.toggleActive(1L).active());
        assertTrue(service.toggleActive(1L).active());
    }

    @Test
    void getByIdMissingThrowsNotFound() {
        when(foodRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(404L));
    }

    @Test
    void listFiltersByKeywordLevelAndActive() {
        onion.setActive(false);
        when(foodRepository.findAllByOrderByNameAsc()).thenReturn(List.of(onion, beef));

        assertEquals(List.of("Hành tây"), names(service.list("ONION", null, null)));
        assertEquals(List.of("Thịt bò"), names(service.list(null, FodmapLevel.LOW, null)));
        assertEquals(List.of("Hành tây"), names(service.list(null, null, false)));
        assertEquals(List.of("Hành tây", "Thịt bò"), names(service.list("  ", null, null)));
    }

    @Test
    void dismissUnknownMarksDismissed() {
        FodmapUnknownName unknown = new FodmapUnknownName("que", "Quế");
        when(unknownNameRepository.findById(5L)).thenReturn(Optional.of(unknown));

        service.dismissUnknown(5L);

        assertTrue(unknown.isDismissed());
    }

    private static List<String> names(List<FodmapFoodResponse> responses) {
        return responses.stream().map(FodmapFoodResponse::name).toList();
    }

    private static FodmapFoodRequest request(String name, List<String> aliases, FodmapLevel level,
                                             List<String> groups, String servingNote, String note,
                                             String source) {
        return new FodmapFoodRequest(name, aliases, level, groups, servingNote, note, source);
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