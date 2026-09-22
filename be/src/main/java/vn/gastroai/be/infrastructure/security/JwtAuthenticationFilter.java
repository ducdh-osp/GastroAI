package vn.gastroai.be.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final PatientRepository patients;
    private final RevokedTokenRepository revoked;

    public JwtAuthenticationFilter(JwtService jwt, PatientRepository patients,
                                   RevokedTokenRepository revoked) {
        this.jwt = jwt;
        this.patients = patients;
        this.revoked = revoked;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.parse(header.substring(7));
                Long patientId = Long.valueOf(claims.getSubject());
                Optional<Patient> patient = patients.findById(patientId);
                Number tokenVersion = claims.get("version", Number.class);
                if (patient.isPresent()
                        && !revoked.existsById(claims.getId())
                        && tokenVersion != null
                        && patient.get().getTokenVersion() == tokenVersion.intValue()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            patientId, null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + patient.get().getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                // Invalid tokens are treated as anonymous requests.
            }
        }
        chain.doFilter(request, response);
    }
}
