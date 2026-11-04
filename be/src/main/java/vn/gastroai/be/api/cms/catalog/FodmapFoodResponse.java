package vn.gastroai.be.api.cms.catalog;

import java.time.Instant;
import java.util.List;
import vn.gastroai.be.domain.catalog.FodmapFood;
import vn.gastroai.be.domain.catalog.FodmapLevel;

public record FodmapFoodResponse(
        Long id,
        String name,
        List<String> aliases,
        FodmapLevel level,
        String levelLabel,
        List<String> groups,
        String servingNote,
        String note,
        String source,
        boolean active,
        Instant updatedAt
) {
    public static FodmapFoodResponse from(FodmapFood food) {
        return new FodmapFoodResponse(
                food.getId(),
                food.getName(),
                food.getAliasList(),
                food.getLevel(),
                food.getLevel().getLabel(),
                food.getGroupList(),
                food.getServingNote(),
                food.getNote(),
                food.getSource(),
                food.isActive(),
                food.getUpdatedAt()
        );
    }
}