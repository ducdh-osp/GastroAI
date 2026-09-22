package vn.gastroai.be.application.auth;

import io.jsonwebtoken.Claims;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.application.readmodel.AuthResult;
import vn.gastroai.be.domain.auth.LoginOutcome;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.domain.auth.PatientLoginHistory;
import vn.gastroai.be.domain.auth.RevokedToken;
import vn.gastroai.be.domain.auth.UserRole;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {
    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW";

    private final PatientRepository patients;
    private final PatientLoginHistoryRepository history;
    private final RevokedTokenRepository revoked;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final EmailVerificationService mail;
    private final LoginSecurityPolicy policy;

    public AuthService(PatientRepository patients, PatientLoginHistoryRepository history,
                       RevokedTokenRepository revoked, PasswordEncoder encoder,
                       JwtService jwt, EmailVerificationService mail,
                       LoginSecurityPolicy policy) {
        this.patients = patients;
        this.history = history;
        this.revoked = revoked;
        this.encoder = encoder;
        this.jwt = jwt;
        this.mail = mail;
        this.policy = policy;
    }

    @Transactional("postgresTransactionManager")
    public void register(String email, String password, String name) {
        String normalizedEmail = normalize(email);
        if (patients.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalStateException("Email da duoc su dung");
        }
        String rawToken = UUID.randomUUID() + UUID.randomUUID().toString();
        Patient patient = new Patient();
        patient.setEmail(normalizedEmail);
        patient.setPasswordHash(encoder.encode(password));
        patient.setFullName(name.trim());
        patient.setRole(UserRole.PATIENT);
        patient.setEmailVerified(false);
        patient.setVerificationTokenHash(hash(rawToken));
        patient.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        patients.saveAndFlush(patient);
        mail.send(normalizedEmail, rawToken);
    }

    @Transactional("postgresTransactionManager")
    public void verify(String token) {
        Patient patient = patients.findByVerificationTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException("Token xac thuc khong hop le"));
        if (patient.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token xac thuc da het han");
        }
        patient.setEmailVerified(true);
        patient.setVerificationTokenHash(null);
        patient.setVerificationTokenExpiresAt(null);
    }

    @Transactional("postgresTransactionManager")
    public void requestPasswordReset(String email) {
        Patient patient = patients.findByEmail(normalize(email)).orElse(null);
        if (patient == null) {
            encoder.matches(UUID.randomUUID().toString(), DUMMY_BCRYPT_HASH);
            return;
        }
        String rawToken = UUID.randomUUID() + UUID.randomUUID().toString();
        patient.setPasswordResetTokenHash(hash(rawToken));
        patient.setPasswordResetTokenExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        mail.sendPasswordReset(patient.getEmail(), rawToken);
    }

    @Transactional("postgresTransactionManager")
    public void resetPassword(String token, String newPassword) {
        Patient patient = patients.findByPasswordResetTokenHash(hash(token))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Token dat lai mat khau khong hop le"));
        if (patient.getPasswordResetTokenExpiresAt() == null
                || patient.getPasswordResetTokenExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token dat lai mat khau da het han");
        }
        patient.setPasswordHash(encoder.encode(newPassword));
        patient.setPasswordResetTokenHash(null);
        patient.setPasswordResetTokenExpiresAt(null);
        patient.setFailedLoginAttempts(0);
        patient.setLockedUntil(null);
        patient.setTokenVersion(patient.getTokenVersion() + 1);
    }

    @Transactional(value = "postgresTransactionManager", noRollbackFor = {
            BadCredentialsException.class, AccountLockedException.class, IllegalStateException.class
    })
    public AuthResult login(LoginCommand command, ClientRequestInfo requestInfo) {
        Optional<Patient> found = patients.findByEmailForUpdate(normalize(command.email()));
        if (found.isEmpty()) {
            encoder.matches(command.password(), DUMMY_BCRYPT_HASH);
            throw new BadCredentialsException("Email hoac mat khau khong dung");
        }
        Patient patient = found.get();
        Instant now = Instant.now();
        if (patient.getLockedUntil() != null && patient.getLockedUntil().isAfter(now)) {
            record(patient, LoginOutcome.BLOCKED, "ACCOUNT_LOCKED", requestInfo);
            throw new AccountLockedException(patient.getLockedUntil());
        }
        if (patient.getLockedUntil() != null) {
            patient.setLockedUntil(null);
            patient.setFailedLoginAttempts(0);
        }
        if (!encoder.matches(command.password(), patient.getPasswordHash())) {
            patient.setFailedLoginAttempts(patient.getFailedLoginAttempts() + 1);
            boolean locked = patient.getFailedLoginAttempts() >= policy.maxFailedAttempts();
            if (locked) {
                patient.setLockedUntil(now.plus(policy.lockDuration()));
            }
            record(patient, locked ? LoginOutcome.BLOCKED : LoginOutcome.FAILURE,
                    locked ? "TOO_MANY_ATTEMPTS" : "BAD_PASSWORD", requestInfo);
            if (locked) {
                throw new AccountLockedException(patient.getLockedUntil());
            }
            throw new BadCredentialsException("Email hoac mat khau khong dung");
        }
        if (!patient.isEmailVerified()) {
            record(patient, LoginOutcome.FAILURE, "EMAIL_NOT_VERIFIED", requestInfo);
            throw new IllegalStateException("Email chua duoc xac thuc");
        }
        patient.setFailedLoginAttempts(0);
        patient.setLockedUntil(null);
        record(patient, LoginOutcome.SUCCESS, null, requestInfo);
        String token = jwt.generateToken(patient);
        return new AuthResult(token, patient.getId(), patient.getEmail(),
                patient.getFullName(), patient.getRole().name(), jwt.expiration(token));
    }

    public AuthResult login(LoginCommand command) {
        return login(command, new ClientRequestInfo("unknown", "unknown", "unknown"));
    }

    @Transactional("postgresTransactionManager")
    public void logoutSafely(String token) {
        Claims claims;
        try {
            claims = jwt.parse(token);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
            return;
        }
        revoked.save(new RevokedToken(
                claims.getId(), claims.getExpiration().toInstant(), Instant.now()));
    }

    @Transactional("postgresTransactionManager")
    public void changePassword(Long id, String oldPassword, String newPassword) {
        Patient patient = patients.findById(id)
                .orElseThrow(() -> new BadCredentialsException("Tai khoan khong ton tai"));
        if (!encoder.matches(oldPassword, patient.getPasswordHash())) {
            throw new BadCredentialsException("Mat khau hien tai khong dung");
        }
        if (encoder.matches(newPassword, patient.getPasswordHash())) {
            throw new IllegalArgumentException("Mat khau moi phai khac mat khau hien tai");
        }
        patient.setPasswordHash(encoder.encode(newPassword));
        patient.setTokenVersion(patient.getTokenVersion() + 1);
    }

    public Page<PatientLoginHistory> history(Long patientId, Pageable pageable) {
        return history.findByPatientId(patientId, pageable);
    }

    private void record(Patient patient, LoginOutcome outcome, String reason,
                        ClientRequestInfo requestInfo) {
        history.save(new PatientLoginHistory(patient, Instant.now(), outcome, reason,
                requestInfo.ipAddress(), requestInfo.userAgent(), requestInfo.deviceLabel()));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
