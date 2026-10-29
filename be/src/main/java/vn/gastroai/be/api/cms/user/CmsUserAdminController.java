package vn.gastroai.be.api.cms.user;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.admin.AdminPatientManagementService;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.application.audit.AuditLogService;    

/** UC0046 - API chi danh cho ADMIN. */
@RestController
@RequestMapping("/api/v1/cms/users")
public class CmsUserAdminController {
    private final AdminPatientManagementService service;
    private final AuditLogService auditLogService;

    public CmsUserAdminController(AdminPatientManagementService service, AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public CmsPatientPageResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean locked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return CmsPatientPageResponse.from(service.list(q, locked,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PostMapping("/{patientId}/lock")
    public CmsPatientResponse lock(@PathVariable Long patientId) {
        CmsPatientResponse response = service.lock(patientId);
        auditLogService.record(AuditAction.PATIENT_LOCK, "PATIENT", patientId, null);
        return response;
    }

    @PostMapping("/{patientId}/unlock")
    public CmsPatientResponse unlock(@PathVariable Long patientId) {
        CmsPatientResponse response = service.unlock(patientId);
        auditLogService.record(AuditAction.PATIENT_UNLOCK, "PATIENT", patientId, null);
        return response;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
    }
}
