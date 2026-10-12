package vn.gastroai.be.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import vn.gastroai.be.domain.auth.Patient;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PatientWebSocketHandshakeInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final PatientRepository patientRepository = mock(PatientRepository.class);
    private final RevokedTokenRepository revokedTokenRepository = mock(RevokedTokenRepository.class);
    private final PatientWebSocketHandshakeInterceptor interceptor =
            new PatientWebSocketHandshakeInterceptor(jwtService, patientRepository, revokedTokenRepository);
    private final ServerHttpResponse response = mock(ServerHttpResponse.class);
    private final WebSocketHandler handler = mock(WebSocketHandler.class);

    @Test
    void rejectsHandshakeWithoutToken() {
        boolean accepted = interceptor.beforeHandshake(request("http://localhost/ws/patient/info"),
                response, handler, new HashMap<>());

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void acceptsValidCurrentPatientTokenAndStoresPatientId() {
        Claims claims = mock(Claims.class);
        when(jwtService.parse("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("7");
        when(claims.get("version", Number.class)).thenReturn(3);
        when(claims.getId()).thenReturn("jti-1");
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setTokenVersion(3);
        when(patientRepository.findById(7L)).thenReturn(Optional.of(patient));
        when(revokedTokenRepository.existsById("jti-1")).thenReturn(false);
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                request("http://localhost/ws/patient/info?access_token=valid-token"),
                response, handler, attributes);

        assertTrue(accepted);
        assertEquals(7L, attributes.get(PatientWebSocketHandshakeInterceptor.PATIENT_ID_ATTRIBUTE));
    }

    @Test
    void rejectsMalformedToken() {
        when(jwtService.parse("bad-token")).thenThrow(new JwtException("invalid"));

        boolean accepted = interceptor.beforeHandshake(
                request("http://localhost/ws/patient/info?access_token=bad-token"),
                response, handler, new HashMap<>());

        assertFalse(accepted);
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    private ServerHttpRequest request(String uri) {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getURI()).thenReturn(URI.create(uri));
        return request;
    }
}
