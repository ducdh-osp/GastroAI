package vn.gastroai.be.application.cms.dashboard;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import vn.gastroai.be.api.cms.dashboard.*;
import vn.gastroai.be.infrastructure.pdf.PdfDocumentBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardExportService {

    public byte[] exportPdf(DashboardStatsResponse stats) {
        PdfDocumentBuilder builder = new PdfDocumentBuilder();

        builder.title("BÁO CÁO THỐNG KÊ HOẠT ĐỘNG GASTROAI")
                .subtitle("Giai đoạn: " + stats.fromDate() + " đến " + stats.toDate()
                        + "   |   Ngày xuất: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // 1. Tổng quan
        builder.section("1. CÁC CHỈ SỐ HOẠT ĐỘNG TỔNG QUAN");
        String[] summaryHeaders = {"Chỉ số", "Số lượng", "Chỉ số", "Số lượng"};
        float[] summaryWidths = {35f, 15f, 35f, 15f};
        List<String[]> summaryRows = new ArrayList<>();
        summaryRows.add(new String[]{
                "Tổng số bệnh nhân", String.valueOf(stats.summary().totalPatients()),
                "Tổng số phiên chat", String.valueOf(stats.summary().totalSessions())
        });
        summaryRows.add(new String[]{
                "Tổng số tin nhắn chat", String.valueOf(stats.summary().totalMessages()),
                "Tin nhắn trong ngày", String.valueOf(stats.summary().todayMessages())
        });
        summaryRows.add(new String[]{
                "Tổng số ca đánh giá triệu chứng", String.valueOf(stats.summary().totalAssessments()),
                "Ca đánh giá khẩn cấp", String.valueOf(stats.summary().emergencyAssessments())
        });
        summaryRows.add(new String[]{
                "Tổng số cảnh báo Triage", String.valueOf(stats.summary().totalAlerts()),
                "Cảnh báo mới (chờ xử lý)", String.valueOf(stats.summary().newAlerts())
        });
        summaryRows.add(new String[]{
                "Đánh giá AI hữu ích", String.valueOf(stats.summary().helpfulRatings()),
                "Tỷ lệ hài lòng người dùng", stats.summary().satisfactionRate() + "%"
        });
        builder.table(summaryHeaders, summaryWidths, summaryRows);

        // 2. Xu hướng chat
        builder.section("2. LƯỢT CHAT VÀ PHẢN HỒI THEO NGÀY");
        String[] chatHeaders = {"Ngày", "Tổng tin nhắn", "Bệnh nhân gửi", "Phản hồi AI", "Cảnh báo khẩn cấp"};
        float[] chatWidths = {24f, 19f, 19f, 19f, 19f};
        List<String[]> chatRows = new ArrayList<>();
        for (DailyChatMetric d : stats.dailyChats()) {
            chatRows.add(new String[]{
                    d.day(),
                    String.valueOf(d.totalMessages()),
                    String.valueOf(d.patientMessages()),
                    String.valueOf(d.botMessages()),
                    String.valueOf(d.emergencyMessages())
            });
        }
        builder.table(chatHeaders, chatWidths, chatRows);

        // 3. Triệu chứng phổ biến
        builder.section("3. PHÂN BỐ TRIỆU CHỨNG TIÊU HÓA PHỔ BIẾN");
        String[] symptomHeaders = {"STT", "Triệu chứng tiêu hóa", "Mã", "Số ca", "Tỷ lệ (%)"};
        float[] symptomWidths = {10f, 40f, 25f, 12f, 13f};
        List<String[]> symptomRows = new ArrayList<>();
        int sIdx = 1;
        for (SymptomMetric s : stats.topSymptoms()) {
            symptomRows.add(new String[]{
                    String.valueOf(sIdx++),
                    s.symptomName(),
                    s.symptomCode(),
                    String.valueOf(s.totalCount()),
                    s.percentage() + "%"
            });
        }
        builder.table(symptomHeaders, symptomWidths, symptomRows);

        // 4. An toàn Triage
        builder.section("4. GIÁM SÁT AN TOÀN Y KHOA VÀ CẢNH BÁO TRIAGE");
        String[] triageHeaders = {"Nhóm dấu hiệu cảnh báo", "Số ca kích hoạt", "Tỷ lệ (%)"};
        float[] triageWidths = {60f, 20f, 20f};
        List<String[]> triageRows = new ArrayList<>();
        for (TriageGroupMetric g : stats.triageGroups()) {
            triageRows.add(new String[]{
                    g.groupName(),
                    String.valueOf(g.count()),
                    g.percentage() + "%"
            });
        }
        builder.table(triageHeaders, triageWidths, triageRows);

        builder.note("Báo cáo được trích xuất tự động từ Phân hệ Quản trị (CMS) GastroAI. "
                + "Dữ liệu phục vụ đánh giá năng lực hệ thống và giám sát an toàn y khoa.");

        return builder.build();
    }

    public byte[] exportExcel(DashboardStatsResponse stats) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);

            // Sheet 1: Tổng quan
            Sheet sheet1 = workbook.createSheet("Tổng quan");
            createTitleRow(sheet1, titleStyle, "BÁO CÁO THỐNG KÊ TỔNG QUAN GASTROAI", 0);
            createSubRow(sheet1, "Khoảng thời gian: " + stats.fromDate() + " đến " + stats.toDate(), 1);

            Row hRow1 = sheet1.createRow(3);
            String[] s1Cols = {"STT", "Chỉ số hoạt động", "Giá trị", "Ghi chú"};
            for (int i = 0; i < s1Cols.length; i++) {
                Cell c = hRow1.createCell(i);
                c.setCellValue(s1Cols[i]);
                c.setCellStyle(headerStyle);
            }

            Object[][] kpis = {
                    {1, "Tổng số bệnh nhân đăng ký", stats.summary().totalPatients(), "Người dùng"},
                    {2, "Tổng số phiên chat", stats.summary().totalSessions(), "Phiên"},
                    {3, "Tổng số tin nhắn chat", stats.summary().totalMessages(), "Tin nhắn"},
                    {4, "Tin nhắn hôm nay", stats.summary().todayMessages(), "Tin nhắn"},
                    {5, "Tổng số ca đánh giá triệu chứng", stats.summary().totalAssessments(), "Lượt đánh giá"},
                    {6, "Số ca đánh giá khẩn cấp", stats.summary().emergencyAssessments(), "Lượt khẩn cấp"},
                    {7, "Tổng số cảnh báo Triage kích hoạt", stats.summary().totalAlerts(), "Cảnh báo"},
                    {8, "Cảnh báo Triage mới (chờ xử lý)", stats.summary().newAlerts(), "Cảnh báo"},
                    {9, "Cảnh báo Triage đang xử lý", stats.summary().inProgressAlerts(), "Cảnh báo"},
                    {10, "Cảnh báo Triage đã xử lý", stats.summary().resolvedAlerts(), "Cảnh báo"},
                    {11, "Đánh giá phản hồi AI hữu ích", stats.summary().helpfulRatings(), "Lượt thích"},
                    {12, "Đánh giá phản hồi AI không hữu ích", stats.summary().unhelpfulRatings(), "Lượt không thích"},
                    {13, "Tỷ lệ hài lòng phản hồi AI", stats.summary().satisfactionRate() + "%", "Phần trăm"}
            };

            int rIdx = 4;
            for (Object[] kpi : kpis) {
                Row r = sheet1.createRow(rIdx++);
                for (int c = 0; c < kpi.length; c++) {
                    Cell cell = r.createCell(c);
                    cell.setCellStyle(dataStyle);
                    if (kpi[c] instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(kpi[c]));
                    }
                }
            }
            autoSize(sheet1, s1Cols.length);

            // Sheet 2: Lượt chat theo ngày
            Sheet sheet2 = workbook.createSheet("Lượt chat theo ngày");
            createTitleRow(sheet2, titleStyle, "HOẠT ĐỘNG TƯƠNG TÁC CHAT THEO NGÀY", 0);
            createSubRow(sheet2, "Giai đoạn: " + stats.fromDate() + " đến " + stats.toDate(), 1);

            Row hRow2 = sheet2.createRow(3);
            String[] s2Cols = {"Ngày", "Tổng số tin nhắn", "Bệnh nhân gửi", "Phản hồi AI", "Cảnh báo khẩn cấp"};
            for (int i = 0; i < s2Cols.length; i++) {
                Cell c = hRow2.createCell(i);
                c.setCellValue(s2Cols[i]);
                c.setCellStyle(headerStyle);
            }

            int r2 = 4;
            for (DailyChatMetric d : stats.dailyChats()) {
                Row r = sheet2.createRow(r2++);
                createNumericCell(r, 0, d.day(), dataStyle, false);
                createNumericCell(r, 1, d.totalMessages(), dataStyle, true);
                createNumericCell(r, 2, d.patientMessages(), dataStyle, true);
                createNumericCell(r, 3, d.botMessages(), dataStyle, true);
                createNumericCell(r, 4, d.emergencyMessages(), dataStyle, true);
            }
            autoSize(sheet2, s2Cols.length);

            // Sheet 3: Triệu chứng phổ biến
            Sheet sheet3 = workbook.createSheet("Triệu chứng phổ biến");
            createTitleRow(sheet3, titleStyle, "PHÂN BỐ TRIỆU CHỨNG TIÊU HÓA", 0);
            createSubRow(sheet3, "Dữ liệu thu thập từ các lần đánh giá triệu chứng của bệnh nhân", 1);

            Row hRow3 = sheet3.createRow(3);
            String[] s3Cols = {"STT", "Triệu chứng", "Mã", "Số ca ghi nhận", "Số ca khẩn cấp", "Tỷ lệ (%)"};
            for (int i = 0; i < s3Cols.length; i++) {
                Cell c = hRow3.createCell(i);
                c.setCellValue(s3Cols[i]);
                c.setCellStyle(headerStyle);
            }

            int r3 = 4;
            int idxS = 1;
            for (SymptomMetric s : stats.topSymptoms()) {
                Row r = sheet3.createRow(r3++);
                createNumericCell(r, 0, idxS++, dataStyle, true);
                createNumericCell(r, 1, s.symptomName(), dataStyle, false);
                createNumericCell(r, 2, s.symptomCode(), dataStyle, false);
                createNumericCell(r, 3, s.totalCount(), dataStyle, true);
                createNumericCell(r, 4, s.emergencyCount(), dataStyle, true);
                createNumericCell(r, 5, s.percentage() + "%", dataStyle, false);
            }
            autoSize(sheet3, s3Cols.length);

            // Sheet 4: Cảnh báo Triage
            Sheet sheet4 = workbook.createSheet("Cảnh báo Triage");
            createTitleRow(sheet4, titleStyle, "THỐNG KÊ CẢNH BÁO AN TOÀN Y KHOA (TRIAGE)", 0);
            createSubRow(sheet4, "Các trường hợp phát hiện dấu hiệu nguy hiểm và tình trạng can thiệp", 1);

            Row hRow4 = sheet4.createRow(3);
            String[] s4Cols = {"Nhóm dấu hiệu cảnh báo Triage", "Mã nhóm", "Số ca kích hoạt", "Tỷ lệ (%)"};
            for (int i = 0; i < s4Cols.length; i++) {
                Cell c = hRow4.createCell(i);
                c.setCellValue(s4Cols[i]);
                c.setCellStyle(headerStyle);
            }

            int r4 = 4;
            for (TriageGroupMetric g : stats.triageGroups()) {
                Row r = sheet4.createRow(r4++);
                createNumericCell(r, 0, g.groupName(), dataStyle, false);
                createNumericCell(r, 1, g.groupCode(), dataStyle, false);
                createNumericCell(r, 2, g.count(), dataStyle, true);
                createNumericCell(r, 3, g.percentage() + "%", dataStyle, false);
            }
            autoSize(sheet4, s4Cols.length);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Không tạo được file Excel báo cáo", e);
        }
    }

    private void createTitleRow(Sheet sheet, CellStyle style, String text, int rowIdx) {
        Row r = sheet.createRow(rowIdx);
        Cell c = r.createCell(0);
        c.setCellValue(text);
        c.setCellStyle(style);
    }

    private void createSubRow(Sheet sheet, String text, int rowIdx) {
        Row r = sheet.createRow(rowIdx);
        Cell c = r.createCell(0);
        c.setCellValue(text);
    }

    private void createNumericCell(Row row, int col, Object val, CellStyle style, boolean isNumeric) {
        Cell c = row.createCell(col);
        c.setCellStyle(style);
        if (isNumeric && val instanceof Number n) {
            c.setCellValue(n.doubleValue());
        } else {
            c.setCellValue(String.valueOf(val));
        }
    }

    private void autoSize(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth + 1000, 3500));
        }
    }
}
