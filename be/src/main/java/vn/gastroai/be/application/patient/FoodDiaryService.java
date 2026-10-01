package vn.gastroai.be.application.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.OwnedResourceLoader;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnamDateRange;
import vn.gastroai.be.api.patient.DailyCountPoint;
import vn.gastroai.be.api.patient.DigestiveTimelineItem;
import vn.gastroai.be.api.patient.DigestiveTimelineResponse;
import vn.gastroai.be.api.patient.FoodDiaryEntryRequest;
import vn.gastroai.be.api.patient.FoodDiaryEntryResponse;
import vn.gastroai.be.api.patient.FoodDiaryListResponse;
import vn.gastroai.be.api.patient.FoodDiaryTrendResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** UC0011 (CRUD nhat ky an uong) + UC0012 (xem lich su/bieu do). */
@Service
public class FoodDiaryService {

    // Gom nhom theo ngay theo gio Viet Nam (khong dung gio server) - benh nhan quan tam
    // "an luc may gio theo dong ho cua ho", khong phai UTC.
    private final FoodDiaryEntryRepository foodDiaryEntryRepository;
    private final PatientRepository patientRepository;
    private final BristolLogRepository bristolLogRepository;

    public FoodDiaryService(FoodDiaryEntryRepository foodDiaryEntryRepository,
                            PatientRepository patientRepository,
                            BristolLogRepository bristolLogRepository) {
        this.foodDiaryEntryRepository = foodDiaryEntryRepository;
        this.patientRepository = patientRepository;
        this.bristolLogRepository = bristolLogRepository;
    }

    @Transactional("postgresTransactionManager")
    public FoodDiaryEntryResponse create(Long patientId, FoodDiaryEntryRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay benh nhan"));
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, request.eatenAt(), request.description(),
                request.mealType(), request.symptomsAfterMeal(), request.symptomOnsetMinutes(), request.notes());
        return toResponse(foodDiaryEntryRepository.save(entry));
    }

    @Transactional("postgresTransactionManager")
    public FoodDiaryEntryResponse update(Long patientId, Long entryId, FoodDiaryEntryRequest request) {
        FoodDiaryEntry entry = loadOwned(patientId, entryId);
        entry.setEatenAt(request.eatenAt());
        entry.setDescription(request.description());
        entry.setMealType(request.mealType());
        entry.setSymptomsAfterMeal(request.symptomsAfterMeal());
        entry.setSymptomOnsetMinutes(request.symptomOnsetMinutes());
        entry.setNotes(request.notes());
        return toResponse(foodDiaryEntryRepository.save(entry));
    }

    @Transactional("postgresTransactionManager")
    public void delete(Long patientId, Long entryId) {
        FoodDiaryEntry entry = loadOwned(patientId, entryId);
        foodDiaryEntryRepository.delete(entry);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public FoodDiaryListResponse list(Long patientId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<FoodDiaryEntry> result = foodDiaryEntryRepository.findByPatientIdOrderByEatenAtDesc(patientId, pageable);
        List<FoodDiaryEntryResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return new FoodDiaryListResponse(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** UC0012 - so bua an ghi nhan moi ngay trong dung `days` ngay gan nhat (ke ca hom nay),
     * fill 0 cho ngay khong co du lieu de bieu do lien tuc khong bi dut quang.
     * Tinh start/end truc tiep tu LocalDate (khong suy tu Instant.now().minus(days)) de
     * chac chan co dung `days` diem - truoc day tinh end tu `to` va start tu `to - days`
     * roi lay ca 2 dau nen ra `days + 1` diem (off-by-one da phat hien qua code review). */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public FoodDiaryTrendResponse trend(Long patientId, int days) {
        VietnamDateRange.Range range = VietnamDateRange.recentDaysIncludingToday(days);
        LocalDate start = range.startDate();
        LocalDate endExclusive = range.endExclusiveDate();

        List<FoodDiaryEntry> entries =
                foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(
                        patientId, range.fromInclusive(), range.toExclusive());

        Map<LocalDate, Long> countByDate = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getEatenAt().atZone(VietnamDateRange.ZONE).toLocalDate(), Collectors.counting()));

        List<DailyCountPoint> points = start.datesUntil(endExclusive)
                .map(day -> new DailyCountPoint(day, countByDate.getOrDefault(day, 0L)))
                .toList();
        return new FoodDiaryTrendResponse(points);
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public DigestiveTimelineResponse timeline(Long patientId, int days) {
        VietnamDateRange.Range range = VietnamDateRange.recentDaysIncludingToday(days);
        List<DigestiveTimelineItem> meals = foodDiaryEntryRepository
                .findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(
                        patientId, range.fromInclusive(), range.toExclusive())
                .stream()
                .map(entry -> new DigestiveTimelineItem(
                        "MEAL", entry.getId(), entry.getEatenAt(), entry.getMealType(),
                        entry.getDescription(), entry.getSymptomsAfterMeal(), entry.getSymptomOnsetMinutes(),
                        null, entry.getNotes()))
                .toList();
        List<DigestiveTimelineItem> bristolLogs = bristolLogRepository
                .findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(
                        patientId, range.fromInclusive(), range.toExclusive())
                .stream()
                .map(log -> new DigestiveTimelineItem(
                        "BRISTOL", log.getId(), log.getLoggedAt(), null,
                        null, null, null, log.getBristolType(), log.getNotes()))
                .toList();
        List<DigestiveTimelineItem> items = java.util.stream.Stream.concat(meals.stream(), bristolLogs.stream())
                .sorted(java.util.Comparator.comparing(DigestiveTimelineItem::occurredAt).reversed())
                .toList();
        return new DigestiveTimelineResponse(items);
    }

    private FoodDiaryEntry loadOwned(Long patientId, Long entryId) {
        return OwnedResourceLoader.loadOwned(foodDiaryEntryRepository.findById(entryId),
                e -> e.getPatient().getId().equals(patientId),
                "Khong tim thay muc nhat ky an uong hoac ban khong co quyen truy cap");
    }

    private FoodDiaryEntryResponse toResponse(FoodDiaryEntry entry) {
        return new FoodDiaryEntryResponse(entry.getId(), entry.getEatenAt(), entry.getDescription(),
                entry.getMealType(), entry.getSymptomsAfterMeal(), entry.getSymptomOnsetMinutes(), entry.getNotes());
    }
}
