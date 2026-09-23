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

/**
 * Nghiệp vụ xác thực cho Bệnh nhân (UC0001-0008): đăng ký, xác thực email,
 * quên/đổi mật khẩu, đăng nhập, đăng xuất, lịch sử đăng nhập.
 * Chỉ thao tác trên PostgreSQL (bảng patients) — không liên quan Admin/Bác sĩ,
 * 2 nhóm đó dùng CmsAuthService riêng trên MySQL.
 */
@Service
public class AuthService {
    // Hash BCrypt "giả" dùng để so khớp mật khẩu khi không tìm thấy tài khoản,
    // nhằm giữ thời gian phản hồi giống hệt trường hợp tài khoản tồn tại nhưng sai mật khẩu
    // (chống timing attack dò email nào đã đăng ký).
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

    /**
     * UC0001 - Đăng ký. Tài khoản tạo ra ở trạng thái emailVerified=false,
     * sinh token xác thực ngẫu nhiên (sống 24h), chỉ lưu bản hash của token trong DB
     * (giống cách lưu mật khẩu) và gửi bản gốc qua email — lộ DB cũng không suy ra token.
     */
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

    /** Xác thực email từ link trong mail. Token dùng 1 lần — xoá hash ngay sau khi verify. */
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

    /**
     * UC0001 - Issue #7: cấp lại token xác thực mới khi token cũ (24h) đã hết hạn mà
     * chưa bấm link. Im lặng trả về (không báo lỗi) nếu không tìm thấy tài khoản hoặc
     * tài khoản đã xác thực rồi — cùng nguyên tắc không tiết lộ thông tin như
     * requestPasswordReset() bên dưới. Token cũ (nếu còn) bị ghi đè, chỉ token mới nhất
     * còn dùng được.
     */
    @Transactional("postgresTransactionManager")
    public void resendVerification(String email) {
        String normalizedEmail = normalize(email);
        Patient patient = patients.findByEmail(normalizedEmail).orElse(null);
        if (patient == null || patient.isEmailVerified()) {
            return;
        }

        String rawToken = UUID.randomUUID() + UUID.randomUUID().toString();
        patient.setVerificationTokenHash(hash(rawToken));
        patient.setVerificationTokenExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        mail.send(normalizedEmail, rawToken);
    }

    /**
     * UC0004 - Quên mật khẩu. Luôn trả về thành công dù email có tồn tại hay không
     * (không tiết lộ email nào đã đăng ký); nếu không tìm thấy tài khoản vẫn chạy
     * 1 lượt BCrypt giả để thời gian phản hồi không khác biệt.
     */
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
        // Tăng tokenVersion để mọi JWT đã phát hành trước đó bị vô hiệu ngay lập tức
        // (JwtAuthenticationFilter so version trong token với version hiện tại trong DB).
        patient.setTokenVersion(patient.getTokenVersion() + 1);
    }

    /**
     * UC0002 - Đăng nhập. noRollbackFor để các exception "nghiệp vụ" (sai mật khẩu,
     * bị khoá, chưa xác thực) không rollback transaction — cần giữ lại thay đổi
     * (tăng failedLoginAttempts, ghi lịch sử) dù request kết thúc bằng exception.
     * Dùng findByEmailForUpdate (SELECT ... FOR UPDATE) để khoá row khi đọc, tránh
     * race condition khi 2 request đăng nhập sai cùng lúc làm đếm sai số lần thất bại.
     */
    @Transactional(value = "postgresTransactionManager", noRollbackFor = {
            BadCredentialsException.class, AccountLockedException.class, IllegalStateException.class
    })
    public AuthResult login(LoginCommand command, ClientRequestInfo requestInfo) {
        Optional<Patient> found = patients.findByEmailForUpdate(normalize(command.email()));
        if (found.isEmpty()) {
            // Không tìm thấy tài khoản: vẫn chạy BCrypt giả để thời gian phản hồi
            // giống hệt case "có tài khoản nhưng sai mật khẩu" bên dưới.
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
            // Đã hết hạn khoá (lockedUntil đã qua) nhưng bộ đếm cũ vẫn còn — reset để
            // lần thử mới không bị cộng dồn tiếp từ trước.
            patient.setLockedUntil(null);
            patient.setFailedLoginAttempts(0);
        }
        if (!encoder.matches(command.password(), patient.getPasswordHash())) {
            patient.setFailedLoginAttempts(patient.getFailedLoginAttempts() + 1);
            // UC0008 - Chạm ngưỡng tối đa (maxFailedAttempts) ngay ở lần sai này thì khoá
            // luôn, ghi outcome BLOCKED (khác các lần sai trước chỉ ghi FAILURE) để phân
            // biệt "lần gây ra khoá" khi xem lại lịch sử.
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
        // Mật khẩu đúng nhưng chưa xác thực email: chỉ tiết lộ sau khi đã qua bước
        // kiểm tra mật khẩu ở trên, để không lộ trạng thái xác thực cho người dò mật khẩu.
        // Không tăng failedLoginAttempts — đây không phải hành vi đăng nhập sai.
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

    /** Overload dùng cho nơi không có thông tin request (vd job nội bộ, test). */
    public AuthResult login(LoginCommand command) {
        return login(command, new ClientRequestInfo("unknown", "unknown", "unknown"));
    }

    /**
     * UC0003 - Đăng xuất. JWT là stateless nên không thể "xoá" token phía client-side
     * kiểm soát được — phải đưa jti (JWT ID) vào bảng revoked_tokens, JwtAuthenticationFilter
     * sẽ từ chối mọi token có jti nằm trong bảng này dù chữ ký còn hợp lệ.
     * Token không hợp lệ/đã hết hạn thì coi như đã đăng xuất, không throw lỗi.
     */
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

    /** UC0005 - Đổi mật khẩu khi đã đăng nhập. Tăng tokenVersion để JWT cũ hết hiệu lực. */
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

    /** UC0007 - Xem lịch sử đăng nhập của chính mình, phân trang theo thời gian gần nhất. */
    public Page<PatientLoginHistory> history(Long patientId, Pageable pageable) {
        return history.findByPatientId(patientId, pageable);
    }

    /**
     * Số lần đăng nhập KHÔNG thành công trong 7 ngày gần nhất, tính trên toàn bộ lịch sử —
     * dùng cho cảnh báo bảo mật ở MeController, không bị giới hạn bởi trang đang xem như khi
     * tự đếm trên danh sách items() ở FE.
     */
    public long recentFailureCount(Long patientId) {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        return history.countByPatientIdAndOutcomeNotAndAttemptedAtAfter(
                patientId, LoginOutcome.SUCCESS, sevenDaysAgo);
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
