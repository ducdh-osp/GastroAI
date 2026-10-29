package vn.gastroai.be.application.audit;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.api.cms.audit.AuditLogResponse;
import vn.gastroai.be.domain.audit.AuditAction;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditLogExportServiceTest {

    private final AuditLogExportService service = new AuditLogExportService();

    private AuditLogResponse log(long id, AuditAction action, String detail) {
        return new AuditLogResponse(id, Instant.parse("2026-10-29T18:02:11Z"), "ADMIN", 1L,
                "admin@gastroai.vn", action, "DOCUMENT", "42", detail, "10.0.0.1", "Mozilla/5.0");
    }

    @Test
    void csvStartsWithBomAndHeader() {
        String csv = new String(service.toCsv(List.of()), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.startsWith("\uFEFFMã log,Thời gian,Loại tài khoản"));
    }

    @Test
    void csvRowUsesVietnamTimeAndVietnameseLabels() {
        String csv = new String(service.toCsv(List.of(log(7, AuditAction.DOCUMENT_DELETE, null))),
                StandardCharsets.UTF_8);
        String row = csv.split("\r\n")[1];

        assertEquals("7,30/10/2026 01:02:11,Admin,1,admin@gastroai.vn,Xoá tài liệu,DOCUMENT_DELETE,"
                + "DOCUMENT,42,,10.0.0.1,Mozilla/5.0", row);
    }

    @Test
    void escapeCsvQuotesCommasAndDoubleQuotes() {
        assertEquals("\"postgresOk=true, mysqlOk=true\"", AuditLogExportService.escapeCsv("postgresOk=true, mysqlOk=true"));
        assertEquals("\"Tên: \"\"HP\"\"\"", AuditLogExportService.escapeCsv("Tên: \"HP\""));
        assertEquals("\"dòng 1\ndòng 2\"", AuditLogExportService.escapeCsv("dòng 1\ndòng 2"));
        assertEquals("binh thuong", AuditLogExportService.escapeCsv("binh thuong"));
    }

    @Test
    void escapeCsvNeutralisesFormulaInjection() {
        assertEquals("'=1+1", AuditLogExportService.escapeCsv("=1+1"));
        assertEquals("'+84", AuditLogExportService.escapeCsv("+84"));
        assertEquals("'@SUM(A1)", AuditLogExportService.escapeCsv("@SUM(A1)"));
        assertEquals("\"'=HYPERLINK(\"\"http://x\"\",\"\"Bấm\"\")\"",
                AuditLogExportService.escapeCsv("=HYPERLINK(\"http://x\",\"Bấm\")"));
    }

    @Test
    void nullFieldsBecomeEmptyCells() {
        AuditLogResponse systemLog = new AuditLogResponse(9L, Instant.parse("2026-10-29T18:02:11Z"),
                "SYSTEM", null, null, AuditAction.SYSTEM_RESTORE, null, null, null, "unknown", "unknown");

        String[] row = service.toRow(systemLog);

        assertEquals("Hệ thống", row[2]);
        assertEquals("", row[3]);
        assertEquals("", row[4]);
        assertEquals("", row[7]);
        assertEquals("", row[9]);
    }

    @Test
    void excelHasHeaderRowsFrozenHeaderAndFilter() throws Exception {
        byte[] bytes = service.toExcel(List.of(
                log(1, AuditAction.PATIENT_LOCK, null),
                log(2, AuditAction.ANOMALY_DETECTED, "Khoá/mở khoá nhiều bệnh nhân: 10 trong 10 phút")));

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet("Nhật ký hoạt động");
            assertNotNull(sheet);
            assertEquals(2, sheet.getLastRowNum());

            Row header = sheet.getRow(0);
            assertEquals("Mã log", header.getCell(0).getStringCellValue());
            assertEquals("Thiết bị", header.getCell(AuditLogExportService.HEADERS.length - 1).getStringCellValue());

            Row second = sheet.getRow(2);
            assertEquals("Cảnh báo bất thường", second.getCell(5).getStringCellValue());
            assertEquals("ANOMALY_DETECTED", second.getCell(6).getStringCellValue());

            assertNotNull(sheet.getPaneInformation());
            assertTrue(sheet.getPaneInformation().isFreezePane());
        }
    }

    @Test
    void everyActionHasVietnameseLabel() {
        for (AuditAction action : AuditAction.values()) {
            String label = AuditLogExportService.actionLabel(action);
            assertFalse(label.isBlank(), "Thiếu nhãn cho " + action);
        }
    }
}