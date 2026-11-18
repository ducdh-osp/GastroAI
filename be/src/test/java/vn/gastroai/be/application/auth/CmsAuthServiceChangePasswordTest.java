package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.AdminPasswordHistory;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminPasswordHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.time.Instant;
import java.util.List;
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

    private static final String REUSED_MESSAGE = "Mật khẩu mới không được trùng với mật khẩu đã từng sử dụng";

    @Mock private AdminRepository adminRepository;
    @Mock private AdminLoginHistoryRepository adminLoginHistoryRepository;
    @Mock private AdminPasswordHistoryRepository passwordHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CmsAuthService service;
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        // Toi da 5 lan sai, khoa 10 phut - giong cau hinh mac dinh.
        service = new CmsAuthService(adminRepository, adminLoginHistoryRepository, passwordHistoryRepository,
                passwordEncoder, new LoginSecurityPolicy(5, 10, 90),
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
    void changesPasswordRemembersOldOneAndReturnsNewSessionVersion() {
        Admin admin = admin();
        admin.setFailedLoginAttempts(2);
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("matkhau-cu", "hash-cu")).thenReturn(true);
        when(passwordEncoder.matches("Gastro#Ai2026", "hash-cu")).thenReturn(false);
        when(passwordEncoder.encode("Gastro#Ai2026")).thenReturn("hash-moi");

        int sessionVersion = service.changePassword(1L, "matkhau-cu", "Gastro#Ai2026", request);

        assertEquals(1, sessionVersion);
        assertEquals(1, admin.getSessionVersion());
        assertEquals("hash-moi", admin.getPasswordHash());
        assertEquals(0, admin.getFailedLoginAttempts());
        ArgumentCaptor<AdminPasswordHistory> saved = ArgumentCaptor.forClass(AdminPasswordHistory.class);
        verify(passwordHistoryRepository).save(saved.capture());
        assertEquals("hash-cu", saved.getValue().getPasswordHash());
    }

    @Test
    void wrongCurrentPasswordCountsFailureAndRejects() {
        Admin admin = admin();
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("sai", "hash-cu")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(1L, "sai", "Gastro#Ai2026", request));

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
                () -> service.changePassword(1L, "sai", "Gastro#Ai2026", request));

        assertEquals(5, admin.getFailedLoginAttempts());
        assertNotNull(admin.getLockedUntil());
    }

    @Test
    void lockedAccountCannotChangePasswordEvenWithRightPassword() {
        Admin admin = admin();
        admin.setLockedUntil(Instant.now().plusSeconds(300));
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));

        assertThrows(AccountLockedException.class,
                () -> service.changePassword(1L, "matkhau-cu", "Gastro#Ai2026", request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        assertEquals(0, admin.getSessionVersion());
    }

    @Test
    void newPasswordSameAsCurrentIsRejected() {
        Admin admin = admin();
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("Matkhau#Cu1", "hash-cu")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(1L, "Matkhau#Cu1", "Matkhau#Cu1", request));

        assertEquals(REUSED_MESSAGE, ex.getMessage());
        verify(passwordEncoder, never()).encode(anyString());
        assertEquals(0, admin.getSessionVersion());
    }

    @Test
    void passwordUsedBeforeIsRejected() {
        Admin admin = admin();
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("matkhau-cu", "hash-cu")).thenReturn(true);
        when(passwordEncoder.matches("Gastro#Ai2025", "hash-cu")).thenReturn(false);
        when(passwordHistoryRepository.findPasswordHashesByAdminId(1L))
                .thenReturn(List.of("hash-nam-ngoai", "hash-thang-truoc"));
        when(passwordEncoder.matches("Gastro#Ai2025", "hash-nam-ngoai")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(1L, "matkhau-cu", "Gastro#Ai2025", request));

        assertEquals(REUSED_MESSAGE, ex.getMessage());
        assertEquals("hash-cu", admin.getPasswordHash());
        verify(passwordHistoryRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void weakNewPasswordIsRejectedWithSameRuleAsPatientPortal() {
        Admin admin = admin();
        admin.setFailedLoginAttempts(2);
        when(adminRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("matkhau-cu", "hash-cu")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(1L, "matkhau-cu", "yeu12345", request));

        assertEquals(PatientPasswordPolicy.REQUIREMENTS_MESSAGE, ex.getMessage());
        assertEquals("hash-cu", admin.getPasswordHash());
        assertEquals(2, admin.getFailedLoginAttempts());
        assertEquals(0, admin.getSessionVersion());
        verify(passwordEncoder, never()).encode(anyString());
    }
}