package vn.gastroai.be.application.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        when(doctors.findById(9L)).thenReturn(Optional.of(doctor));

        var result = service.assignRole(CmsAccountType.DOCTOR, 9L, CmsRole.CONTENT_EDITOR);

        assertEquals(CmsRole.CONTENT_EDITOR, doctor.getRole());
        assertEquals(CmsRole.CONTENT_EDITOR, result.role());
    }

    @Test
    void rejectsEscalatingDoctorAccountToAdmin() {
        assertEquals("Tai khoan nay chi co the gan vai tro DOCTOR hoac CONTENT_EDITOR",
                assertThrows(IllegalArgumentException.class,
                        () -> service.assignRole(CmsAccountType.DOCTOR, 9L, CmsRole.ADMIN)).getMessage());
        verifyNoInteractions(doctors);
    }
}
