package vn.gastroai.be.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;

import java.util.Map;

@Component
public class PatientWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String PATIENT_ID_ATTRIBUTE = "PATIENT_ID";

    private final JwtService jwtService;
    private final PatientRepository patientRepository;
    private final RevokedTokenRepository revokedTokenRepository;

    public PatientWebSocketHandshakeInterceptor(JwtService jwtService,
                                                PatientRepository patientRepository,
                                                RevokedTokenRepository revokedTokenRepository) {
        this.jwtService = jwtService;
        this.patientRepository = patientRepository;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build()
                .getQueryParams().getFirst("access_token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        try {
            Claims claims = jwtService.parse(token);
            Long patientId = Long.valueOf(claims.getSubject());
            Number version = claims.get("version", Number.class);
            var patient = patientRepository.findById(patientId);
            if (patient.isEmpty() || patient.get().isAdminLocked() || version == null
                    || patient.get().getTokenVersion() != version.intValue()
                    || revokedTokenRepository.existsById(claims.getId())) {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return false;
            }
            attributes.put(PATIENT_ID_ATTRIBUTE, patientId);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Không có tài nguyên phụ cần dọn sau handshake.
    }
}
