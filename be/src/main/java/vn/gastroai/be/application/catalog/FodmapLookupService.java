package vn.gastroai.be.application.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.catalog.FodmapFood;
import vn.gastroai.be.domain.catalog.FodmapUnknownName;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapFoodRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapUnknownNameRepository;

@Service
public class FodmapLookupService {

    private static final Logger log = LoggerFactory.getLogger(FodmapLookupService.class);

    static final int MAX_NAMES = 20;
    private static final int MAX_NAME_LENGTH = 120;

    private final FodmapFoodRepository foodRepository;
    private final FodmapUnknownNameRepository unknownNameRepository;

    public FodmapLookupService(FodmapFoodRepository foodRepository,
                               FodmapUnknownNameRepository unknownNameRepository) {
        this.foodRepository = foodRepository;
        this.unknownNameRepository = unknownNameRepository;
    }

    public FodmapDishResult lookupDish(String dishName, List<String> ingredients) {
        return FodmapDishResult.from(dishName, lookup(ingredients));
    }

    public List<FodmapIngredientResult> lookup(List<String> names) {
        List<String> cleaned = cleanNames(names);
        if (cleaned.isEmpty()) {
            return List.of();
        }

        Map<String, FodmapFood> index = buildIndex();
        List<FodmapIngredientResult> results = new ArrayList<>();
        Set<String> recorded = new HashSet<>();

        for (String name : cleaned) {
            String key = VietnameseText.normalizeKey(name);
            FodmapFood food = index.get(key);
            if (food != null) {
                results.add(FodmapIngredientResult.of(name, food));
            } else {
                results.add(FodmapIngredientResult.unknown(name));
                if (!key.isEmpty() && recorded.add(key)) {
                    recordUnknown(key, name);
                }
            }
        }
        return results;
    }

    private List<String> cleanNames(List<String> names) {
        if (names == null) {
            return List.of();
        }
        return names.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .limit(MAX_NAMES)
                .toList();
    }

    private Map<String, FodmapFood> buildIndex() {
        List<FodmapFood> foods = foodRepository.findByActiveTrue();
        Map<String, FodmapFood> index = new HashMap<>();
        for (FodmapFood food : foods) {
            index.put(food.getNormalizedName(), food);
        }
        for (FodmapFood food : foods) {
            for (String alias : food.getAliasList()) {
                index.putIfAbsent(VietnameseText.normalizeKey(alias), food);
            }
        }
        return index;
    }

    private void recordUnknown(String key, String sampleName) {
        try {
            String safeKey = cut(key);
            unknownNameRepository.findByNormalizedName(safeKey).ifPresentOrElse(
                    existing -> {
                        existing.recordHit();
                        unknownNameRepository.save(existing);
                    },
                    () -> unknownNameRepository.save(new FodmapUnknownName(safeKey, cut(sampleName)))
            );
        } catch (RuntimeException ex) {
            log.warn("Không ghi được tên FODMAP chưa có dữ liệu '{}': {}", key, ex.getMessage());
        }
    }

    private static String cut(String value) {
        return value.length() <= MAX_NAME_LENGTH ? value : value.substring(0, MAX_NAME_LENGTH);
    }
}