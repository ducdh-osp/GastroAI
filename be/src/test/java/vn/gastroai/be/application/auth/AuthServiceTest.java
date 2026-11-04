package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.application.notification.PatientNotificationSettingService;
import vn.gastroai.be.application.readmodel.AuthResult;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.auth.PatientLoginHistory;
import vn.gastroai.be.domain.auth.UserRole;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientPasswordHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final PatientRepository patients = mock(PatientRepository.class);
    private final PatientLoginHistoryRepository history = mock(PatientLoginHistoryRepository.class);
    private final PatientPasswordHistoryRepository passwordHistory = mock(PatientPasswordHistoryRepository.class);
    private final RevokedTokenRepository revoked = mock(RevokedTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final EmailVerificationService mail = mock(EmailVerificationService.class);
    private final AppNotificationService notifications = mock(AppNotificationService.class);
    private final PatientNotificationSettingService notificationSettings = mock(PatientNotificationSettingService.class);
    private final LoginSecurityPolicy policy = new LoginSecurityPolicy(3, 10, 90);
    private AuthService service;

    @BeforeEach
    void setUp() {
        when(notificationSettings.isSecurityAlertEnabled(anyLong())).thenReturn(true);
        when(passwordHistory.findPasswordHashesByPatientId(anyLong())).thenReturn(List.of());
        service = new AuthService(patients, history, passwordHistory, revoked, encoder, jwt, mail, policy, notifications, notificationSettings);
    }

    @Test
    void registerStoresHashAndSendsVerificationMail() {
        when(patients.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(encoder.encode("Password1!")).thenReturn("bcrypt");
        when(patients.saveAndFlush(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            patient.setId(7L);
            return patient;
        });

        service.register(" User@Example.com ", "Password1!", " Nguyen Van A ");

        verify(patients).saveAndFlush(argThat(patient ->
                patient.getId().equals(7L)
                        && patient.getEmail().equals("user@example.com")
                        && patient.getPasswordHash().equals("bcrypt")
                        && patient.getFullName().equals("Nguyen Van A")
                        && patient.getRole() == UserRole.PATIENT
                        && !patient.isEmailVerified()
                        && patient.getVerificationTokenHash() != null
                        && patient.getVerificationTokenExpiresAt().isAfter(Instant.now())));
        verify(mail).send(argThat(patient -> patient.getEmail().equals("user@example.com")), anyString());
    }

    @Test
    void registerRejectsPasswordOverBcryptUtf8LimitBeforeDatabaseCall() {
        String password = "A1!" + "ă".repeat(35); // 73 UTF-8 bytes

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.register("user@example.com", password, "User"));

        assertEquals("Mật khẩu tối đa 72 byte UTF-8", exception.getMessage());
        verifyNoInteractions(patients, encoder, mail);
    }

    @Test
    void registerAccepts72Utf8BytesEvenWhenCharacterCountIsBelowLimit() {
        String password = "A1!" + "ă".repeat(34) + "a"; // 72 UTF-8 bytes
        when(patients.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(password)).thenReturn("bcrypt");
        when(patients.saveAndFlush(any(Patient.class))).thenAnswer(invocation ->
                (Patient) invocation.getArgument(0));

        service.register("user@example.com", password, "User");

        verify(encoder).encode(password);
        verify(mail).send(any(Patient.class), anyString());
    }

    @Test
    void verifyConsumesValidTokenAndRejectsItOnSecondUse() {
        Patient patient = patient();
        patient.setVerificationTokenHash(hash("verify-token"));
        patient.setVerificationTokenExpiresAt(Instant.now().plusSeconds(60));
        when(patients.findByVerificationTokenHash(hash("verify-token")))
                .thenReturn(Optional.of(patient), Optional.empty());

        service.verify("verify-token");

        assertTrue(patient.isEmailVerified());
        assertNull(patient.getVerificationTokenHash());
        assertNull(patient.getVerificationTokenExpiresAt());
        assertThrows(IllegalArgumentException.class, () -> service.verify("verify-token"));
    }

    @Test
    void verifyRejectsExpiredToken() {
        Patient patient = patient();
        patient.setVerificationTokenHash(hash("expired"));
        patient.setVerificationTokenExpiresAt(Instant.now().minusSeconds(1));
        when(patients.findByVerificationTokenHash(hash("expired"))).thenReturn(Optional.of(patient));

        assertEquals("Token xac thuc da het han",
                assertThrows(IllegalArgumentException.class, () -> service.verify("expired")).getMessage());
        assertFalse(patient.isEmailVerified());
    }

    @Test
    void resetPasswordChangesPasswordInvalidatesTokenAndResetsLockState() {
        Patient patient = patient();
        patient.setPasswordResetTokenHash(hash("reset-token"));
        patient.setPasswordResetTokenExpiresAt(Instant.now().plusSeconds(60));
        patient.setFailedLoginAttempts(3);
        patient.setLockedUntil(Instant.now().plusSeconds(60));
        patient.setTokenVersion(4);
        when(patients.findByPasswordResetTokenHash(hash("reset-token"))).thenReturn(Optional.of(patient));
        when(encoder.encode("NewPassword1!")).thenReturn("new-bcrypt");

        service.resetPassword("reset-token", "NewPassword1!");

        assertEquals("new-bcrypt", patient.getPasswordHash());
        assertNull(patient.getPasswordResetTokenHash());
        assertNull(patient.getPasswordResetTokenExpiresAt());
        assertEquals(0, patient.getFailedLoginAttempts());
        assertNull(patient.getLockedUntil());
        assertEquals(5, patient.getTokenVersion());
        verify(passwordHistory).save(argThat(item -> item.getPasswordHash().equals("old-bcrypt")));
    }

    @Test
    void resetPasswordRejectsCurrentPasswordAndKeepsResetTokenUsable() {
        Patient patient = patient();
        patient.setPasswordResetTokenHash(hash("reset-token"));
        patient.setPasswordResetTokenExpiresAt(Instant.now().plusSeconds(60));
        when(patients.findByPasswordResetTokenHash(hash("reset-token"))).thenReturn(Optional.of(patient));
        when(encoder.matches("OldPassword1!", "old-bcrypt")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("reset-token", "OldPassword1!"));

        assertEquals("Mật khẩu mới không được trùng với mật khẩu đã từng sử dụng", error.getMessage());
        assertNotNull(patient.getPasswordResetTokenHash());
        verify(passwordHistory, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void resetPasswordRejectsPreviouslyUsedPassword() {
        Patient patient = patient();
        patient.setPasswordResetTokenHash(hash("reset-token"));
        patient.setPasswordResetTokenExpiresAt(Instant.now().plusSeconds(60));
        when(patients.findByPasswordResetTokenHash(hash("reset-token"))).thenReturn(Optional.of(patient));
        when(passwordHistory.findPasswordHashesByPatientId(1L)).thenReturn(List.of("older-bcrypt"));
        when(encoder.matches("OlderPassword1!", "old-bcrypt")).thenReturn(false);
        when(encoder.matches("OlderPassword1!", "older-bcrypt")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("reset-token", "OlderPassword1!"));

        verify(passwordHistory, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void requestPasswordResetSendsMailForExistingAccount() {
        Patient patient = patient();
        when(patients.findByEmail("user@example.com")).thenReturn(Optional.of(patient));

        service.requestPasswordReset("USER@example.com");

        assertNotNull(patient.getPasswordResetTokenHash());
        assertTrue(patient.getPasswordResetTokenExpiresAt().isAfter(Instant.now()));
        verify(mail).sendPasswordReset(argThat(p -> p.getEmail().equals("user@example.com")), anyString());
    }

    @Test
    void requestPasswordResetDoesNotRevealUnknownAccount() {
        when(patients.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("unknown@example.com");

        verify(encoder).matches(anyString(), anyString());
        verifyNoInteractions(mail);
    }

    @Test
    void changePasswordInvalidatesExistingTokens() {
        Patient patient = patient();
        patient.setTokenVersion(2);
        when(patients.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));
        when(encoder.matches("OldPassword1!", "old-bcrypt")).thenReturn(true);
        when(encoder.matches("NewPassword1!", "old-bcrypt")).thenReturn(false);
        when(encoder.encode("NewPassword1!")).thenReturn("new-bcrypt");

        service.changePassword(1L, "OldPassword1!", "NewPassword1!");

        assertEquals("new-bcrypt", patient.getPasswordHash());
        assertEquals(3, patient.getTokenVersion());
        verify(passwordHistory).save(argThat(item -> item.getPasswordHash().equals("old-bcrypt")));
    }

    @Test
    void changePasswordRejectsPreviouslyUsedPassword() {
        Patient patient = patient();
        when(patients.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));
        when(passwordHistory.findPasswordHashesByPatientId(1L)).thenReturn(List.of("older-bcrypt"));
        when(encoder.matches("OldPassword1!", "old-bcrypt")).thenReturn(true);
        when(encoder.matches("OlderPassword1!", "old-bcrypt")).thenReturn(false);
        when(encoder.matches("OlderPassword1!", "older-bcrypt")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword(1L, "OldPassword1!", "OlderPassword1!"));

        verify(passwordHistory, never()).save(any());
        verify(encoder, never()).encode(anyString());
    }

    @Test
    void changePasswordLocksAfterConfiguredFailedAttempts() {
        Patient patient = patient();
        when(patients.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));
        when(encoder.matches("wrong", "old-bcrypt")).thenReturn(false);

        for (int attempt = 1; attempt < policy.maxFailedAttempts(); attempt++) {
            assertThrows(BadCredentialsException.class,
                    () -> service.changePassword(1L, "wrong", "NewPassword1!"));
        }
        assertThrows(AccountLockedException.class,
                () -> service.changePassword(1L, "wrong", "NewPassword1!"));

        assertEquals(policy.maxFailedAttempts(), patient.getFailedLoginAttempts());
        assertNotNull(patient.getLockedUntil());
        verify(history, times(policy.maxFailedAttempts())).save(any(PatientLoginHistory.class));
    }

    @Test
    void changePasswordRejectsCorrectPasswordWhileLocked() {
        Patient patient = patient();
        patient.setLockedUntil(Instant.now().plusSeconds(60));
        when(patients.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));

        assertThrows(AccountLockedException.class,
                () -> service.changePassword(1L, "OldPassword1!", "NewPassword1!"));

        verifyNoInteractions(encoder);
        verify(history).save(argThat(item -> item.getOutcome() == vn.gastroai.be.domain.auth.LoginOutcome.BLOCKED));
    }

    @Test
    void successfulChangePasswordResetsFailureCounterAndLock() {
        Patient patient = patient();
        patient.setFailedLoginAttempts(2);
        patient.setLockedUntil(Instant.now().minusSeconds(1));
        patient.setTokenVersion(2);
        when(patients.findByIdForUpdate(1L)).thenReturn(Optional.of(patient));
        when(encoder.matches("OldPassword1!", "old-bcrypt")).thenReturn(true);
        when(encoder.matches("NewPassword1!", "old-bcrypt")).thenReturn(false);
        when(encoder.encode("NewPassword1!")).thenReturn("new-bcrypt");

        service.changePassword(1L, "OldPassword1!", "NewPassword1!");

        assertEquals(0, patient.getFailedLoginAttempts());
        assertNull(patient.getLockedUntil());
        assertEquals("new-bcrypt", patient.getPasswordHash());
    }

    @Test
    void loginSuccessIssuesJwtAndResetsFailureCounter() {
        Patient patient = patient();
        patient.setEmail("user@example.com");
        patient.setPasswordHash("bcrypt");
        patient.setEmailVerified(true);
        patient.setFailedLoginAttempts(2);
        when(patients.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(patient));
        when(encoder.matches("Password1!", "bcrypt")).thenReturn(true);
        when(jwt.generateToken(patient)).thenReturn("jwt");
        when(jwt.expiration("jwt")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResult result = service.login(new LoginCommand("USER@example.com", "Password1!"));

        assertEquals("jwt", result.token());
        assertEquals(0, patient.getFailedLoginAttempts());
        verify(history).save(any());
    }

    @Test
    void wrongPasswordLocksAccountAtConfiguredThreshold() {
        Patient patient = patient();
        patient.setPasswordHash("bcrypt");
        patient.setEmailVerified(true);
        patient.setFailedLoginAttempts(2);
        when(patients.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(patient));
        when(encoder.matches("wrong", "bcrypt")).thenReturn(false);

        assertThrows(AccountLockedException.class,
                () -> service.login(new LoginCommand("user@example.com", "wrong")));

        assertEquals(3, patient.getFailedLoginAttempts());
        assertNotNull(patient.getLockedUntil());
        verify(history).save(argThat((PatientLoginHistory item) ->
                item.getOutcome().name().equals("BLOCKED")));
        verify(notifications).createFailedLoginAlert(1L, "unknown", "unknown", true);
    }

    @Test
    void wrongPasswordCreatesSecurityNotificationForPatient() {
        Patient patient = patient();
        patient.setPasswordHash("bcrypt");
        patient.setEmailVerified(true);
        when(patients.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(patient));
        when(encoder.matches("wrong", "bcrypt")).thenReturn(false);
        ClientRequestInfo requestInfo = new ClientRequestInfo(
                "203.0.113.10", "Mozilla/5.0", "Chrome on Windows");

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginCommand("user@example.com", "wrong"), requestInfo));

        verify(notifications).createFailedLoginAlert(
                1L, "Chrome on Windows", "203.0.113.10", false);
    }

    @Test
    void wrongPasswordDoesNotCreateSecurityNotificationWhenDisabledByPatientSetting() {
        Patient patient = patient();
        patient.setPasswordHash("bcrypt");
        patient.setEmailVerified(true);
        when(patients.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(patient));
        when(encoder.matches("wrong", "bcrypt")).thenReturn(false);
        when(notificationSettings.isSecurityAlertEnabled(1L)).thenReturn(false);

        ClientRequestInfo requestInfo = new ClientRequestInfo(
                "203.0.113.10", "Mozilla/5.0", "Chrome on Windows");

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginCommand("user@example.com", "wrong"), requestInfo));

        // Xac nhan khong goi createFailedLoginAlert khi benh nhan da tat toggle Canh bao an toan & bao mat
        verify(notifications, never()).createFailedLoginAlert(any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("Verify Spring container can instantiate AuthService unambiguously with @Autowired")
    void verifySpringInstantiation() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("patients", PatientRepository.class, () -> patients);
        context.registerBean("history", PatientLoginHistoryRepository.class, () -> history);
        context.registerBean("passwordHistory", PatientPasswordHistoryRepository.class, () -> passwordHistory);
        context.registerBean("revoked", RevokedTokenRepository.class, () -> revoked);
        context.registerBean("encoder", PasswordEncoder.class, () -> encoder);
        context.registerBean("jwt", JwtService.class, () -> jwt);
        context.registerBean("mail", EmailVerificationService.class, () -> mail);
        context.registerBean("policy", LoginSecurityPolicy.class, () -> policy);
        context.registerBean("notifications", AppNotificationService.class, () -> notifications);
        context.registerBean("notificationSettings", PatientNotificationSettingService.class, () -> notificationSettings);
        context.registerBean(AuthService.class);
        context.refresh();

        AuthService bean = context.getBean(AuthService.class);
        assertNotNull(bean);
        context.close();
    }

    @Test
    void unknownEmailDoesNotCreateSecurityNotification() {
        when(patients.findByEmailForUpdate("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> service.login(new LoginCommand("unknown@example.com", "wrong")));

        verifyNoInteractions(notifications);
    }

    @Test
    void lockedAccountCannotAttemptPasswordAndRecordsBlockedLogin() {
        Patient patient = patient();
        patient.setLockedUntil(Instant.now().plusSeconds(60));
        when(patients.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(patient));

        assertThrows(AccountLockedException.class,
                () -> service.login(new LoginCommand("user@example.com", "Password1!")));

        verifyNoInteractions(encoder, jwt);
        verify(history).save(argThat((PatientLoginHistory item) ->
                item.getOutcome().name().equals("BLOCKED")));
    }

    @Test
    void manuallyDisabledAccountCannotLoginEvenWithCorrectPassword() {
        Patient patient = patient();
        patient.setAdminLocked(true);
        patient.setPasswordHash("bcrypt");
        patient.setEmailVerified(true);
        when(patients.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(patient));

        assertThrows(AccountDisabledException.class,
                () -> service.login(new LoginCommand("user@example.com", "Password1!")));

        verifyNoInteractions(encoder, jwt);
        verify(history).save(argThat((PatientLoginHistory item) ->
                item.getOutcome().name().equals("BLOCKED")));
    }

    @Test
    void expiredLockIsClearedBeforeSuccessfulLogin() {
        Patient patient = patient();
        patient.setPasswordHash("bcrypt");
        patient.setEmailVerified(true);
        patient.setFailedLoginAttempts(3);
        patient.setLockedUntil(Instant.now().minusSeconds(1));
        when(patients.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(patient));
        when(encoder.matches("Password1!", "bcrypt")).thenReturn(true);
        when(jwt.generateToken(patient)).thenReturn("jwt");
        when(jwt.expiration("jwt")).thenReturn(Instant.now().plusSeconds(3600));

        service.login(new LoginCommand("user@example.com", "Password1!"));

        assertEquals(0, patient.getFailedLoginAttempts());
        assertNull(patient.getLockedUntil());
    }

    @Test
    void resetPasswordRejectsExpiredTokenWithoutChangingPassword() {
        Patient patient = patient();
        patient.setPasswordResetTokenHash(hash("expired-reset"));
        patient.setPasswordResetTokenExpiresAt(Instant.now().minusSeconds(1));
        when(patients.findByPasswordResetTokenHash(hash("expired-reset"))).thenReturn(Optional.of(patient));

        assertEquals("Token dat lai mat khau da het han", assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("expired-reset", "NewPassword1!")).getMessage());
        assertEquals("old-bcrypt", patient.getPasswordHash());
        verify(encoder, never()).encode(anyString());
    }

    private Patient patient() {
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setRole(UserRole.PATIENT);
        patient.setEmail("user@example.com");
        patient.setPasswordHash("old-bcrypt");
        return patient;
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
