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

/**
 * Xác thực JWT cho mọi request của Bệnh nhân — chạy trước UsernamePasswordAuthenticationFilter
 * (đăng ký trong SecurityConfig). Không cần session, mỗi request tự chứng minh danh tính qua
 * header Authorization: Bearer <token>. Token hợp lệ về mặt chữ ký/hạn vẫn có thể bị từ chối
 * nếu: đã bị thu hồi (logout, xem RevokedTokenRepository) hoặc tokenVersion không khớp
 * (đổi/reset mật khẩu làm mọi token cũ trước đó hết hiệu lực ngay, không cần đợi hết hạn).
 */
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
                // 3 điều kiện phải đúng hết: tài khoản còn tồn tại, token chưa bị thu hồi
                // (logout), và version trong token khớp version hiện tại trong DB (chưa bị
                // vô hiệu bởi đổi/reset mật khẩu).
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
                // Token sai định dạng/hết hạn/chữ ký sai: không set Authentication, coi như
                // request ẩn danh — để SecurityConfig tự quyết định endpoint đó có cần
                // đăng nhập hay không (permitAll thì vẫn qua, còn lại bị 401/403).
            }
        }
        chain.doFilter(request, response);
    }
}
