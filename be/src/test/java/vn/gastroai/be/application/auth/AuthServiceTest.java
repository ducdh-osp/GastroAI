package vn.gastroai.be.application.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.application.readmodel.AuthResult;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.auth.PatientLoginHistory;
import vn.gastroai.be.domain.auth.UserRole;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private final PatientRepository patients = mock(PatientRepository.class);
    private final PatientLoginHistoryRepository history = mock(PatientLoginHistoryRepository.class);
    private final RevokedTokenRepository revoked = mock(RevokedTokenRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwt = mock(JwtService.class);
    private final EmailVerificationService mail = mock(EmailVerificationService.class);
    private final LoginSecurityPolicy policy = new LoginSecurityPolicy(3, 10, 90);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(patients, history, revoked, encoder, jwt, mail, policy);
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
        verify(mail).send(eq("user@example.com"), anyString());
    }

    @Test
    void registerRejectsPasswordOverBcryptUtf8LimitBeforeDatabaseCall() {
        String password = "ă".repeat(37); // 74 UTF-8 bytes

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.register("user@example.com", password, "User"));

        assertEquals("Mat khau khong duoc vuot qua 72 byte UTF-8", exception.getMessage());
        verifyNoInteractions(patients, encoder, mail);
    }

    @Test
    void registerAccepts72Utf8BytesEvenWhenCharacterCountIsBelowLimit() {
        String password = "ă".repeat(36); // 72 UTF-8 bytes
        when(patients.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(encoder.encode(password)).thenReturn("bcrypt");
        when(patients.saveAndFlush(any(Patient.class))).thenAnswer(invocation ->
                (Patient) invocation.getArgument(0));

        service.register("user@example.com", password, "User");

        verify(encoder).encode(password);
        verify(mail).send(eq("user@example.com"), anyString());
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
    }

    @Test
    void requestPasswordResetSendsMailForExistingAccount() {
        Patient patient = patient();
        when(patients.findByEmail("user@example.com")).thenReturn(Optional.of(patient));

        service.requestPasswordReset("USER@example.com");

        assertNotNull(patient.getPasswordResetTokenHash());
        assertTrue(patient.getPasswordResetTokenExpiresAt().isAfter(Instant.now()));
        verify(mail).sendPasswordReset(eq("user@example.com"), anyString());
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
        when(patients.findById(1L)).thenReturn(Optional.of(patient));
        when(encoder.matches("OldPassword1!", "old-bcrypt")).thenReturn(true);
        when(encoder.matches("NewPassword1!", "old-bcrypt")).thenReturn(false);
        when(encoder.encode("NewPassword1!")).thenReturn("new-bcrypt");

        service.changePassword(1L, "OldPassword1!", "NewPassword1!");

        assertEquals("new-bcrypt", patient.getPasswordHash());
        assertEquals(3, patient.getTokenVersion());
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
