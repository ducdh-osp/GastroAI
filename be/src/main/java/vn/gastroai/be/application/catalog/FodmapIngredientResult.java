package vn.gastroai.be.application.catalog;

import java.util.List;
import vn.gastroai.be.domain.catalog.FodmapFood;
import vn.gastroai.be.domain.catalog.FodmapLevel;

public record FodmapIngredientResult(
        String inputName,
        Long foodId,
        String matchedName,
        FodmapLevel level,
        String levelLabel,
        List<String> groups,
        String servingNote,
        String note
) {

    public static FodmapIngredientResult unknown(String inputName) {
        return new FodmapIngredientResult(
                inputName,
                null,
                null,
                FodmapLevel.UNKNOWN,
                FodmapLevel.UNKNOWN.getLabel(),
                List.of(),
                null,
                null
        );
    }

    public static FodmapIngredientResult of(String inputName, FodmapFood food) {
        return new FodmapIngredientResult(
                inputName,
                food.getId(),
                food.getName(),
                food.getLevel(),
                food.getLevel().getLabel(),
                food.getGroupList(),
                food.getServingNote(),
                food.getNote()
        );
    }

    public boolean found() {
        return foodId != null;
    }
}