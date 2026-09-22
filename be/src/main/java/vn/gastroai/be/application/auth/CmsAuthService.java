package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.gastroai.be.application.commands.CmsLoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.AdminLoginHistory;
import vn.gastroai.be.domain.admin.CmsRole;
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

    public static final String AUTH_USER_ID = "AUTH_USER_ID";
    public static final String AUTH_USER_TYPE = "AUTH_USER_TYPE";

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO5sK2PXrAnq3N33CpA6RKpA1MKSO0HsW";

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final AdminLoginHistoryRepository adminLoginHistoryRepository;
    private final DoctorLoginHistoryRepository doctorLoginHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginSecurityPolicy loginSecurityPolicy;

    public CmsAuthService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            AdminLoginHistoryRepository adminLoginHistoryRepository,
            DoctorLoginHistoryRepository doctorLoginHistoryRepository,
            PasswordEncoder passwordEncoder,
            LoginSecurityPolicy loginSecurityPolicy) {

        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.adminLoginHistoryRepository = adminLoginHistoryRepository;
        this.doctorLoginHistoryRepository = doctorLoginHistoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginSecurityPolicy = loginSecurityPolicy;
    }

    private HttpSession rotateSession(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        return request.getSession(true);
    }

    public CmsAuthResult login(
            CmsLoginCommand command,
            HttpServletRequest request) {

        if (command.role() == CmsRole.ADMIN) {

            var admin = adminRepository.findByEmail(command.email());

            if (admin.isPresent()) {
                return loginAdmin(
                        admin.get(),
                        command,
                        request);
            }

        } else if (command.role() == CmsRole.DOCTOR) {

            var doctor = doctorRepository.findByEmail(command.email());

            if (doctor.isPresent()) {
                return loginDoctor(
                        doctor.get(),
                        command,
                        request);
            }
        }

        // Giữ lại fake BCrypt để hạn chế timing attack
        passwordEncoder.matches(
                command.password(),
                DUMMY_BCRYPT_HASH);

        throw new BadCredentialsException(
                "Email hoặc mật khẩu không đúng");
    }

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

        session.setAttribute(
                AUTH_USER_TYPE,
                "ADMIN");

        return new CmsAuthResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                "ADMIN");
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

        session.setAttribute(
                AUTH_USER_TYPE,
                "DOCTOR");

        return new CmsAuthResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                "DOCTOR");
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

        adminLoginHistoryRepository.save(
                new AdminLoginHistory(
                        user,
                        attemptedAt,
                        outcome,
                        failureReason,
                        getIpAddress(request),
                        getUserAgent(request),
                        getDeviceLabel(request)));
    }

    private void saveDoctorHistory(
            Doctor user,
            Instant attemptedAt,
            LoginOutcome outcome,
            String failureReason,
            HttpServletRequest request) {

        doctorLoginHistoryRepository.save(
                new DoctorLoginHistory(
                        user,
                        attemptedAt,
                        outcome,
                        failureReason,
                        getIpAddress(request),
                        getUserAgent(request),
                        getDeviceLabel(request)));
    }

    private String getIpAddress(
            HttpServletRequest request) {

        return request.getRemoteAddr();
    }

    private String getUserAgent(
            HttpServletRequest request) {

        String userAgent =
                request.getHeader("User-Agent");

        return userAgent != null
                ? userAgent
                : "Unknown";
    }

    private String getDeviceLabel(
            HttpServletRequest request) {

        String userAgent =
                request.getHeader("User-Agent");

        return userAgent != null
                ? userAgent
                : "Unknown";
    }

    public void logout(
            HttpServletRequest request) {

        HttpSession session =
                request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }
}

