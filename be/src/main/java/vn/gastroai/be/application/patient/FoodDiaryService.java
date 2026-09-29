package vn.gastroai.be.application.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.patient.DailyCountPoint;
import vn.gastroai.be.api.patient.FoodDiaryEntryRequest;
import vn.gastroai.be.api.patient.FoodDiaryEntryResponse;
import vn.gastroai.be.api.patient.FoodDiaryListResponse;
import vn.gastroai.be.api.patient.FoodDiaryTrendResponse;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** UC0011 (CRUD nhat ky an uong) + UC0012 (xem lich su/bieu do). */
@Service
public class FoodDiaryService {

    // Gom nhom theo ngay theo gio Viet Nam (khong dung gio server) - benh nhan quan tam
    // "an luc may gio theo dong ho cua ho", khong phai UTC.
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FoodDiaryEntryRepository foodDiaryEntryRepository;
    private final PatientRepository patientRepository;

    public FoodDiaryService(FoodDiaryEntryRepository foodDiaryEntryRepository, PatientRepository patientRepository) {
        this.foodDiaryEntryRepository = foodDiaryEntryRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional("postgresTransactionManager")
    public FoodDiaryEntryResponse create(Long patientId, FoodDiaryEntryRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        FoodDiaryEntry entry = new FoodDiaryEntry(patient, request.eatenAt(), request.description(), request.notes());
        return toResponse(foodDiaryEntryRepository.save(entry));
    }

    @Transactional("postgresTransactionManager")
    public FoodDiaryEntryResponse update(Long patientId, Long entryId, FoodDiaryEntryRequest request) {
        FoodDiaryEntry entry = loadOwned(patientId, entryId);
        entry.setEatenAt(request.eatenAt());
        entry.setDescription(request.description());
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

    /** UC0012 - so bua an ghi nhan moi ngay trong `days` ngay gan nhat, fill 0 cho ngay
     * khong co du lieu de bieu do lien tuc khong bi dut quang. */
    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public FoodDiaryTrendResponse trend(Long patientId, int days) {
        Instant to = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);

        List<FoodDiaryEntry> entries =
                foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(patientId, from, to);

        Map<LocalDate, Long> countByDate = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getEatenAt().atZone(VN_ZONE).toLocalDate(), Collectors.counting()));

        LocalDate start = from.atZone(VN_ZONE).toLocalDate();
        LocalDate end = to.atZone(VN_ZONE).toLocalDate();
        List<DailyCountPoint> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            points.add(new DailyCountPoint(day, countByDate.getOrDefault(day, 0L)));
        }
        return new FoodDiaryTrendResponse(points);
    }

    private FoodDiaryEntry loadOwned(Long patientId, Long entryId) {
        return foodDiaryEntryRepository.findById(entryId)
                .filter(e -> e.getPatient().getId().equals(patientId))
                .orElseThrow(() -> new IllegalArgumentException("Food diary entry not found or access denied"));
    }

    private FoodDiaryEntryResponse toResponse(FoodDiaryEntry entry) {
        return new FoodDiaryEntryResponse(entry.getId(), entry.getEatenAt(), entry.getDescription(), entry.getNotes());
    }
}
