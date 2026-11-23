package vn.gastroai.be.application.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.domain.admin.Doctor;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** UC0047/UC0077 - RBAC va tao tai khoan cho cac tai khoan CMS. */
@Service
public class CmsRbacService {
    private final AdminRepository admins;
    private final DoctorRepository doctors;
    private final PasswordEncoder passwordEncoder;

    public CmsRbacService(AdminRepository admins, DoctorRepository doctors, PasswordEncoder passwordEncoder) {
        this.admins = admins;
        this.doctors = doctors;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hai loai account nam o hai bang/DB mapping tach biet nen khong the JOIN hay Page chung o DB.
     * So luong tai khoan CMS nho; gop, sap xep va phan trang trong service giu API nhat quan.
     */
    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public CmsAccountPageResponse list(String query, Pageable pageable) {
        String normalized = query == null ? "" : SearchQueryEscaper.escapeLike(query.trim());
        List<CmsAccountResponse> accounts = new ArrayList<>();
        admins.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(normalized, normalized,
                        org.springframework.data.domain.Pageable.unpaged())
                .forEach(admin -> accounts.add(from(admin)));
        doctors.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(normalized, normalized,
                        org.springframework.data.domain.Pageable.unpaged())
                .forEach(doctor -> accounts.add(from(doctor)));

        accounts.sort(Comparator.comparing(CmsAccountResponse::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        int fromIndex = (int) Math.min(pageable.getOffset(), accounts.size());
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), accounts.size());
        Page<CmsAccountResponse> page = new PageImpl<>(accounts.subList(fromIndex, toIndex), pageable, accounts.size());
        return new CmsAccountPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    @Transactional("mysqlTransactionManager")
    public CmsAccountResponse assignRole(CmsAccountType accountType, Long accountId, CmsRole role) {
        if (accountType == CmsAccountType.ADMIN) {
            if (role == CmsRole.DOCTOR) {
                throw new IllegalArgumentException("Tai khoan tren bang admins khong the gan vai tro DOCTOR (da ngung dung)");
            }
            Admin admin = admins.findByIdForUpdate(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan CMS"));
            admin.setRole(role);
            return from(admin);
        }

        if (role != CmsRole.DOCTOR) {
            throw new IllegalArgumentException("Tai khoan Bac si phai giu vai tro DOCTOR");
        }
        Doctor doctor = doctors.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan CMS"));
        doctor.setRole(role);
        return from(doctor);
    }

    /** SCR-AD-14 - chi tao o bang admins; 4 vai tro moi (SCR-AD-03 v1.2) deu nam o bang nay. */
    @Transactional("mysqlTransactionManager")
    public CmsAccountResponse createAccount(CreateCmsAccountRequest request) {
        if (request.role() == CmsRole.DOCTOR) {
            throw new IllegalArgumentException("Khong the tao tai khoan moi voi vai tro DOCTOR (da ngung dung)");
        }
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
            admin.setAdminLocked(true);
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
        admin.setAdminLocked(false);
        admin.setAdminLockedAt(null);
        admins.save(admin);
        return from(admin);
    }

    private CmsAccountResponse from(Admin admin) {
        return new CmsAccountResponse(admin.getId(), CmsAccountType.ADMIN, admin.getEmail(),
                admin.getFullName(), admin.getRole(), admin.getCreatedAt(), admin.isAdminLocked());
    }

    private CmsAccountResponse from(Doctor doctor) {
        return new CmsAccountResponse(doctor.getId(), CmsAccountType.DOCTOR, doctor.getEmail(),
                doctor.getFullName(), doctor.getRole(), doctor.getCreatedAt(), false);
    }
}
