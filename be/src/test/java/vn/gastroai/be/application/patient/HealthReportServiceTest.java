package vn.gastroai.be.application.patient;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import vn.gastroai.be.api.notification.MedicationReminderResponse;
import vn.gastroai.be.api.patient.MedicalProfileResponse;
import vn.gastroai.be.application.notification.MedicationReminderService;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.patient.BristolLog;
import vn.gastroai.be.domain.patient.FoodDiaryEntry;
import vn.gastroai.be.domain.patient.MealType;
import vn.gastroai.be.infrastructure.persistence.postgres.BristolLogRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.FoodDiaryEntryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HealthReportServiceTest {

    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final MedicalProfileService medicalProfileService = mock(MedicalProfileService.class);
    private final FoodDiaryEntryRepository foodDiaryEntryRepository = mock(FoodDiaryEntryRepository.class);
    private final BristolLogRepository bristolLogRepository = mock(BristolLogRepository.class);
    private final MedicationReminderService medicationReminderService = mock(MedicationReminderService.class);

    private final HealthReportService service = new HealthReportService(
            patientRepository, medicalProfileService, foodDiaryEntryRepository,
            bristolLogRepository, medicationReminderService);

    // Ho so rong, giong gia tri fallback thuc te cua MedicalProfileService.getProfile() khi
    // benh nhan chua khai bao - dung chung cho moi test khong can kiem tra phan ho so benh ly.
    private static final MedicalProfileResponse EMPTY_PROFILE = new MedicalProfileResponse(
            null, null, null, null, null, null, null,
            List.of(), List.of(), List.of(), List.of(), List.of(), null, false);

    private Patient patient(String fullName) {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setFullName(fullName);
        patient.setPhone("0901234567");
        return patient;
    }

    private void stubEmptyCollaborators(Long patientId) {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient("Nguyễn Văn A")));
        when(medicalProfileService.getProfile(patientId)).thenReturn(EMPTY_PROFILE);
        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(eq(patientId), any(), any()))
                .thenReturn(List.of());
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(eq(patientId), any(), any()))
                .thenReturn(List.of());
        when(medicationReminderService.list(patientId)).thenReturn(List.of());
    }

    private String extractText(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    // Helper tao MedicationReminderResponse cho test - id/version/confirmedTimesToday khong
    // anh huong den logic overlapsPeriod/toReminderRow nen dat gia tri co dinh.
    private MedicationReminderResponse reminder(String medicineName, LocalDate startDate, LocalDate endDate,
                                                 boolean active) {
        return new MedicationReminderResponse(
                1L, 0L, medicineName, "1 viên", List.of(LocalTime.of(8, 0)),
                startDate, endDate, "Uống sau ăn", active, Set.of());
    }

    @Test
    void exportProducesPdfWithRealDataInVietnamese() throws IOException {
        Long patientId = 1L;
        Patient patient = patient("Nguyễn Văn A");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(medicalProfileService.getProfile(patientId)).thenReturn(EMPTY_PROFILE);
        when(medicationReminderService.list(patientId)).thenReturn(List.of());

        FoodDiaryEntry meal = new FoodDiaryEntry(patient, Instant.parse("2026-09-01T00:00:00Z"),
                "Phở bò", MealType.BREAKFAST, "đầy bụng", 30, null);
        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(eq(patientId), any(), any()))
                .thenReturn(List.of(meal));

        BristolLog bristolLog = new BristolLog(patient, Instant.parse("2026-09-01T06:00:00Z"), 4, null);
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(eq(patientId), any(), any()))
                .thenReturn(List.of(bristolLog));

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));

        assertEquals("%PDF", new String(pdf, 0, 4));

        String text = extractText(pdf);
        assertTrue(text.contains("Nhật ký ăn uống"), "Phai co tieu de muc nhat ky an uong");
        assertTrue(text.contains("Phở bò"), "Phai doc duoc ten mon an co dau");
        assertTrue(text.contains("Bữa sáng"), "Phai doc duoc nhan bua an co dau");
        assertTrue(text.contains("Loại 4"), "Phai doc duoc nhan Bristol co dau");
        assertTrue(text.contains("Nguyễn Văn A"), "Phai doc duoc ten benh nhan co dau");
    }

    @Test
    void exportStillProducesPdfWhenNoDataInRange() throws IOException {
        Long patientId = 1L;
        stubEmptyCollaborators(patientId);

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));

        assertEquals("%PDF", new String(pdf, 0, 4));
        String text = extractText(pdf);
        assertTrue(text.contains("Không có ghi nhận trong khoảng thời gian này."));
    }

    @Test
    void exportThrowsWhenFromIsAfterTo() {
        Long patientId = 1L;
        assertThrows(IllegalArgumentException.class,
                () -> service.export(patientId, LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1)));
    }

    @Test
    void exportThrowsWhenRangeExceeds90Days() {
        Long patientId = 1L;
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = from.plusDays(90); // 91 ngay tinh ca 2 dau
        assertThrows(IllegalArgumentException.class, () -> service.export(patientId, from, to));
    }

    @Test
    void exportThrowsWhenToIsInTheFuture() {
        Long patientId = 1L;
        LocalDate tomorrow = LocalDate.now(vn.gastroai.be.application.support.VietnamDateRange.ZONE).plusDays(1);
        assertThrows(IllegalArgumentException.class,
                () -> service.export(patientId, tomorrow.minusDays(1), tomorrow));
    }

    @Test
    void exportThrowsResourceNotFoundWhenPatientDoesNotExist() {
        Long patientId = 99L;
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());
        LocalDate day = LocalDate.of(2026, 9, 1);
        assertThrows(ResourceNotFoundException.class, () -> service.export(patientId, day, day));
    }

    @Test
    void exportConvertsVietnamCalendarDayToCorrectUtcInstantRange() {
        Long patientId = 1L;
        stubEmptyCollaborators(patientId);
        LocalDate day = LocalDate.of(2026, 9, 1);

        service.export(patientId, day, day);

        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(foodDiaryEntryRepository).findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(
                eq(patientId), fromCaptor.capture(), toCaptor.capture());

        // 00:00 gio Viet Nam (UTC+7) cua 01/09 = 17:00 UTC ngay 31/08.
        assertEquals(Instant.parse("2026-08-31T17:00:00Z"), fromCaptor.getValue());
        assertTrue(toCaptor.getValue().isBefore(Instant.parse("2026-09-01T17:00:00Z")));
    }
    @Test
    void exportSanitizesEmojiInNotesWithoutThrowing() throws IOException {
        Long patientId = 1L;
        Patient patient = patient("Nguyễn Văn A");
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(medicalProfileService.getProfile(patientId)).thenReturn(EMPTY_PROFILE);
        when(medicationReminderService.list(patientId)).thenReturn(List.of());
        when(bristolLogRepository.findByPatientIdAndLoggedAtBetweenOrderByLoggedAtAsc(eq(patientId), any(), any()))
                .thenReturn(List.of());

        FoodDiaryEntry meal = new FoodDiaryEntry(patient, Instant.parse("2026-09-01T00:00:00Z"),
                "Phở bò", MealType.BREAKFAST, null, null, "Ngon quá 😋🎉");
        when(foodDiaryEntryRepository.findByPatientIdAndEatenAtBetweenOrderByEatenAtAsc(eq(patientId), any(), any()))
                .thenReturn(List.of(meal));

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));

        String text = extractText(pdf);
        assertTrue(text.contains("Ngon quá"), "Phai giu duoc chu co dau, chi loai bo emoji");
    }

    @Test
    void exportUsesRenamedMedicationSectionTitle() throws IOException {
        Long patientId = 1L;
        stubEmptyCollaborators(patientId);

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));

        String text = extractText(pdf);
        assertTrue(text.contains("Thuốc trong giai đoạn này"), "Phai doi ten muc thuoc");
        assertTrue(!text.contains("Thuốc theo lịch nhắc"), "Khong con ten muc cu");
    }

    @Test
    void exportIncludesInactiveMedicationUsedDuringReportPeriod() throws IOException {
        Long patientId = 1L;
        stubEmptyCollaborators(patientId);
        // Thuoc da bi tat (active=false) nhung khoang dung [20/08 - 05/09] co giao voi
        // khoang bao cao [01/09 - 10/09] - phai van xuat hien trong bao cao.
        MedicationReminderResponse reminder = reminder("Paracetamol",
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 9, 5), false);
        when(medicationReminderService.list(patientId)).thenReturn(List.of(reminder));

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));

        String text = extractText(pdf);
        assertTrue(text.contains("Paracetamol"), "Thuoc da dung trong giai doan phai xuat hien du da tat");
        assertTrue(text.contains("Đã tắt"), "Phai hien thi trang thai da tat");
    }

    @Test
    void exportExcludesMedicationThatEndedBeforeReportPeriod() throws IOException {
        Long patientId = 1L;
        stubEmptyCollaborators(patientId);
        // Thuoc ket thuc 31/08, bao cao tu 01/09 - khong giao nhau, phai bi loai.
        MedicationReminderResponse reminder = reminder("Omeprazole",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), true);
        when(medicationReminderService.list(patientId)).thenReturn(List.of(reminder));

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));

        String text = extractText(pdf);
        assertTrue(!text.contains("Omeprazole"), "Thuoc da ket thuc truoc giai doan bao cao phai bi loai");
    }

    @Test
    void exportExcludesMedicationThatStartsAfterReportPeriod() throws IOException {
        Long patientId = 1L;
        stubEmptyCollaborators(patientId);
        // Thuoc moi duoc them, bat dau 15/09 - sau khi bao cao (01/09 - 10/09) da ket thuc,
        // du dang active=true van phai bi loai vi chua tung dung trong giai doan nay.
        MedicationReminderResponse reminder = reminder("Domperidone",
                LocalDate.of(2026, 9, 15), null, true);
        when(medicationReminderService.list(patientId)).thenReturn(List.of(reminder));

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));

        String text = extractText(pdf);
        assertTrue(!text.contains("Domperidone"), "Thuoc chi dung sau giai doan bao cao phai bi loai");
    }

    @Test
    void exportIncludesOpenEndedActiveMedicationWithStatusColumn() throws IOException {
        Long patientId = 1L;
        stubEmptyCollaborators(patientId);
        // Thuoc dang dung, khong co ngay ket thuc (endDate = null) - luon duoc coi la giao
        // voi moi khoang bao cao tu sau ngay bat dau.
        MedicationReminderResponse reminder = reminder("Simethicone",
                LocalDate.of(2026, 8, 1), null, true);
        when(medicationReminderService.list(patientId)).thenReturn(List.of(reminder));

        byte[] pdf = service.export(patientId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));

        String text = extractText(pdf);
        assertTrue(text.contains("Simethicone"), "Thuoc khong gioi han ngay ket thuc phai xuat hien");
        assertTrue(text.contains("Đang nhắc"), "Phai hien thi trang thai dang nhac");
    }
}