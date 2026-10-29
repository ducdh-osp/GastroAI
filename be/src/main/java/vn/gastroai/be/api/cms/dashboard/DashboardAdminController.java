package vn.gastroai.be.api.cms.dashboard;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.cms.dashboard.DashboardAdminService;
import vn.gastroai.be.application.cms.dashboard.DashboardExportService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/cms/dashboard")
public class DashboardAdminController {

    private final DashboardAdminService dashboardAdminService;
    private final DashboardExportService dashboardExportService;

    public DashboardAdminController(
            DashboardAdminService dashboardAdminService,
            DashboardExportService dashboardExportService) {
        this.dashboardAdminService = dashboardAdminService;
        this.dashboardExportService = dashboardExportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(
            @RequestParam(defaultValue = "30") int days) {
        DashboardStatsResponse stats = dashboardAdminService.getStats(days);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(defaultValue = "EXCEL") String format,
            @RequestParam(defaultValue = "30") int days) {
        DashboardStatsResponse stats = dashboardAdminService.getStats(days);
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        if ("PDF".equalsIgnoreCase(format)) {
            byte[] pdfBytes = dashboardExportService.exportPdf(stats);
            String filename = "GastroAI-Dashboard-Report-" + dateStr + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } else {
            byte[] excelBytes = dashboardExportService.exportExcel(stats);
            String filename = "GastroAI-Dashboard-Report-" + dateStr + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        }
    }
}
