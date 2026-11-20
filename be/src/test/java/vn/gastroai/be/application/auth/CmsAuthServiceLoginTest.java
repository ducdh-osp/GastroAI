package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.gastroai.be.application.commands.CmsLoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.AdminLoginHistory;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.domain.auth.LoginOutcome;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminPasswordHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CmsAuthServiceLoginTest {

    @Mock private AdminRepository adminRepository;
    @Mock private AdminLoginHistoryRepository loginHistoryRepository;
    @Mock private AdminPasswordHistoryRepository passwordHistoryRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CmsAuthService service;
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        // Toi da 5 lan sai, khoa 10 phut - giong cau hinh mac dinh.
        service = new CmsAuthService(adminRepository, loginHistoryRepository, passwordHistoryRepository, passwordEncoder,
                new LoginSecurityPolicy(5, 10, 90), new ClientRequestInfoResolver(""));
    }

    private Admin editor() {
        Admin account = new Admin();
        account.setId(3L);
        account.setEmail("bientap@gastroai.vn");
        account.setFullName("Biên tập");
        account.setPasswordHash("hash");
        account.setRole(CmsRole.CONTENT_EDITOR);
        account.setSessionVersion(2);
        return account;
    }

    @Test
    void successfulLoginTakesRoleFromAccountAndStoresItInNewSession() {
        Admin account = editor();
        account.setFailedLoginAttempts(3);
        HttpSession oldSession = request.getSession(true);
        when(adminRepository.findByEmailForUpdate("bientap@gastroai.vn")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("dung", "hash")).thenReturn(true);

        CmsAuthResult result = service.login(new CmsLoginCommand("bientap@gastroai.vn", "dung"), request);

        assertEquals("CONTENT_EDITOR", result.userType());
        assertEquals(0, account.getFailedLoginAttempts());
        HttpSession session = request.getSession(false);
        assertNotNull(session);
        assertTrue(((MockHttpSession) oldSession).isInvalid());
        assertEquals(3L, session.getAttribute(CmsAuthService.AUTH_USER_ID));
        assertEquals("CONTENT_EDITOR", session.getAttribute(CmsAuthService.AUTH_USER_TYPE));
        assertEquals(2, session.getAttribute(CmsAuthService.AUTH_SESSION_VERSION));
        assertEquals(LoginOutcome.SUCCESS, savedHistory().getOutcome());
    }

    @Test
    void unknownEmailStillRunsBcryptAndGivesSameError() {
        when(adminRepository.findByEmailForUpdate("khongco@gastroai.vn")).thenReturn(Optional.empty());

        BadCredentialsException error = assertThrows(BadCredentialsException.class,
                () -> service.login(new CmsLoginCommand("khongco@gastroai.vn", "abc"), request));

        assertEquals("Email hoặc mật khẩu không đúng", error.getMessage());
        verify(passwordEncoder).matches(eq("abc"), anyString());
        verify(loginHistoryRepository, never()).save(any());
        assertNull(request.getSession(false));
    }

    @Test
    void wrongPasswordCountsFailureWithSameError() {
        Admin account = editor();
        when(adminRepository.findByEmailForUpdate("bientap@gastroai.vn")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("sai", "hash")).thenReturn(false);

        BadCredentialsException error = assertThrows(BadCredentialsException.class,
                () -> service.login(new CmsLoginCommand("bientap@gastroai.vn", "sai"), request));

        assertEquals("Email hoặc mật khẩu không đúng", error.getMessage());
        assertEquals(1, account.getFailedLoginAttempts());
        assertNull(account.getLockedUntil());
        assertEquals(LoginOutcome.FAILURE, savedHistory().getOutcome());
        assertNull(request.getSession(false));
    }

    @Test
    void fifthWrongPasswordLocksAccount() {
        Admin account = editor();
        account.setFailedLoginAttempts(4);
        when(adminRepository.findByEmailForUpdate("bientap@gastroai.vn")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("sai", "hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> service.login(new CmsLoginCommand("bientap@gastroai.vn", "sai"), request));

        assertEquals(5, account.getFailedLoginAttempts());
        assertNotNull(account.getLockedUntil());
        assertEquals("Vượt quá số lần đăng nhập thất bại, tài khoản bị khóa", savedHistory().getFailureReason());
    }

    @Test
    void lockedAccountIsBlockedWithoutCheckingPassword() {
        Admin account = editor();
        account.setLockedUntil(Instant.now().plusSeconds(300));
        when(adminRepository.findByEmailForUpdate("bientap@gastroai.vn")).thenReturn(Optional.of(account));

        assertThrows(AccountLockedException.class,
                () -> service.login(new CmsLoginCommand("bientap@gastroai.vn", "dung"), request));

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        assertEquals(LoginOutcome.BLOCKED, savedHistory().getOutcome());
    }

    @Test
    void sessionIsCurrentOnlyWhileVersionMatches() {
        Admin account = editor();
        MockHttpSession session = new MockHttpSession();

        assertFalse(CmsAuthService.isCurrentSession(account, session));

        session.setAttribute(CmsAuthService.AUTH_SESSION_VERSION, 2);
        assertTrue(CmsAuthService.isCurrentSession(account, session));

        account.setSessionVersion(3);
        assertFalse(CmsAuthService.isCurrentSession(account, session));
    }

    @Test
    void sessionUserIdIsNullWithoutLogin() {
        MockHttpSession session = new MockHttpSession();

        assertNull(CmsAuthService.sessionUserId(null));
        assertNull(CmsAuthService.sessionUserId(session));

        session.setAttribute(CmsAuthService.AUTH_USER_ID, 3L);
        assertEquals(3L, CmsAuthService.sessionUserId(session));
    }

    private AdminLoginHistory savedHistory() {
        ArgumentCaptor<AdminLoginHistory> captor = ArgumentCaptor.forClass(AdminLoginHistory.class);
        verify(loginHistoryRepository).save(captor.capture());
        return captor.getValue();
    }
}