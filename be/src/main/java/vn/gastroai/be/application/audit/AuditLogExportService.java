package vn.gastroai.be.application.audit;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.gastroai.be.api.cms.audit.AuditLogResponse;
import vn.gastroai.be.domain.audit.AuditAction;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** UC0064 - tao file CSV/Excel tu danh sach audit log da loc. */
@Service
public class AuditLogExportService {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    static final String[] HEADERS = {
            "Mã log", "Thời gian", "Loại tài khoản", "Mã tài khoản", "Email",
            "Hành động", "Mã hành động", "Loại đối tượng", "Mã đối tượng",
            "Chi tiết", "Địa chỉ IP", "Thiết bị"
    };

    private static final int[] COLUMN_WIDTHS = {10, 20, 14, 12, 30, 26, 26, 16, 22, 60, 18, 50};

    public byte[] toCsv(List<AuditLogResponse> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        appendCsvRow(csv, HEADERS);
        for (AuditLogResponse log : logs) {
            appendCsvRow(csv, toRow(log));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toExcel(List<AuditLogResponse> logs) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Nhật ký hoạt động");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }

            int rowIndex = 1;
            for (AuditLogResponse log : logs) {
                Row row = sheet.createRow(rowIndex++);
                String[] values = toRow(log);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(rowIndex - 1, 0), 0, HEADERS.length - 1));

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("Không thể tạo file Excel nhật ký hoạt động", exception);
        }
    }

    String[] toRow(AuditLogResponse log) {
        return new String[] {
                String.valueOf(log.id()),
                TIME_FORMAT.format(log.occurredAt()),
                actorLabel(log.actorType()),
                log.actorId() == null ? "" : String.valueOf(log.actorId()),
                nullToEmpty(log.actorEmail()),
                actionLabel(log.action()),
                log.action().name(),
                nullToEmpty(log.targetType()),
                nullToEmpty(log.targetId()),
                nullToEmpty(log.detail()),
                nullToEmpty(log.ipAddress()),
                nullToEmpty(log.userAgent())
        };
    }

    static String actionLabel(AuditAction action) {
        return switch (action) {
            case DOCUMENT_UPLOAD -> "Tải lên tài liệu";
            case DOCUMENT_NEW_VERSION -> "Tải phiên bản mới";
            case DOCUMENT_ACTIVATE_VERSION -> "Kích hoạt phiên bản";
            case DOCUMENT_UPDATE_INFO -> "Đổi thông tin tài liệu";
            case DOCUMENT_PUBLISH -> "Đăng tài liệu";
            case DOCUMENT_UNPUBLISH -> "Gỡ tài liệu";
            case DOCUMENT_DELETE_VERSION -> "Xoá phiên bản";
            case DOCUMENT_DELETE -> "Xoá tài liệu";
            case RBAC_ROLE_CHANGE -> "Đổi vai trò";
            case PATIENT_LOCK -> "Khoá bệnh nhân";
            case PATIENT_UNLOCK -> "Mở khoá bệnh nhân";
            case PATIENT_DATA_VIEW -> "Xem dữ liệu bệnh nhân";
            case PASSWORD_CHANGE -> "Đổi mật khẩu";
            case SYSTEM_RESTORE -> "Khôi phục dữ liệu";
            case ANOMALY_DETECTED -> "Cảnh báo bất thường";
            case ANOMALY_RULE_UPDATE -> "Sửa quy tắc cảnh báo";
            case AUDIT_LOG_EXPORT -> "Xuất nhật ký";
        };
    }

    private static String actorLabel(String actorType) {
        if (actorType == null) {
            return "";
        }
        return switch (actorType) {
            case "ADMIN" -> "Admin";
            case "DOCTOR" -> "Bác sĩ";
            case "SYSTEM" -> "Hệ thống";
            default -> actorType;
        };
    }

    private static void appendCsvRow(StringBuilder csv, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escapeCsv(values[i]));
        }
        csv.append("\r\n");
    }

    static String escapeCsv(String value) {
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}