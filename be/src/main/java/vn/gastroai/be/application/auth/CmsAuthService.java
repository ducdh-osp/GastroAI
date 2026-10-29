package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import vn.gastroai.be.application.commands.CmsLoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.AdminLoginHistory;
import vn.gastroai.be.domain.admin.CmsAccountType;
import vn.gastroai.be.domain.admin.Doctor;
import vn.gastroai.be.domain.admin.DoctorLoginHistory;
import vn.gastroai.be.domain.auth.LoginOutcome;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorLoginHistoryRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

import java.time.Instant;


@Service
public class CmsAuthService {

    // Session lưu 2 attribute này để AdminSessionFilter/controller sau biết ai đang đăng
    // nhập và thuộc loại nào (ADMIN hay DOCTOR) mà không cần tra DB lại mỗi request.
    public static final String AUTH_USER_ID = "AUTH_USER_ID";
    public static final String AUTH_USER_TYPE = "AUTH_USER_TYPE";
    public static final String AUTH_ACCOUNT_TYPE = "AUTH_ACCOUNT_TYPE";
    public static final String AUTH_SESSION_VERSION = "AUTH_SESSION_VERSION";

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW";

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final AdminLoginHistoryRepository adminLoginHistoryRepository;
    private final DoctorLoginHistoryRepository doctorLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginSecurityPolicy loginSecurityPolicy;
    private final ClientRequestInfoResolver clientRequestInfoResolver;

    public CmsAuthService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            AdminLoginHistoryRepository adminLoginHistoryRepository,
            DoctorLoginHistoryRepository doctorLoginHistoryRepository,
            PasswordEncoder passwordEncoder,
            LoginSecurityPolicy loginSecurityPolicy,
            ClientRequestInfoResolver clientRequestInfoResolver) {

        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.adminLoginHistoryRepository = adminLoginHistoryRepository;
        this.doctorLoginHistoryRepository = doctorLoginHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginSecurityPolicy = loginSecurityPolicy;
        this.clientRequestInfoResolver = clientRequestInfoResolver;
    }

    private HttpSession rotateSession(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        return request.getSession(true);
    }

 
    @Transactional(value = "mysqlTransactionManager", noRollbackFor = {
            BadCredentialsException.class, AccountLockedException.class
    })
    public CmsAuthResult login(
            CmsLoginCommand command,
            HttpServletRequest request) {

        if (command.accountType() == CmsAccountType.ADMIN) {

            var admin = adminRepository.findByEmailForUpdate(command.email());

            if (admin.isPresent()) {
                return loginAdmin(
                        admin.get(),
                        command,
                        request);
            }

        } else if (command.accountType() == CmsAccountType.DOCTOR) {

            var doctor = doctorRepository.findByEmailForUpdate(command.email());

            if (doctor.isPresent()) {
                return loginDoctor(
                        doctor.get(),
                        command,
                        request);
            }
        }

        // Giữ lại fake BCrypt để hạn chế timing attack: không tìm thấy tài khoản vẫn mất
        // ngần ấy thời gian xử lý như trường hợp tìm thấy nhưng sai mật khẩu bên dưới,
        // tránh lộ email nào có tài khoản qua đo thời gian phản hồi.
        passwordEncoder.matches(
                command.password(),
                DUMMY_BCRYPT_HASH);

        throw new BadCredentialsException(
                "Email hoặc mật khẩu không đúng");
    }

    // loginAdmin/loginDoctor giống hệt nhau về logic (khoá tài khoản → check mật khẩu →
    // reset bộ đếm → ghi lịch sử → rotate session) — tách riêng theo Admin/Doctor vì 2 loại
    // dùng 2 entity + 2 repository khác nhau ở MySQL (không gộp interface chung được vì cả
    // 2 team làm độc lập, xem issue #5 mục 1 của Thăng).
    private CmsAuthResult loginAdmin(
            Admin user,
            CmsLoginCommand command,
            HttpServletRequest request) {

        Instant now = Instant.now();

        // 1. Kiểm tra tài khoản có đang bị khóa không
        if (isLocked(user.getLockedUntil(), now)) {

            saveAdminHistory(
                    user,
                    now,
                    LoginOutcome.BLOCKED,
                    "Tài khoản đang bị khóa",
                    request);

            throw new AccountLockedException(
                    user.getLockedUntil());
        }

        // 2. Kiểm tra password
        if (!passwordEncoder.matches(
                command.password(),
                user.getPasswordHash())) {

            handleAdminFailure(
                    user,
                    now,
                    request);

            throw new BadCredentialsException(
                    "Email hoặc mật khẩu không đúng");
        }

        // 3. Login thành công → reset trạng thái thất bại
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        adminRepository.save(user);

        // 4. Ghi lịch sử SUCCESS
        saveAdminHistory(
                user,
                now,
                LoginOutcome.SUCCESS,
                null,
                request);

        // 5. Rotate session
        HttpSession session = rotateSession(request);

        session.setAttribute(
                AUTH_USER_ID,
                user.getId());

        session.setAttribute(AUTH_USER_TYPE, user.getRole().name());
        session.setAttribute(AUTH_ACCOUNT_TYPE, CmsAccountType.ADMIN.name());
        session.setAttribute(AUTH_SESSION_VERSION, user.getSessionVersion());
        return new CmsAuthResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name());
    }

    private CmsAuthResult loginDoctor(
            Doctor user,
            CmsLoginCommand command,
            HttpServletRequest request) {

        Instant now = Instant.now();

        // 1. Kiểm tra tài khoản có đang bị khóa không
        if (isLocked(user.getLockedUntil(), now)) {

            saveDoctorHistory(
                    user,
                    now,
                    LoginOutcome.BLOCKED,
                    "Tài khoản đang bị khóa",
                    request);

            throw new AccountLockedException(
                    user.getLockedUntil());
        }

        // 2. Kiểm tra password
        if (!passwordEncoder.matches(
                command.password(),
                user.getPasswordHash())) {

            handleDoctorFailure(
                    user,
                    now,
                    request);

            throw new BadCredentialsException(
                    "Email hoặc mật khẩu không đúng");
        }

        // 3. Login thành công → reset trạng thái thất bại
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        doctorRepository.save(user);

        // 4. Ghi lịch sử SUCCESS
        saveDoctorHistory(
                user,
                now,
                LoginOutcome.SUCCESS,
                null,
                request);

        // 5. Rotate session
        HttpSession session = rotateSession(request);

        session.setAttribute(
                AUTH_USER_ID,
                user.getId());

        session.setAttribute(AUTH_USER_TYPE, user.getRole().name());
        session.setAttribute(AUTH_ACCOUNT_TYPE, CmsAccountType.DOCTOR.name());
        session.setAttribute(AUTH_SESSION_VERSION, user.getSessionVersion());
        return new CmsAuthResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name());
    }

    private void handleAdminFailure(
            Admin user,
            Instant now,
            HttpServletRequest request) {

        int attempts =
                user.getFailedLoginAttempts() + 1;

        user.setFailedLoginAttempts(attempts);

        String failureReason =
                "Sai email hoặc mật khẩu";

        if (attempts >=
                loginSecurityPolicy.maxFailedAttempts()) {

            Instant lockedUntil =
                    now.plus(
                            loginSecurityPolicy.lockDuration());

            user.setLockedUntil(lockedUntil);

            failureReason =
                    "Vượt quá số lần đăng nhập thất bại, tài khoản bị khóa";
        }

        adminRepository.save(user);

        saveAdminHistory(
                user,
                now,
                LoginOutcome.FAILURE,
                failureReason,
                request);
    }

    private void handleDoctorFailure(
            Doctor user,
            Instant now,
            HttpServletRequest request) {

        int attempts =
                user.getFailedLoginAttempts() + 1;

        user.setFailedLoginAttempts(attempts);

        String failureReason =
                "Sai email hoặc mật khẩu";

        if (attempts >=
                loginSecurityPolicy.maxFailedAttempts()) {

            Instant lockedUntil =
                    now.plus(
                            loginSecurityPolicy.lockDuration());

            user.setLockedUntil(lockedUntil);

            failureReason =
                    "Vượt quá số lần đăng nhập thất bại, tài khoản bị khóa";
        }

        doctorRepository.save(user);

        saveDoctorHistory(
                user,
                now,
                LoginOutcome.FAILURE,
                failureReason,
                request);
    }

    private boolean isLocked(
            Instant lockedUntil,
            Instant now) {

        return lockedUntil != null
                && lockedUntil.isAfter(now);
    }

    private void saveAdminHistory(
            Admin user,
            Instant attemptedAt,
            LoginOutcome outcome,
            String failureReason,
            HttpServletRequest request) {

        ClientRequestInfo requestInfo = clientRequestInfoResolver.resolve(request);

        adminLoginHistoryRepository.save(
                new AdminLoginHistory(
                        user,
                        attemptedAt,
                        outcome,
                        failureReason,
                        requestInfo.ipAddress(),
                        requestInfo.userAgent(),
                        requestInfo.deviceLabel()));
    }

    private void saveDoctorHistory(
            Doctor user,
            Instant attemptedAt,
            LoginOutcome outcome,
            String failureReason,
            HttpServletRequest request) {

        ClientRequestInfo requestInfo = clientRequestInfoResolver.resolve(request);

        doctorLoginHistoryRepository.save(
                new DoctorLoginHistory(
                        user,
                        attemptedAt,
                        outcome,
                        failureReason,
                        requestInfo.ipAddress(),
                        requestInfo.userAgent(),
                        requestInfo.deviceLabel()));
    }
    
    @Transactional(value = "mysqlTransactionManager", noRollbackFor = {
            IllegalArgumentException.class, AccountLockedException.class
    })
    public void changePassword(CmsAccountType accountType, Long accountId, String currentPassword,
            String newPassword, HttpServletRequest request) {
        if (accountType == CmsAccountType.ADMIN) {
            Admin user = adminRepository.findByIdForUpdate(accountId)
                    .orElseThrow(() -> new AccessDeniedException("Tai khoan khong ton tai"));
            changeAdminPassword(user, currentPassword, newPassword, request);
        } else {
            Doctor user = doctorRepository.findByIdForUpdate(accountId)
                    .orElseThrow(() -> new AccessDeniedException("Tai khoan khong ton tai"));
            changeDoctorPassword(user, currentPassword, newPassword, request);
        }
    }

    private void changeAdminPassword(Admin user, String currentPassword, String newPassword,
            HttpServletRequest request) {
        Instant now = Instant.now();
        if (isLocked(user.getLockedUntil(), now)) {
            saveAdminHistory(user, now, LoginOutcome.BLOCKED, "Đổi mật khẩu khi tài khoản đang bị khóa", request);
            throw new AccountLockedException(user.getLockedUntil());
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            boolean locked = attempts >= loginSecurityPolicy.maxFailedAttempts();
            if (locked) {
                user.setLockedUntil(now.plus(loginSecurityPolicy.lockDuration()));
            }
            adminRepository.save(user);
            saveAdminHistory(user, now, locked ? LoginOutcome.BLOCKED : LoginOutcome.FAILURE,
                    locked ? "Đổi mật khẩu sai quá số lần, tài khoản bị khóa"
                            : "Đổi mật khẩu: sai mật khẩu hiện tại",
                    request);
            if (locked) {
                throw new AccountLockedException(user.getLockedUntil());
            }
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        CmsPasswordPolicy.validate(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setSessionVersion(user.getSessionVersion() + 1);
        adminRepository.save(user);
    }

    private void changeDoctorPassword(Doctor user, String currentPassword, String newPassword,
            HttpServletRequest request) {
        Instant now = Instant.now();
        if (isLocked(user.getLockedUntil(), now)) {
            saveDoctorHistory(user, now, LoginOutcome.BLOCKED, "Đổi mật khẩu khi tài khoản đang bị khóa", request);
            throw new AccountLockedException(user.getLockedUntil());
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            boolean locked = attempts >= loginSecurityPolicy.maxFailedAttempts();
            if (locked) {
                user.setLockedUntil(now.plus(loginSecurityPolicy.lockDuration()));
            }
            doctorRepository.save(user);
            saveDoctorHistory(user, now, locked ? LoginOutcome.BLOCKED : LoginOutcome.FAILURE,
                    locked ? "Đổi mật khẩu sai quá số lần, tài khoản bị khóa"
                            : "Đổi mật khẩu: sai mật khẩu hiện tại",
                    request);
            if (locked) {
                throw new AccountLockedException(user.getLockedUntil());
            }
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        CmsPasswordPolicy.validate(newPassword);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setSessionVersion(user.getSessionVersion() + 1);
        doctorRepository.save(user);
    }

    /** UC0045 - Đăng xuất Admin/Bác sĩ: chỉ cần huỷ session, không có token nào cần thu hồi. */
    public void logout(
            HttpServletRequest request) {

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }

    /** Lịch sử đăng nhập của Admin đang xem, phân trang theo thời gian gần nhất. */
    public Page<AdminLoginHistory> adminHistory(Long adminId, Pageable pageable) {
        return adminLoginHistoryRepository.findByAdminId(adminId, pageable);
    }

    /** Tương tự adminHistory nhưng cho Bác sĩ — 2 bảng tách riêng nên 2 phương thức riêng. */
    public Page<DoctorLoginHistory> doctorHistory(Long doctorId, Pageable pageable) {
        return doctorLoginHistoryRepository.findByDoctorId(doctorId, pageable);
    }

    /** Số lần đăng nhập không thành công trong 7 ngày gần nhất — tính trên toàn bộ lịch sử. */
    public long adminRecentFailureCount(Long adminId) {
        Instant sevenDaysAgo = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        return adminLoginHistoryRepository.countByAdminIdAndOutcomeNotAndAttemptedAtAfter(
                adminId, LoginOutcome.SUCCESS, sevenDaysAgo);
    }

    /** Tương tự adminRecentFailureCount nhưng cho Bác sĩ. */
    public long doctorRecentFailureCount(Long doctorId) {
        Instant sevenDaysAgo = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
        return doctorLoginHistoryRepository.countByDoctorIdAndOutcomeNotAndAttemptedAtAfter(
                doctorId, LoginOutcome.SUCCESS, sevenDaysAgo);
    }
}

