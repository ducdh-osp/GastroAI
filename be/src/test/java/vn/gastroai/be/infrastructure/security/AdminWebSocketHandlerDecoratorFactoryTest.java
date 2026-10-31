package vn.gastroai.be.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminWebSocketHandlerDecoratorFactoryTest {

    @Test
    void registersAndUnregistersAdminAndPatientSessions() throws Exception {
        AdminWebSocketSessionRegistry adminRegistry = mock(AdminWebSocketSessionRegistry.class);
        PatientWebSocketSessionRegistry patientRegistry = mock(PatientWebSocketSessionRegistry.class);
        WebSocketHandler delegate = mock(WebSocketHandler.class);
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(AdminWebSocketHandshakeInterceptor.HTTP_SESSION_ID_ATTRIBUTE, "http-session-1");
        attributes.put(PatientWebSocketHandshakeInterceptor.PATIENT_ID_ATTRIBUTE, 42L);
        when(session.getAttributes()).thenReturn(attributes);

        WebSocketHandler decorated = new AdminWebSocketHandlerDecoratorFactory(adminRegistry, patientRegistry)
                .decorate(delegate);

        decorated.afterConnectionEstablished(session);
        decorated.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(adminRegistry).register("http-session-1", session);
        verify(patientRegistry).register(42L, session);
        verify(adminRegistry).unregister("http-session-1", session);
        verify(patientRegistry).unregister(42L, session);
        verify(delegate).afterConnectionEstablished(session);
        verify(delegate).afterConnectionClosed(session, CloseStatus.NORMAL);
    }
}
