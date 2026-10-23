package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.admin.Doctor;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CmsAuthServiceChangePasswordTest {

    @Mock private AdminRepository adminRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AdminLoginHistoryRepository adminLoginHistoryRepository;
    @Mock private DoctorLoginHistoryRepository doctorLoginHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CmsAuthService service;
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        // Toi da 5 lan sai, khoa 10 phut - giong cau hinh mac dinh.
        service = new CmsAuthService(adminRepository, doctorRepository, adminLoginHistoryRepository,
                doctorLoginHistoryRepository, passwordEncoder, new LoginSecurityPolicy(5, 10, 90),
                new ClientRequestInfoResolver(""));
    }

    private Admin admin() {
        Admin admin = new Admin();
        admin.setId(1L);
        admin.setEmail("admin@gastroai.vn");
        admin.setPasswordHash("hash-cu");
        return admin;
    }

    @Test
    void changesPasswordResetsCountersAndBumpsSessionVersion() {
        Admin admin = admin();
        admin.setFailedLoginAttempts(2);
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("matkhau-cu", "hash-cu")).thenReturn(true);
        when(passwordEncoder.matches("matkhau-moi", "hash-cu")).thenReturn(false);
        when(passwordEncoder.encode("matkhau-moi")).thenReturn("hash-moi");

        service.changePassword(CmsAccountType.ADMIN, 1L, "matkhau-cu", "matkhau-moi", request);

        assertEquals("hash-moi", admin.getPasswordHash());
        assertEquals(0, admin.getFailedLoginAttempts());
        assertEquals(1, admin.getSessionVersion());
    }

    @Test
    void wrongCurrentPasswordCountsFailureAndRejects() {
        Admin admin = admin();
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("sai", "hash-cu")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(CmsAccountType.ADMIN, 1L, "sai", "matkhau-moi", request));

        assertEquals("Mật khẩu hiện tại không đúng", ex.getMessage());
        assertEquals(1, admin.getFailedLoginAttempts());
        assertNull(admin.getLockedUntil());
        assertEquals("hash-cu", admin.getPasswordHash());
        verify(adminLoginHistoryRepository).save(any());
    }

    @Test
    void tooManyWrongAttemptsLocksAccount() {
        Admin admin = admin();
        admin.setFailedLoginAttempts(4);
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("sai", "hash-cu")).thenReturn(false);

        assertThrows(AccountLockedException.class,
                () -> service.changePassword(CmsAccountType.ADMIN, 1L, "sai", "matkhau-moi", request));

        assertEquals(5, admin.getFailedLoginAttempts());
        assertNotNull(admin.getLockedUntil());
    }

    @Test
    void lockedAccountCannotChangePasswordEvenWithRightPassword() {
        Admin admin = admin();
        admin.setLockedUntil(Instant.now().plusSeconds(300));
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));

        assertThrows(AccountLockedException.class,
                () -> service.changePassword(CmsAccountType.ADMIN, 1L, "matkhau-cu", "matkhau-moi", request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        assertEquals(0, admin.getSessionVersion());
    }

    @Test
    void newPasswordSameAsCurrentIsRejected() {
        Admin admin = admin();
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("matkhau-cu", "hash-cu")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(CmsAccountType.ADMIN, 1L, "matkhau-cu", "matkhau-cu", request));

        verify(passwordEncoder, never()).encode(anyString());
        assertEquals(0, admin.getSessionVersion());
    }

    @Test
    void doctorCanChangePassword() {
        Doctor doctor = new Doctor();
        doctor.setId(2L);
        doctor.setPasswordHash("hash-cu");
        when(doctorRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(doctor));
        when(passwordEncoder.matches("matkhau-cu", "hash-cu")).thenReturn(true);
        when(passwordEncoder.matches("matkhau-moi", "hash-cu")).thenReturn(false);
        when(passwordEncoder.encode("matkhau-moi")).thenReturn("hash-moi");

        service.changePassword(CmsAccountType.DOCTOR, 2L, "matkhau-cu", "matkhau-moi", request);

        assertEquals("hash-moi", doctor.getPasswordHash());
        assertEquals(1, doctor.getSessionVersion());
        verify(adminRepository, never()).findByIdForUpdate(any());
    }
}