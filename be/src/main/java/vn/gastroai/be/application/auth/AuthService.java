package vn.gastroai.be.application.auth;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.application.readmodel.AuthResult;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

@Service
public class AuthService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PatientRepository patientRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResult login(LoginCommand command) {
        Patient patient = patientRepository.findByEmail(command.email())
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(command.password(), patient.getPasswordHash())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }

        String token = jwtService.generateToken(patient.getId(), patient.getEmail());
        return new AuthResult(token, patient.getId(), patient.getEmail(), patient.getFullName());
    }
}
