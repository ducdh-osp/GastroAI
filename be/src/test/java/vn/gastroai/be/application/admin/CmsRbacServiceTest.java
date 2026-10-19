package vn.gastroai.be.application.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.Doctor;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CmsRbacServiceTest {
    private final AdminRepository admins = mock(AdminRepository.class);
    private final DoctorRepository doctors = mock(DoctorRepository.class);
    private CmsRbacService service;

    @BeforeEach
    void setUp() {
        service = new CmsRbacService(admins, doctors);
    }

    @Test
    void assignsContentEditorToCmsAdminAccount() {
        Admin admin = new Admin();
        admin.setId(9L);
        admin.setEmail("editor@gastroai.vn");
        when(admins.findByIdForUpdate(9L)).thenReturn(Optional.of(admin));

        var result = service.assignRole(CmsAccountType.ADMIN, 9L, CmsRole.CONTENT_EDITOR);

        assertEquals(CmsRole.CONTENT_EDITOR, admin.getRole());
        assertEquals(CmsRole.CONTENT_EDITOR, result.role());
        verify(admins).findByIdForUpdate(9L);
    }

    @Test
    void rejectsAssigningContentEditorToDoctorAccount() {
        assertEquals("Tai khoan Bac si phai giu vai tro DOCTOR",
                assertThrows(IllegalArgumentException.class,
                        () -> service.assignRole(CmsAccountType.DOCTOR, 9L, CmsRole.CONTENT_EDITOR)).getMessage());
        verifyNoInteractions(doctors);
    }

    @Test
    void escapesLikeWildcardsBeforeSearchingAccounts() {
        String escaped = "100!%!_!\\!!";
        when(admins.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                escaped, escaped, org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(Page.empty());
        when(doctors.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                escaped, escaped, org.springframework.data.domain.Pageable.unpaged()))
                .thenReturn(Page.empty());

        service.list("100%_\\!", PageRequest.of(0, 20));

        verify(admins).findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                escaped, escaped, org.springframework.data.domain.Pageable.unpaged());
        verify(doctors).findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                escaped, escaped, org.springframework.data.domain.Pageable.unpaged());
    }
}
