package vn.gastroai.be.application.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.admin.CmsRole;
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
    void assignsContentEditorToCmsDoctorAccount() {
        Doctor doctor = new Doctor();
        doctor.setId(9L);
        doctor.setEmail("editor@gastroai.vn");
        when(doctors.findByIdForUpdate(9L)).thenReturn(Optional.of(doctor));

        var result = service.assignRole(CmsAccountType.DOCTOR, 9L, CmsRole.CONTENT_EDITOR);

        assertEquals(CmsRole.CONTENT_EDITOR, doctor.getRole());
        assertEquals(CmsRole.CONTENT_EDITOR, result.role());
        verify(doctors).findByIdForUpdate(9L);
    }

    @Test
    void rejectsEscalatingDoctorAccountToAdmin() {
        assertEquals("Tai khoan nay chi co the gan vai tro DOCTOR hoac CONTENT_EDITOR",
                assertThrows(IllegalArgumentException.class,
                        () -> service.assignRole(CmsAccountType.DOCTOR, 9L, CmsRole.ADMIN)).getMessage());
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
