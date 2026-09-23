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

/**
 * Đăng nhập cho Admin/Bác sĩ (UC0045) — dùng HttpSession, khác hẳn JWT của Bệnh nhân, vì
 * Admin/Doctor lưu ở MySQL (2 bảng riêng: admins, doctors), không dùng chung
 * PatientRepository/AuthService. Logic khoá tài khoản (UC0008) tái dùng chung
 * LoginSecurityPolicy với AuthService để nhất quán chính sách trên toàn hệ thống,
 * dù dữ liệu tách bảng.
 */
@Service
public class CmsAuthService {

    // Session lưu 2 attribute này để AdminSessionFilter/controller sau biết ai đang đăng
    // nhập và thuộc loại nào (ADMIN hay DOCTOR) mà không cần tra DB lại mỗi request.
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

    // Chống session fixation: huỷ session cũ (nếu có) rồi tạo session MỚI hoàn toàn sau khi
    // xác thực thành công, thay vì tái sử dụng session đã tồn tại từ trước khi đăng nhập —
    // nếu không, kẻ tấn công có thể "gài" sẵn 1 session ID cho nạn nhân rồi chiếm quyền
    // ngay khi nạn nhân đăng nhập vào chính session ID đó.
    private HttpSession rotateSession(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        return request.getSession(true);
    }

    /**
     * FE phải tự khai báo role (ADMIN/DOCTOR) muốn đăng nhập — không tự dò cả 2 bảng như
     * cách cũ, vì email admin và doctor có thể trùng nhau ở 2 bảng khác nhau, dò cả 2 sẽ
     * mơ hồ nên đăng nhập vào tài khoản nào.
     */
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

    // Lưu ý: chưa thật sự suy ra tên thiết bị/trình duyệt — đang trả về nguyên User-Agent
    // giống hệt getUserAgent() ở trên, khác với ClientRequestInfoResolver.deviceLabel() bên
    // AuthService (Bệnh nhân) đã parse ra dạng "Chrome trên Windows". Có thể tái dùng logic
    // đó nếu muốn CMS hiển thị đẹp hơn, không cấp bách.
    private String getDeviceLabel(
            HttpServletRequest request) {

        String userAgent =
                request.getHeader("User-Agent");

        return userAgent != null
                ? userAgent
                : "Unknown";
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
}

