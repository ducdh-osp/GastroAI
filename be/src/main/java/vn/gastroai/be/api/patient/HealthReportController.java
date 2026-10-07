package vn.gastroai.be.api.patient;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.support.AuthenticatedRequest;
import vn.gastroai.be.application.patient.HealthReportService;

import java.security.Principal;
import java.time.LocalDate;

/** UC0019 - xuat nhat ky suc khoe ra PDF. */
@RestController
@RequestMapping("/api/v1/patient/health-report")
public class HealthReportController {

    private final HealthReportService healthReportService;

    public HealthReportController(HealthReportService healthReportService) {
        this.healthReportService = healthReportService;
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        byte[] pdf = healthReportService.export(patientId, from, to);

        // Ten file chi dung chu khong dau, de moi trinh duyet tai ve dung ten.
        String filename = "nhat-ky-suc-khoe_" + from + "_" + to + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                // Du lieu y te - khong cho trinh duyet hay proxy luu ban sao.
                .cacheControl(CacheControl.noStore())
                .body(pdf);
    }
}