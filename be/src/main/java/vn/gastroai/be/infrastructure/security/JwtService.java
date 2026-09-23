package vn.gastroai.be.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.gastroai.be.domain.auth.Patient;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Sinh và giải mã JWT cho Bệnh nhân — ký bằng HMAC (khoá bí mật đối xứng, app.jwt.secret). */
@Component
public class JwtService {
    private final SecretKey key;
    private final long minutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes:60}") long minutes) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.minutes = minutes;
    }

    /**
     * jti (id) ngẫu nhiên để có thể thu hồi từng token riêng lẻ khi logout
     * (xem RevokedTokenRepository). Claim "version" gắn với tokenVersion của Patient
     * tại thời điểm phát hành — đổi/reset mật khẩu tăng version này lên làm token cũ
     * hết hiệu lực ngay dù chưa hết hạn (xem JwtAuthenticationFilter).
     */
    public String generateToken(Patient p) {
        Instant now = Instant.now();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(p.getId().toString())
                .claim("email", p.getEmail()).claim("role", p.getRole().name())
                .claim("version", p.getTokenVersion()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(minutes, ChronoUnit.MINUTES))).signWith(key).compact();
    }

    /** Ném JwtException nếu chữ ký sai hoặc token hết hạn — caller tự bắt và xử lý. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public Instant expiration(String token) {
        return parse(token).getExpiration().toInstant();
    }
}
