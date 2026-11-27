package vn.gastroai.be.application.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.rbac.CmsAccountPageResponse;
import vn.gastroai.be.api.cms.rbac.CmsAccountResponse;
import vn.gastroai.be.api.cms.rbac.CreateCmsAccountRequest;
import vn.gastroai.be.application.auth.PasswordPolicy;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.SearchQueryEscaper;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.time.Instant;

/** UC0047/UC0077 - RBAC va tao tai khoan cho cac tai khoan CMS. */
@Service
public class CmsRbacService {
    private final AdminRepository admins;
    private final PasswordEncoder passwordEncoder;

    public CmsRbacService(AdminRepository admins, PasswordEncoder passwordEncoder) {
        this.admins = admins;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public CmsAccountPageResponse list(String query, Pageable pageable) {
        String normalized = query == null ? "" : SearchQueryEscaper.escapeLike(query.trim());
        Page<CmsAccountResponse> page = admins
                .findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(normalized, normalized, pageable)
                .map(this::from);
        return new CmsAccountPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional("mysqlTransactionManager")
    public CmsAccountResponse assignRole(Long accountId, CmsRole role) {
        Admin admin = admins.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan CMS"));
        admin.setRole(role);
        return from(admin);
    }

    /** SCR-AD-14 - chi tao o bang admins; 4 vai tro moi (SCR-AD-03 v1.2) deu nam o bang nay. */
    @Transactional("mysqlTransactionManager")
    public CmsAccountResponse createAccount(CreateCmsAccountRequest request) {
        if (admins.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email nay da co tai khoan CMS");
        }
        PasswordPolicy.validate(request.password());

        Admin admin = new Admin();
        admin.setEmail(request.email());
        admin.setFullName(request.fullName());
        admin.setPasswordHash(passwordEncoder.encode(request.password()));
        admin.setRole(request.role());
        admins.save(admin);
        return from(admin);
    }

    /** SCR-AD-14 Section 10 / SCR-AD-03: khoa tai khoan nhan su CMS - chi bang admins. */
    @Transactional("mysqlTransactionManager")
    public CmsAccountResponse lock(Long accountId) {
        Admin admin = admins.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan CMS"));
        if (!admin.isAdminLocked()) {
            admin.setAdminLockedAt(Instant.now());
            // Thu hoi ngay phien dang nhap hien tai, giong het AdminPatientManagementService.lock().
            admin.setSessionVersion(admin.getSessionVersion() + 1);
        }
        admins.save(admin);
        return from(admin);
    }

    @Transactional("mysqlTransactionManager")
    public CmsAccountResponse unlock(Long accountId) {
        Admin admin = admins.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan CMS"));
        admin.setAdminLockedAt(null);
        admins.save(admin);
        return from(admin);
    }

    private CmsAccountResponse from(Admin admin) {
        return new CmsAccountResponse(admin.getId(), admin.getEmail(),
                admin.getFullName(), admin.getRole(), admin.getCreatedAt(), admin.isAdminLocked());
    }
}
