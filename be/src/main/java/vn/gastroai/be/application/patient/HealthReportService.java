package vn.gastroai.be.application.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.notification.MedicationReminderResponse;
import vn.gastroai.be.api.patient.MedicalProfileResponse;
import vn.gastroai.be.application.notification.MedicationReminderService;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnamDateRange;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.domain.patient.MealType;
import vn.gastroai.be.infrastructure.pdf.PdfDocumentBuilder;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UC0019 - gom du lieu benh ly/thuoc/nhat ky an uong/Bristol cua 1 benh nhan trong 1
 * khoang ngay, dung PdfDocumentBuilder de xuat ra 1 file PDF benh nhan co the mang theo
 * khi di kham. Gioi han toi da 90 ngay/lan de file khong qua dai va truy van khong qua nang.
 */
@Service
public class HealthReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(VietnamDateRange.ZONE);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final Map<MealType, String> MEAL_TYPE_LABELS = Map.of(
            MealType.BREAKFAST, "Bữa sáng",
            MealType.LUNCH, "Bữa trưa",
            MealType.DINNER, "Bữa tối",
            MealType.SNACK, "Ăn vặt",
            MealType.OTHER, "Khác");

    // Chep dung tu fe/src/constants/bristol.ts - de PDF va giao dien dung cung 1 cach dien dat.
    private static final Map<Integer, String> BRISTOL_TYPE_LABELS = Map.of(
            1, "Loại 1 – Cứng, rời rạc (táo bón nặng)",
            2, "Loại 2 – Hình khúc, vón cục",
            3, "Loại 3 – Hình khúc, có vết nứt bề mặt",
            4, "Loại 4 – Mềm, mịn, hình chuối (lý tưởng)",
            5, "Loại 5 – Cục mềm, rời, bờ rõ",
            6, "Loại 6 – Bột nhão, bờ nham nhở",
            7, "Loại 7 – Lỏng hoàn toàn, không có phần rắn (tiêu chảy)");

    private static final Map<String, String> GENDER_LABELS = Map.of(
            "MALE", "Nam",
            "FEMALE", "Nữ",
            "OTHER", "Khác");

    private final PatientRepository patientRepository;
    private final MedicalProfileService medicalProfileService;
    private final FoodDiaryEntryRepository foodDiaryEntryRepository;
    private final BristolLogRepository bristolLogRepository;
    private final MedicationReminderService medicationReminderService;

    public HealthReportService(PatientRepository patientRepository,
                                MedicalProfileService medicalProfileService,
                                FoodDiaryEntryRepository foodDiaryEntryRepository,
                                BristolLogRepository bristolLogRepository,
                                MedicationReminderService medicationReminderService) {
        this.patientRepository = patientRepository;
        this.medicalProfileService = medicalProfileService;
        this.foodDiaryEntryRepository = foodDiaryEntryRepository;
        this.bristolLogRepository = bristolLogRepository;
        this.medicationReminderService = medicationReminderService;
    }

    @Transactional(value = "postgresTransactionManager", readOnly = true)
    public byte[] export(Long patientId, LocalDate from, LocalDate to) {
        validateRange(from, to);

        Instant start = from.atStartOfDay(VietnamDateRange.ZONE).toInstant();
        // +1 ngay roi tru 1 nano - cac ham findBy...Between lay ca 2 dau, khong tru se tinh
        // nham ban ghi dung 00:00:00 cua ngay hom sau vao khoang nay.
        Instant end = to.plusDays(1).atStartOfDay(VietnamDateRange.ZONE).toInstant().minusNanos(1);

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay benh nhan"));
        MedicalProfileResponse profile = medicalProfileService.getProfile(patientId);
        List<FoodDiaryEntry> meals = foodDiaryEntryRepository
                .findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(patientId, start, end);
        List<BristolLog> bristolLogs = bristolLogRepository
                .findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(patientId, start, end);
        List<MedicationReminderResponse> activeReminders = medicationReminderService.list(patientId).stream()
                .filter(MedicationReminderResponse::active)
                .toList();

        PdfDocumentBuilder builder = new PdfDocumentBuilder();
        builder.title("NHẬT KÝ SỨC KHỎE TIÊU HÓA");
        builder.subtitle("Từ " + DATE_FORMAT.format(from) + " đến " + DATE_FORMAT.format(to)
                + " · Xuất lúc " + DATE_TIME_FORMAT.format(Instant.now()));

        builder.section("Thông tin bệnh nhân");
        builder.keyValue("Họ tên", patient.getFullName());
        builder.keyValue("Số điện thoại", patient.getPhone());
        builder.keyValue("Ngày sinh", profile.dateOfBirth() == null ? null : DATE_FORMAT.format(profile.dateOfBirth()));
        builder.keyValue("Giới tính", profile.gender() == null ? null : GENDER_LABELS.get(profile.gender()));
        builder.keyValue("Chiều cao (cm)", profile.heightCm() == null ? null : profile.heightCm().toString());
        builder.keyValue("Cân nặng (kg)", profile.weightKg() == null ? null : profile.weightKg().toString());

        builder.section("Hồ sơ bệnh lý");
        if (!profile.exists()) {
            builder.paragraph("Chưa khai báo hồ sơ bệnh lý");
        } else {
            builder.keyValue("Tiền sử bệnh", profile.medicalHistory());
            builder.keyValue("Dị ứng", String.join(", ", profile.allergies()));
            builder.keyValue("Bệnh mạn tính", String.join(", ", profile.chronicConditions()));
            builder.keyValue("Phẫu thuật", String.join(", ", profile.pastSurgeries()));
            builder.keyValue("Thuốc đang dùng", String.join(", ", profile.currentMedications()));
            builder.keyValue("Chế độ ăn kiêng", String.join(", ", profile.dietaryRestrictions()));
        }

        builder.section("Thuốc theo lịch nhắc");
        builder.table(
                new String[] {"Tên thuốc", "Liều", "Khung giờ", "Thời gian dùng", "Hướng dẫn"},
                new float[] {2, 1.2f, 1.5f, 1.8f, 2.5f},
                activeReminders.stream().map(this::toReminderRow).toList());

        builder.section("Nhật ký ăn uống");
        builder.table(
                new String[] {"Thời gian", "Bữa", "Món ăn", "Triệu chứng sau ăn", "Khởi phát (phút)", "Ghi chú"},
                new float[] {1.4f, 1, 1.8f, 1.8f, 1, 1.5f},
                meals.stream().map(this::toMealRow).toList());

        builder.section("Nhật ký Bristol");
        builder.table(
                new String[] {"Thời gian", "Phân loại", "Ghi chú"},
                new float[] {1.4f, 3, 2},
                bristolLogs.stream().map(this::toBristolRow).toList());

        // In ca muc Tom tat thanh 1 khoi giu tren cung 1 trang - truoc day in tung dong rieng
        // nen bi cat doi khi gan cuoi trang (tieu de o trang 1, cac dong con lai o trang 2).
        builder.keyValueBlock("Tóm tắt", buildSummary(meals, bristolLogs));

        builder.note("Thông tin do người dùng tự ghi nhận trên GastroAI, chỉ mang tính tham khảo, "
                + "không thay thế chẩn đoán của bác sĩ.");

        return builder.build();
    }

    /** Tinh cac dong tom tat (nhan - gia tri), chua in gi vao PDF - keyValueBlock() se in ca khoi. */
    private List<String[]> buildSummary(List<FoodDiaryEntry> meals, List<BristolLog> bristolLogs) {
        List<String[]> lines = new ArrayList<>();

        Map<Integer, Long> bristolCounts = bristolLogs.stream()
                .collect(Collectors.groupingBy(BristolLog::getBristolType, Collectors.counting()));
        for (int type = 1; type <= 7; type++) {
            long count = bristolCounts.getOrDefault(type, 0L);
            if (count > 0) {
                lines.add(new String[] {BRISTOL_TYPE_LABELS.get(type), count + " lần"});
            }
        }

        long mealsWithSymptoms = meals.stream()
                .filter(m -> m.getSymptomsAfterMeal() != null && !m.getSymptomsAfterMeal().isBlank())
                .count();
        lines.add(new String[] {"Số bữa ăn có ghi triệu chứng", mealsWithSymptoms + " / " + meals.size()});

        Set<LocalDate> daysWithRecord = new HashSet<>();
        meals.forEach(m -> daysWithRecord.add(m.getEatenAt().atZone(VietnamDateRange.ZONE).toLocalDate()));
        bristolLogs.forEach(b -> daysWithRecord.add(b.getLoggedAt().atZone(VietnamDateRange.ZONE).toLocalDate()));
        lines.add(new String[] {"Số ngày có ít nhất một ghi nhận", String.valueOf(daysWithRecord.size())});

        return lines;
    }

    private String[] toReminderRow(MedicationReminderResponse reminder) {
        String times = reminder.timesOfDay().stream().map(TIME_FORMAT::format).collect(Collectors.joining(", "));
        String period = (reminder.startDate() == null ? "—" : DATE_FORMAT.format(reminder.startDate()))
                + " – " + (reminder.endDate() == null ? "—" : DATE_FORMAT.format(reminder.endDate()));
        return new String[] {reminder.medicineName(), reminder.dosage(), times, period, reminder.instructions()};
    }

    private String[] toMealRow(FoodDiaryEntry entry) {
        return new String[] {
                DATE_TIME_FORMAT.format(entry.getEatenAt()),
                MEAL_TYPE_LABELS.get(entry.getMealType()),
                entry.getDescription(),
                entry.getSymptomsAfterMeal(),
                entry.getSymptomOnsetMinutes() == null ? null : entry.getSymptomOnsetMinutes().toString(),
                entry.getNotes()
        };
    }

    private String[] toBristolRow(BristolLog log) {
        return new String[] {
                DATE_TIME_FORMAT.format(log.getLoggedAt()),
                BRISTOL_TYPE_LABELS.get(log.getBristolType()),
                log.getNotes()
        };
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Vui lòng chọn khoảng thời gian");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        LocalDate today = LocalDate.now(VietnamDateRange.ZONE);
        if (to.isAfter(today)) {
            throw new IllegalArgumentException("Không thể xuất dữ liệu của ngày trong tương lai");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > 90) {
            throw new IllegalArgumentException("Chỉ xuất tối đa 90 ngày mỗi lần");
        }
    }
}