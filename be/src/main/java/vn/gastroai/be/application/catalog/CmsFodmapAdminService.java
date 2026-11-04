package vn.gastroai.be.application.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.catalog.FodmapFoodRequest;
import vn.gastroai.be.api.cms.catalog.FodmapFoodResponse;
import vn.gastroai.be.api.cms.catalog.FodmapUnknownNameResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.catalog.FodmapFood;
import vn.gastroai.be.domain.catalog.FodmapLevel;
import vn.gastroai.be.domain.catalog.FodmapUnknownName;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapFoodRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FodmapUnknownNameRepository;

@Service
public class CmsFodmapAdminService {

    private static final String TX = "postgresTransactionManager";
    static final Set<String> ALLOWED_GROUPS = Set.of("FRUCTAN", "GOS", "LACTOSE", "FRUCTOSE", "POLYOL");

    private final FodmapFoodRepository foodRepository;
    private final FodmapUnknownNameRepository unknownNameRepository;

    public CmsFodmapAdminService(FodmapFoodRepository foodRepository,
                                 FodmapUnknownNameRepository unknownNameRepository) {
        this.foodRepository = foodRepository;
        this.unknownNameRepository = unknownNameRepository;
    }

    @Transactional(value = TX, readOnly = true)
    public List<FodmapFoodResponse> list(String q, FodmapLevel level, Boolean active) {
        String keyword = VietnameseText.normalizeKey(q);
        return foodRepository.findAllByOrderByNameAsc().stream()
                .filter(food -> level == null || food.getLevel() == level)
                .filter(food -> active == null || food.isActive() == active)
                .filter(food -> keyword.isEmpty()
                        || keysOf(food).stream().anyMatch(key -> key.contains(keyword)))
                .map(FodmapFoodResponse::from)
                .toList();
    }

    @Transactional(value = TX, readOnly = true)
    public FodmapFoodResponse getById(Long id) {
        return FodmapFoodResponse.from(load(id));
    }

    @Transactional(TX)
    public FodmapFoodResponse create(FodmapFoodRequest request) {
        Cleaned cleaned = clean(request, null);
        FodmapFood food = new FodmapFood();
        apply(food, cleaned, request);
        FodmapFood saved = foodRepository.save(food);
        removeFromUnknown(cleaned);
        return FodmapFoodResponse.from(saved);
    }

    @Transactional(TX)
    public FodmapFoodResponse update(Long id, FodmapFoodRequest request) {
        FodmapFood food = load(id);
        Cleaned cleaned = clean(request, id);
        apply(food, cleaned, request);
        removeFromUnknown(cleaned);
        return FodmapFoodResponse.from(food);
    }

    @Transactional(TX)
    public FodmapFoodResponse toggleActive(Long id) {
        FodmapFood food = load(id);
        food.setActive(!food.isActive());
        return FodmapFoodResponse.from(food);
    }

    @Transactional(TX)
    public void delete(Long id) {
        FodmapFood food = load(id);
        foodRepository.delete(food);
    }

    @Transactional(value = TX, readOnly = true)
    public List<FodmapUnknownNameResponse> listUnknown() {
        return unknownNameRepository.findByDismissedFalseOrderByHitCountDescLastSeenAtDesc().stream()
                .map(FodmapUnknownNameResponse::from)
                .toList();
    }

    @Transactional(TX)
    public FodmapUnknownNameResponse dismissUnknown(Long id) {
        FodmapUnknownName unknown = loadUnknown(id);
        unknown.dismiss();
        return FodmapUnknownNameResponse.from(unknown);
    }

    private Cleaned clean(FodmapFoodRequest request, Long selfId) {
        if (!request.level().isStorable()) {
            throw new IllegalArgumentException("Mức FODMAP không hợp lệ");
        }

        String name = request.name().trim();
        String nameKey = VietnameseText.normalizeKey(name);
        if (nameKey.isEmpty()) {
            throw new IllegalArgumentException("Tên món không hợp lệ");
        }

        if (request.level() == FodmapLevel.MEDIUM && blankToNull(request.servingNote()) == null) {
            throw new IllegalArgumentException("Món mức Trung bình phải có khẩu phần tham khảo");
        }

        Set<String> groups = new LinkedHashSet<>();
        if (request.groups() != null) {
            for (String raw : request.groups()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String group = raw.trim().toUpperCase(Locale.ROOT);
                if (!ALLOWED_GROUPS.contains(group)) {
                    throw new IllegalArgumentException("Nhóm FODMAP không hợp lệ: " + raw);
                }
                groups.add(group);
            }
        }

        Map<String, String> aliasByKey = new LinkedHashMap<>();
        if (request.aliases() != null) {
            for (String raw : request.aliases()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String alias = raw.trim();
                String key = VietnameseText.normalizeKey(alias);
                if (!key.isEmpty() && !key.equals(nameKey)) {
                    aliasByKey.putIfAbsent(key, alias);
                }
            }
        }

        Map<String, String> requestKeys = new LinkedHashMap<>();
        requestKeys.put(nameKey, name);
        requestKeys.putAll(aliasByKey);

        for (FodmapFood other : foodRepository.findAll()) {
            if (Objects.equals(other.getId(), selfId)) {
                continue;
            }
            Set<String> otherKeys = keysOf(other);
            for (Map.Entry<String, String> entry : requestKeys.entrySet()) {
                if (otherKeys.contains(entry.getKey())) {
                    throw new IllegalStateException(
                            "Tên «" + entry.getValue() + "» đã được dùng cho món «" + other.getName() + "»");
                }
            }
        }

        return new Cleaned(name, nameKey, new ArrayList<>(aliasByKey.values()),
                new ArrayList<>(groups), new ArrayList<>(requestKeys.keySet()));
    }

    private void apply(FodmapFood food, Cleaned cleaned, FodmapFoodRequest request) {
        food.setName(cleaned.name());
        food.setNormalizedName(cleaned.nameKey());
        food.setAliasList(cleaned.aliases());
        food.setLevel(request.level());
        food.setGroupList(cleaned.groups());
        food.setServingNote(blankToNull(request.servingNote()));
        food.setNote(blankToNull(request.note()));
        food.setSource(request.source().trim());
    }

    private void removeFromUnknown(Cleaned cleaned) {
        for (String key : cleaned.allKeys()) {
            unknownNameRepository.deleteByNormalizedName(key);
        }
    }

    private static Set<String> keysOf(FodmapFood food) {
        Set<String> keys = new LinkedHashSet<>();
        keys.add(food.getNormalizedName());
        for (String alias : food.getAliasList()) {
            keys.add(VietnameseText.normalizeKey(alias));
        }
        return keys;
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private FodmapFood load(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy món FODMAP #" + id));
    }

    private FodmapUnknownName loadUnknown(Long id) {
        return unknownNameRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tên chưa có dữ liệu #" + id));
    }

    private record Cleaned(String name, String nameKey, List<String> aliases,
                           List<String> groups, List<String> allKeys) {
    }
}