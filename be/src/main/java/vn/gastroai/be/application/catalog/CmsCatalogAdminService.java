package vn.gastroai.be.application.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.catalog.CmsCatalogItemRequest;
import vn.gastroai.be.api.cms.catalog.CmsCatalogItemResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.catalog.CatalogItem;
import vn.gastroai.be.domain.catalog.CatalogType;
import vn.gastroai.be.domain.notification.MedicationReminder;
import vn.gastroai.be.domain.patient.MedicalProfile;
import vn.gastroai.be.infrastructure.persistence.postgres.CatalogItemRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicalProfileRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.MedicationReminderRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.SymptomAssessmentRecordRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * UC0060 - Nghiep vu quan ly danh muc chuan hoa (trieu chung, benh ly, thuoc) cho Admin/Bien tap vien.
 */
@Service
public class CmsCatalogAdminService {

    private static final String TX = "postgresTransactionManager";
    private static final int SORT_STEP = 10;

    private final CatalogItemRepository catalogItemRepository;
    private final MedicalProfileRepository medicalProfileRepository;
    private final MedicationReminderRepository medicationReminderRepository;
    private final SymptomAssessmentRecordRepository symptomAssessmentRepository;
    private final ObjectMapper objectMapper;

    public CmsCatalogAdminService(CatalogItemRepository catalogItemRepository) {
        this(catalogItemRepository, null, null, null, new ObjectMapper());
    }

    @Autowired
    public CmsCatalogAdminService(
            CatalogItemRepository catalogItemRepository,
            MedicalProfileRepository medicalProfileRepository,
            MedicationReminderRepository medicationReminderRepository,
            SymptomAssessmentRecordRepository symptomAssessmentRepository,
            ObjectMapper objectMapper) {
        this.catalogItemRepository = catalogItemRepository;
        this.medicalProfileRepository = medicalProfileRepository;
        this.medicationReminderRepository = medicationReminderRepository;
        this.symptomAssessmentRepository = symptomAssessmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(value = TX, readOnly = true)
    public List<CmsCatalogItemResponse> list(CatalogType type, String query, Boolean active) {
        List<CatalogItem> items = type != null
                ? catalogItemRepository.findByTypeOrderBySortOrderAscNameAsc(type)
                : catalogItemRepository.findAllByOrderByTypeAscSortOrderAscNameAsc();

        String normalizedQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : null;

        List<CatalogItem> filtered = items.stream()
                .filter(item -> {
                    if (active != null && item.isActive() != active) {
                        return false;
                    }
                    if (normalizedQuery != null && !normalizedQuery.isEmpty()) {
                        boolean matchName = item.getName() != null && item.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                        boolean matchCode = item.getCode() != null && item.getCode().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                        boolean matchTriage = item.getTriageCode() != null && item.getTriageCode().toLowerCase(Locale.ROOT).contains(normalizedQuery);
                        return matchName || matchCode || matchTriage;
                    }
                    return true;
                })
                .toList();

        Map<Long, Long> usageCounts = computeUsageCounts(filtered);
        return filtered.stream()
                .map(item -> CmsCatalogItemResponse.from(item, usageCounts.getOrDefault(item.getId(), 0L)))
                .toList();
    }

    @Transactional(value = TX, readOnly = true)
    public CmsCatalogItemResponse getById(Long id) {
        CatalogItem item = load(id);
        long usageCount = computeUsageCounts(List.of(item)).getOrDefault(item.getId(), 0L);
        return CmsCatalogItemResponse.from(item, usageCount);
    }

    @Transactional(TX)
    public CmsCatalogItemResponse create(CmsCatalogItemRequest request) {
        CatalogType type = request.type();
        String code = cleanCode(request.code());
        String name = cleanName(request.name());
        String triageCode = cleanTriageCode(request.triageCode());

        if (catalogItemRepository.existsByTypeAndCodeIgnoreCase(type, code)) {
            throw new IllegalStateException("Mã \"" + code + "\" đã tồn tại trong danh mục " + type);
        }

        int sortOrder = request.sortOrder() != null ? request.sortOrder() : nextSortOrder(type);
        boolean active = request.active() == null || request.active();

        CatalogItem item = new CatalogItem();
        item.setType(type);
        item.setCode(code);
        item.setName(name);
        item.setTriageCode(triageCode);
        item.setSortOrder(sortOrder);
        item.setActive(active);

        CatalogItem saved = catalogItemRepository.saveAndFlush(item);
        return CmsCatalogItemResponse.from(saved);
    }

    @Transactional(TX)
    public CmsCatalogItemResponse update(Long id, CmsCatalogItemRequest request) {
        CatalogItem item = load(id);

        CatalogType type = request.type();
        String code = cleanCode(request.code());
        String name = cleanName(request.name());
        String triageCode = cleanTriageCode(request.triageCode());

        if (catalogItemRepository.existsByTypeAndCodeIgnoreCaseAndIdNot(type, code, id)) {
            throw new IllegalStateException("Mã \"" + code + "\" đã tồn tại trong danh mục " + type);
        }

        item.setType(type);
        item.setCode(code);
        item.setName(name);
        item.setTriageCode(triageCode);
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            item.setActive(request.active());
        }

        CatalogItem saved = catalogItemRepository.saveAndFlush(item);
        return CmsCatalogItemResponse.from(saved);
    }

    @Transactional(TX)
    public CmsCatalogItemResponse toggleActive(Long id) {
        CatalogItem item = load(id);
        item.setActive(!item.isActive());
        CatalogItem saved = catalogItemRepository.saveAndFlush(item);
        return CmsCatalogItemResponse.from(saved);
    }

    @Transactional(TX)
    public void delete(Long id) {
        delete(id, false);
    }

    @Transactional(TX)
    public void delete(Long id, boolean force) {
        CatalogItem item = load(id);
        if (!force) {
            long usageCount = computeUsageCounts(List.of(item)).getOrDefault(item.getId(), 0L);
            if (usageCount > 0) {
                throw new IllegalStateException("Mục \"" + item.getName() + "\" đang được sử dụng ở " + usageCount
                        + " hồ sơ/bản ghi. Hãy gộp sang mục khác hoặc tắt kích hoạt thay vì xoá trực tiếp.");
            }
        }
        catalogItemRepository.delete(item);
        catalogItemRepository.flush();
    }

    @Transactional(TX)
    public CmsCatalogItemResponse merge(Long sourceId, Long targetId) {
        if (sourceId == null || targetId == null || sourceId.equals(targetId)) {
            throw new IllegalArgumentException("Phải chọn 2 mục danh mục khác nhau để gộp.");
        }
        CatalogItem source = load(sourceId);
        CatalogItem target = load(targetId);
        if (source.getType() != target.getType()) {
            throw new IllegalArgumentException("Chỉ có thể gộp 2 mục thuộc cùng một loại danh mục.");
        }

        Set<String> sourceKeys = keysFor(source);
        switch (source.getType()) {
            case CONDITION -> mergeMedicalProfiles(sourceKeys, target.getName(), true);
            case MEDICATION -> {
                mergeMedicalProfiles(sourceKeys, target.getName(), false);
                mergeMedicationReminders(sourceKeys, target.getName());
            }
            case SYMPTOM -> {
                if (symptomAssessmentRepository != null) {
                    for (String key : sourceKeys) {
                        symptomAssessmentRepository.replacePrimarySymptom(key, target.getCode());
                    }
                }
            }
        }

        catalogItemRepository.delete(source);
        catalogItemRepository.flush();

        long updatedUsage = computeUsageCounts(List.of(target)).getOrDefault(target.getId(), 0L);
        return CmsCatalogItemResponse.from(target, updatedUsage);
    }

    private Map<Long, Long> computeUsageCounts(List<CatalogItem> items) {
        Map<Long, Long> counts = new HashMap<>();
        if (items.isEmpty()) {
            return counts;
        }
        List<MedicalProfile> profiles = medicalProfileRepository != null
                ? medicalProfileRepository.findAll()
                : Collections.emptyList();
        List<MedicationReminder> reminders = medicationReminderRepository != null
                ? medicationReminderRepository.findAll()
                : Collections.emptyList();
        Map<String, Long> symptomCounts = new HashMap<>();
        if (symptomAssessmentRepository != null) {
            for (Object[] row : symptomAssessmentRepository.countGroupedByPrimarySymptom()) {
                if (row[0] != null) {
                    symptomCounts.put(String.valueOf(row[0]).trim().toLowerCase(Locale.ROOT), (Long) row[1]);
                }
            }
        }

        for (CatalogItem item : items) {
            Set<String> keys = keysFor(item);
            long count = 0L;
            if (item.getType() == CatalogType.CONDITION) {
                for (MedicalProfile profile : profiles) {
                    if (containsAnyKey(parseJsonList(profile.getChronicConditions()), keys)) {
                        count++;
                    }
                }
            } else if (item.getType() == CatalogType.MEDICATION) {
                for (MedicalProfile profile : profiles) {
                    if (containsAnyKey(parseJsonList(profile.getCurrentMedications()), keys)) {
                        count++;
                    }
                }
                for (MedicationReminder reminder : reminders) {
                    if (reminder.getMedicineName() != null
                            && keys.contains(reminder.getMedicineName().trim().toLowerCase(Locale.ROOT))) {
                        count++;
                    }
                }
            } else if (item.getType() == CatalogType.SYMPTOM) {
                for (String key : keys) {
                    count += symptomCounts.getOrDefault(key, 0L);
                }
            }
            counts.put(item.getId(), count);
        }
        return counts;
    }

    private void mergeMedicalProfiles(Set<String> sourceKeys, String targetName, boolean isCondition) {
        if (medicalProfileRepository == null) return;
        List<MedicalProfile> profiles = medicalProfileRepository.findAll();
        for (MedicalProfile profile : profiles) {
            List<String> list = parseJsonList(isCondition ? profile.getChronicConditions() : profile.getCurrentMedications());
            boolean changed = false;
            LinkedHashSet<String> updated = new LinkedHashSet<>();
            for (String entry : list) {
                String trimmed = entry == null ? "" : entry.trim();
                if (sourceKeys.contains(trimmed.toLowerCase(Locale.ROOT))) {
                    updated.add(targetName);
                    changed = true;
                } else if (!trimmed.isEmpty()) {
                    updated.add(trimmed);
                }
            }
            if (changed) {
                String json = writeJsonList(new ArrayList<>(updated));
                if (isCondition) {
                    profile.setChronicConditions(json);
                } else {
                    profile.setCurrentMedications(json);
                }
                medicalProfileRepository.save(profile);
            }
        }
    }

    private void mergeMedicationReminders(Set<String> sourceKeys, String targetName) {
        if (medicationReminderRepository == null) return;
        for (MedicationReminder reminder : medicationReminderRepository.findAll()) {
            if (reminder.getMedicineName() != null
                    && sourceKeys.contains(reminder.getMedicineName().trim().toLowerCase(Locale.ROOT))) {
                reminder.setMedicineName(targetName);
                medicationReminderRepository.save(reminder);
            }
        }
    }

    private static Set<String> keysFor(CatalogItem item) {
        Set<String> keys = new LinkedHashSet<>();
        if (item.getCode() != null && !item.getCode().isBlank()) {
            keys.add(item.getCode().trim().toLowerCase(Locale.ROOT));
        }
        if (item.getName() != null && !item.getName().isBlank()) {
            keys.add(item.getName().trim().toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    private static boolean containsAnyKey(List<String> values, Set<String> keys) {
        for (String v : values) {
            if (v != null && keys.contains(v.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private List<String> parseJsonList(String rawJson) {
        if (rawJson == null || rawJson.isBlank() || "[]".equals(rawJson.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String writeJsonList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new IllegalStateException("Không serialize được danh sách hồ sơ bệnh án", e);
        }
    }

    private CatalogItem load(Long id) {
        return catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục chuẩn hóa với id " + id));
    }

    private int nextSortOrder(CatalogType type) {
        return catalogItemRepository.findByTypeOrderBySortOrderAscNameAsc(type).stream()
                .mapToInt(CatalogItem::getSortOrder)
                .max()
                .orElse(0) + SORT_STEP;
    }

    private static String cleanCode(String raw) {
        String code = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Mã chuẩn hóa không được để trống");
        }
        return code;
    }

    private static String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống");
        }
        return name;
    }

    private static String cleanTriageCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }
}
