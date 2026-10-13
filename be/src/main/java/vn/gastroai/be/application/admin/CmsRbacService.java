package vn.gastroai.be.application.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.api.cms.rbac.CmsAccountPageResponse;
import vn.gastroai.be.api.cms.rbac.CmsAccountResponse;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.domain.admin.Doctor;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** UC0047 - RBAC cho cac tai khoan CMS. */
@Service
public class CmsRbacService {
    private final AdminRepository admins;
    private final DoctorRepository doctors;

    public CmsRbacService(AdminRepository admins, DoctorRepository doctors) {
        this.admins = admins;
        this.doctors = doctors;
    }

    /**
     * Hai loai account nam o hai bang/DB mapping tach biet nen khong the JOIN hay Page chung o DB.
     * So luong tai khoan CMS nho; gop, sap xep va phan trang trong service giu API nhat quan.
     */
    @Transactional(value = "mysqlTransactionManager", readOnly = true)
    public CmsAccountPageResponse list(String query, Pageable pageable) {
        String normalized = query == null ? "" : query.trim();
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
            if (role != CmsRole.ADMIN) {
                throw new IllegalArgumentException("Tai khoan Admin phai giu vai tro ADMIN");
            }
            return from(admins.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan CMS")));
        }

        if (role != CmsRole.DOCTOR && role != CmsRole.CONTENT_EDITOR) {
            throw new IllegalArgumentException("Tai khoan nay chi co the gan vai tro DOCTOR hoac CONTENT_EDITOR");
        }
        Doctor doctor = doctors.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tai khoan CMS"));
        doctor.setRole(role);
        return from(doctor);
    }

    private CmsAccountResponse from(Admin admin) {
        return new CmsAccountResponse(admin.getId(), CmsAccountType.ADMIN, admin.getEmail(),
                admin.getFullName(), admin.getRole(), admin.getCreatedAt());
    }

    private CmsAccountResponse from(Doctor doctor) {
        return new CmsAccountResponse(doctor.getId(), CmsAccountType.DOCTOR, doctor.getEmail(),
                doctor.getFullName(), doctor.getRole(), doctor.getCreatedAt());
    }
}
