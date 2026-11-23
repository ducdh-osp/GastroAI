package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.commands.CmsLoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;
import vn.gastroai.be.application.readmodel.LoginHistoryItem;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.AdminLoginHistory;
import vn.gastroai.be.domain.admin.AdminPasswordHistory;
import vn.gastroai.be.domain.auth.LoginOutcome;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminPasswordHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * UC0045 - Đăng nhập, đăng xuất, đổi mật khẩu và lịch sử đăng nhập của tài khoản CMS.
 * Mọi tài khoản CMS nằm trong bảng admins; vai trò (ADMIN, CONTENT_EDITOR) là thuộc tính
 * của tài khoản, hệ thống tự đọc ra chứ không để người dùng tự chọn lúc đăng nhập.
 */
@Service
public class CmsAuthService {

    public static final String AUTH_USER_ID = "AUTH_USER_ID";
    public static final String AUTH_USER_TYPE = "AUTH_USER_TYPE";
    public static final String AUTH_SESSION_VERSION = "AUTH_SESSION_VERSION";

    private static final String INVALID_CREDENTIALS = "Email hoặc mật khẩu không đúng";

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW";

    private final AdminRepository adminRepository;
    private final AdminLoginHistoryRepository loginHistoryRepository;
    private final AdminPasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginSecurityPolicy loginSecurityPolicy;
    private final ClientRequestInfoResolver clientRequestInfoResolver;

    public CmsAuthService(
            AdminRepository adminRepository,
           AdminLoginHistoryRepository loginHistoryRepository,
            AdminPasswordHistoryRepository passwordHistoryRepository,
            PasswordEncoder passwordEncoder,
            LoginSecurityPolicy loginSecurityPolicy,
            ClientRequestInfoResolver clientRequestInfoResolver) {
        this.adminRepository = adminRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginSecurityPolicy = loginSecurityPolicy;
        this.clientRequestInfoResolver = clientRequestInfoResolver;
    }

    @Transactional(value = "mysqlTransactionManager", noRollbackFor = {
            BadCredentialsException.class, AccountLockedException.class, AccountDisabledException.class
    })
    public CmsAuthResult login(CmsLoginCommand command, HttpServletRequest request) {
        Optional<Admin> found = adminRepository.findByEmailForUpdate(command.email());

        if (found.isEmpty()) {
            // Vẫn chạy BCrypt với mã băm giả để thời gian phản hồi giống trường hợp sai mật khẩu,
            // không để lộ email nào có tài khoản qua việc đo thời gian.
            passwordEncoder.matches(command.password(), DUMMY_BCRYPT_HASH);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        Admin user = found.get();
        Instant now = Instant.now();

        if (user.isAdminLocked()) {
            saveHistory(user, now, LoginOutcome.BLOCKED, "Tài khoản bị Admin khóa", request);
            throw new AccountDisabledException();
        }

        if (isLocked(user, now)) {
            saveHistory(user, now, LoginOutcome.BLOCKED, "Tài khoản đang bị khóa", request);
            throw new AccountLockedException(user.getLockedUntil());
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            boolean locked = registerFailedAttempt(user, now);
            saveHistory(user, now, LoginOutcome.FAILURE,
                    locked ? "Vượt quá số lần đăng nhập thất bại, tài khoản bị khóa"
                            : "Sai email hoặc mật khẩu",
                    request);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        adminRepository.save(user);
        saveHistory(user, now, LoginOutcome.SUCCESS, null, request);

        HttpSession session = rotateSession(request);
        session.setAttribute(AUTH_USER_ID, user.getId());
        session.setAttribute(AUTH_USER_TYPE, user.getRole().name());
        session.setAttribute(AUTH_SESSION_VERSION, user.getSessionVersion());

        return new CmsAuthResult(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }

    @Transactional(value = "mysqlTransactionManager", noRollbackFor = {
            IllegalArgumentException.class, AccountLockedException.class, AccountDisabledException.class
    })
    public int changePassword(Long accountId, String currentPassword, String newPassword,
            HttpServletRequest request) {
        Admin user = adminRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccessDeniedException("Tài khoản không tồn tại hoặc đã bị xóa"));
        Instant now = Instant.now();

        if (user.isAdminLocked()) {
            saveHistory(user, now, LoginOutcome.BLOCKED, "Đổi mật khẩu khi tài khoản bị Admin khóa", request);
            throw new AccountDisabledException();
        }

        if (isLocked(user, now)) {
            saveHistory(user, now, LoginOutcome.BLOCKED, "Đổi mật khẩu khi tài khoản đang bị khóa", request);
            throw new AccountLockedException(user.getLockedUntil());
        }

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            boolean locked = registerFailedAttempt(user, now);
            saveHistory(user, now, locked ? LoginOutcome.BLOCKED : LoginOutcome.FAILURE,
                    locked ? "Đổi mật khẩu sai quá số lần, tài khoản bị khóa"
                            : "Đổi mật khẩu: sai mật khẩu hiện tại",
                    request);
            if (locked) {
                throw new AccountLockedException(user.getLockedUntil());
            }
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }

        PasswordPolicy.validate(newPassword);
        rejectReusedPassword(user, newPassword);

        passwordHistoryRepository.save(new AdminPasswordHistory(user, user.getPasswordHash(), now));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setSessionVersion(user.getSessionVersion() + 1);
        adminRepository.save(user);
        return user.getSessionVersion();
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Lịch sử đăng nhập của chính tài khoản đang xem, mới nhất trước. */
    public Page<LoginHistoryItem> loginHistory(Long accountId, Pageable pageable) {
        return loginHistoryRepository.findByAdminId(accountId, pageable)
                .map(history -> new LoginHistoryItem(
                        history.getId(),
                        history.getAttemptedAt(),
                        history.getOutcome().name(),
                        history.getFailureReason(),
                        history.getIpAddress(),
                        history.getUserAgent(),
                        history.getDeviceLabel()));
    }

    /** Số lần đăng nhập hoặc đổi mật khẩu không thành công trong 7 ngày qua, tính trên toàn bộ lịch sử. */
    public long recentFailureCount(Long accountId) {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        return loginHistoryRepository.countByAdminIdAndOutcomeNotAndAttemptedAtAfter(
                accountId, LoginOutcome.SUCCESS, sevenDaysAgo);
    }

    /** Mã tài khoản CMS đang đăng nhập trong phiên, null nếu phiên chưa đăng nhập. */
    public static Long sessionUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(AUTH_USER_ID);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    /**
     * Phiên còn hiệu lực khi số phiên lưu lúc đăng nhập vẫn khớp với tài khoản.
     * Đổi mật khẩu làm số này tăng lên, nên mọi phiên đăng nhập trước đó đều hết hiệu lực.
     */
    public static boolean isCurrentSession(Admin account, HttpSession session) {
        Object value = session.getAttribute(AUTH_SESSION_VERSION);
        return value != null && value.toString().equals(String.valueOf(account.getSessionVersion()));
    }

    /** Đăng nhập lại thì huỷ phiên cũ, cấp phiên mới để chống cố định phiên (session fixation). */
    private HttpSession rotateSession(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        return request.getSession(true);
    }

    private void rejectReusedPassword(Admin user, String newPassword) {
        boolean reused = passwordEncoder.matches(newPassword, user.getPasswordHash())
                || passwordHistoryRepository.findPasswordHashesByAdminId(user.getId()).stream()
                        .anyMatch(previousHash -> passwordEncoder.matches(newPassword, previousHash));
        if (reused) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu đã từng sử dụng");
        }
    }

    private boolean isLocked(Admin user, Instant now) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
    }

    /** Tăng bộ đếm sai mật khẩu, đủ ngưỡng thì khoá tạm. Trả về true nếu lần sai này làm khoá tài khoản. */
    private boolean registerFailedAttempt(Admin user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        boolean locked = attempts >= loginSecurityPolicy.maxFailedAttempts();
        if (locked) {
            user.setLockedUntil(now.plus(loginSecurityPolicy.lockDuration()));
        }
        adminRepository.save(user);
        return locked;
    }

    private void saveHistory(Admin user, Instant attemptedAt, LoginOutcome outcome, String failureReason,
            HttpServletRequest request) {
        ClientRequestInfo requestInfo = clientRequestInfoResolver.resolve(request);
        loginHistoryRepository.save(new AdminLoginHistory(
                user,
                attemptedAt,
                outcome,
                failureReason,
                requestInfo.ipAddress(),
                requestInfo.userAgent(),
                requestInfo.deviceLabel()));
    }
}