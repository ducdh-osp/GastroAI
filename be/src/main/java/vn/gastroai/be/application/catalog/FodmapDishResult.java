package vn.gastroai.be.application.catalog;

import java.util.List;
import vn.gastroai.be.domain.catalog.FodmapLevel;

public record FodmapDishResult(
        String dishName,
        FodmapLevel overallLevel,
        String overallLabel,
        List<String> triggers,
        boolean complete,
        List<FodmapIngredientResult> ingredients,
        String disclaimer
) {

    public static final String DISCLAIMER =
            "Mức FODMAP chỉ mang tính tham khảo, không thay thế tư vấn của bác sĩ hoặc chuyên gia dinh dưỡng.";

    public static FodmapDishResult from(String dishName, List<FodmapIngredientResult> ingredients) {
        FodmapLevel max = null;
        for (FodmapIngredientResult item : ingredients) {
            if (item.found() && item.level().isHigherThan(max)) {
                max = item.level();
            }
        }
        FodmapLevel overall = (max == null) ? FodmapLevel.UNKNOWN : max;

        List<String> triggers = List.of();
        if (overall == FodmapLevel.MEDIUM || overall == FodmapLevel.HIGH) {
            triggers = ingredients.stream()
                    .filter(item -> item.found() && item.level() == overall)
                    .map(FodmapIngredientResult::matchedName)
                    .distinct()
                    .toList();
        }

        boolean complete = !ingredients.isEmpty()
                && ingredients.stream().allMatch(FodmapIngredientResult::found);

        return new FodmapDishResult(
                dishName,
                overall,
                overall.getLabel(),
                triggers,
                complete,
                ingredients,
                DISCLAIMER
        );
    }
}