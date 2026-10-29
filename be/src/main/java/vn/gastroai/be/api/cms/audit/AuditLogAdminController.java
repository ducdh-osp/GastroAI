package vn.gastroai.be.api.cms.audit;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.audit.AuditAnomalyRuleService;
import vn.gastroai.be.application.audit.AuditLogExportService;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.domain.audit.AuditAction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** UC0053, UC0063, UC0064 - chi ADMIN xem/loc/xuat nhat ky va dat nguong canh bao bat thuong. */
@RestController
@RequestMapping("/api/v1/cms/audit-logs")
public class AuditLogAdminController {

    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType CSV = MediaType.parseMediaType("text/csv; charset=UTF-8");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final AuditLogService auditLogService;
    private final AuditAnomalyRuleService ruleService;
    private final AuditLogExportService exportService;

    public AuditLogAdminController(AuditLogService auditLogService,
                                   AuditAnomalyRuleService ruleService,
                                   AuditLogExportService exportService) {
        this.auditLogService = auditLogService;
        this.ruleService = ruleService;
        this.exportService = exportService;
    }

    @GetMapping
    public AuditLogPageResponse list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        return auditLogService.search(from, to, action, targetType, q, page, size);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate to,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "EXCEL") String format) {
        boolean csv = "CSV".equalsIgnoreCase(format);
        if (!csv && !"EXCEL".equalsIgnoreCase(format)) {
            throw new IllegalArgumentException("Định dạng xuất chỉ hỗ trợ CSV hoặc EXCEL");
        }

        List<AuditLogResponse> logs = auditLogService.findForExport(from, to, action, targetType, q);
        byte[] content = csv ? exportService.toCsv(logs) : exportService.toExcel(logs);

        auditLogService.record(AuditAction.AUDIT_LOG_EXPORT, "SYSTEM", null,
                "Xuất " + (csv ? "CSV" : "EXCEL") + " " + logs.size() + " dòng | "
                        + describeFilter(from, to, action, targetType, q));

        String filename = "GastroAI-AuditLog-"
                + LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(FILE_TIME)
                + (csv ? ".csv" : ".xlsx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(csv ? CSV : XLSX)
                .body(content);
    }

    @GetMapping("/rules")
    public List<AuditAnomalyRuleResponse> listRules() {
        return ruleService.list();
    }

    @PutMapping("/rules/{code}")
    public AuditAnomalyRuleResponse updateRule(
            @PathVariable String code,
            @Valid @RequestBody UpdateAuditAnomalyRuleRequest request) {
        return ruleService.update(code, request);
    }

    private String describeFilter(java.time.LocalDate from, java.time.LocalDate to, AuditAction action,
                                  String targetType, String q) {
        List<String> parts = new ArrayList<>();
        if (from != null) {
            parts.add("từ " + from);
        }
        if (to != null) {
            parts.add("đến " + to);
        }
        if (action != null) {
            parts.add("hành động " + action.name());
        }
        if (targetType != null && !targetType.isBlank()) {
            parts.add("đối tượng " + targetType.trim());
        }
        if (q != null && !q.isBlank()) {
            parts.add("từ khoá \"" + q.trim() + "\"");
        }
        return parts.isEmpty() ? "không lọc" : String.join(", ", parts);
    }
}