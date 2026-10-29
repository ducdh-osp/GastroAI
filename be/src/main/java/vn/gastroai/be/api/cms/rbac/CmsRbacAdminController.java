package vn.gastroai.be.api.cms.rbac;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.admin.CmsRbacService;
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.application.audit.AuditLogService;

/** UC0047 - chi ADMIN duoc xem va cap nhat vai tro CMS. */
@RestController
@RequestMapping("/api/v1/cms/rbac/accounts")
public class CmsRbacAdminController {
    private final CmsRbacService service;
    private final AuditLogService auditLogService;

    public CmsRbacAdminController(CmsRbacService service, AuditLogService auditLogService) {
        this.service = service;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public CmsAccountPageResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return service.list(q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PutMapping("/{accountType}/{accountId}/role")
    public CmsAccountResponse assignRole(
            @PathVariable CmsAccountType accountType,
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateCmsAccountRoleRequest request) {
        CmsAccountResponse response = service.assignRole(accountType, accountId, request.role());
        auditLogService.record(AuditAction.RBAC_ROLE_CHANGE, accountType.name(), accountId,
                "Vai trò mới: " + request.role());
        return response;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
    }
}
