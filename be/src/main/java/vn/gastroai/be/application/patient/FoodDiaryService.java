package vn.gastroai.be.application.patient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.OwnedResourceLoader;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnamDateRange;
import vn.gastroai.be.api.patient.DailyCountPoint;
import vn.gastroai.be.api.patient.FoodDiaryEntryRequest;
import vn.gastroai.be.api.patient.FoodDiaryEntryResponse;
import vn.gastroai.be.api.patient.FoodDiaryListResponse;
import vn.gastroai.be.api.patient.FoodDiaryTrashItem;
import vn.gastroai.be.api.patient.FoodDiaryTrendResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** UC0011 (CRUD nhat ky an uong) + UC0012 (xem lich su/bieu do) + UC0020 (thung rac). */
@Service
public class FoodDiaryService {

    // Gom nhom theo ngay theo gio Viet Nam (khong dung gio server) - benh nhan quan tam
    // "an luc may gio theo dong ho cua ho", khong phai UTC.
    private final FoodDiaryEntryRepository foodDiaryEntryRepository;
    private final PatientRepository patientRepository;
    // UC0020 - so ngay giu trong thung rac truoc khi bi don dep that su (xem application.yml).
    private final int retentionDays;

    public FoodDiaryService(FoodDiaryEntryRepository foodDiaryEntryRepository,
                            PatientRepository patientRepository,
                            @Value("${app.journal.trash-retention-days:30}") int retentionDays) {
        this.foodDiaryEntryRepository = foodDiaryEntryRepository;
        this.patientRepository = patientRepository;
        this.retentionDays = retentionDays;
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
        // loadOwned van dung findById nhu cu: dong da bi xoa truoc do se tu 404 vi
        // @SQLRestriction da an no khoi findById - dung y muon (khong xoa 2 lan).
        FoodDiaryEntry entry = loadOwned(patientId, entryId);
        entry.setDeletedAt(Instant.now());
        foodDiaryEntryRepository.save(entry);
    }

    /** UC0020 - danh sach thung rac, con trong han 30 ngay (hoac so ngay cau hinh). */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public List<FoodDiaryTrashItem> listTrash(Long patientId) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        return foodDiaryEntryRepository.findTrash(patientId, cutoff).stream()
                .map(this::toTrashItem)
                .toList();
    }

    /** UC0020 - khoi phuc 1 muc tu thung rac. 404 neu khong thay/khong phai cua benh nhan nay,
     * 409 neu da qua han giu (thuong la job don dep chua kip chay). */
    @Transactional("postgresTransactionManager")
    public FoodDiaryEntryResponse restore(Long patientId, Long entryId) {
        FoodDiaryEntry entry = foodDiaryEntryRepository.findDeletedOwned(entryId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Khong tim thay muc nhat ky an uong trong thung rac hoac ban khong co quyen truy cap"));
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        if (entry.getDeletedAt().isBefore(cutoff)) {
            throw new IllegalStateException(
                    "Mục này đã nằm trong thùng rác quá " + retentionDays + " ngày nên không thể khôi phục.");
        }
        entry.setDeletedAt(null);
        return toResponse(foodDiaryEntryRepository.save(entry));
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

    private FoodDiaryEntry loadOwned(Long patientId, Long entryId) {
        return OwnedResourceLoader.loadOwned(foodDiaryEntryRepository.findById(entryId),
                e -> e.getPatient().getId().equals(patientId),
                "Khong tim thay muc nhat ky an uong hoac ban khong co quyen truy cap");
    }

    private FoodDiaryEntryResponse toResponse(FoodDiaryEntry entry) {
        return new FoodDiaryEntryResponse(entry.getId(), entry.getEatenAt(), entry.getDescription(),
                entry.getMealType(), entry.getSymptomsAfterMeal(), entry.getSymptomOnsetMinutes(), entry.getNotes());
    }

    private FoodDiaryTrashItem toTrashItem(FoodDiaryEntry entry) {
        Instant deletedAt = entry.getDeletedAt();
        Instant purgeAt = deletedAt.plus(retentionDays, ChronoUnit.DAYS);
        return new FoodDiaryTrashItem(toResponse(entry), deletedAt, purgeAt);
    }
}