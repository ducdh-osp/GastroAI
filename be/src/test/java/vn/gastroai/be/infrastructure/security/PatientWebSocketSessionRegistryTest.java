package vn.gastroai.be.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientWebSocketSessionRegistryTest {

    private final PatientWebSocketSessionRegistry registry = new PatientWebSocketSessionRegistry();

    @Test
    void patientStaysOnlineWhileAtLeastOneTabIsOpen() {
        WebSocketSession tab1 = mock(WebSocketSession.class);
        WebSocketSession tab2 = mock(WebSocketSession.class);
        when(tab1.isOpen()).thenReturn(true);
        when(tab2.isOpen()).thenReturn(true);
        registry.register(7L, tab1);
        registry.register(7L, tab2);

        registry.unregister(7L, tab1);
        assertTrue(registry.isOnline(7L));

        registry.unregister(7L, tab2);
        assertFalse(registry.isOnline(7L));
    }

    @Test
    void removesStaleClosedSessions() {
        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.isOpen()).thenReturn(false);
        registry.register(7L, closed);

        assertFalse(registry.isOnline(7L));
    }
}
