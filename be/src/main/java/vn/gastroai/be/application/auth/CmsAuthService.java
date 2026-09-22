package vn.gastroai.be.application.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.Doctor;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;
import vn.gastroai.be.infrastructure.persistence.mysql.DoctorRepository;

@Service
public class CmsAuthService {

    public static final String AUTH_USER_ID = "AUTH_USER_ID";
    public static final String AUTH_USER_TYPE = "AUTH_USER_TYPE";

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public CmsAuthService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CmsAuthResult login(LoginCommand command, HttpSession session) {

        var admin = adminRepository.findByEmail(command.email());

        if (admin.isPresent()) {
            Admin user = admin.get();

            if (!passwordEncoder.matches(
                    command.password(),
                    user.getPasswordHash())) {
                throw new BadCredentialsException(
                        "Email hoặc mật khẩu không đúng");
            }

            session.setAttribute(AUTH_USER_ID, user.getId());
            session.setAttribute(AUTH_USER_TYPE, "ADMIN");

            return new CmsAuthResult(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    "ADMIN"
            );
        }

        var doctor = doctorRepository.findByEmail(command.email());

        if (doctor.isPresent()) {
            Doctor user = doctor.get();

            if (!passwordEncoder.matches(
                    command.password(),
                    user.getPasswordHash())) {
                throw new BadCredentialsException(
                        "Email hoặc mật khẩu không đúng");
            }

            session.setAttribute(AUTH_USER_ID, user.getId());
            session.setAttribute(AUTH_USER_TYPE, "DOCTOR");

            return new CmsAuthResult(
                    user.getId(),
                    user.getEmail(),
                    user.getFullName(),
                    "DOCTOR"
            );
        }

        throw new BadCredentialsException(
                "Email hoặc mật khẩu không đúng");
    }
}