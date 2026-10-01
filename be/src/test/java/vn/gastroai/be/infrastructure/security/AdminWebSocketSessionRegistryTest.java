package vn.gastroai.be.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminWebSocketSessionRegistryTest {

    private final AdminWebSocketSessionRegistry registry = new AdminWebSocketSessionRegistry();

    @Test
    void closesOnlySessionsRegisteredUnderTheGivenHttpSessionId() throws Exception {
        WebSocketSession sessionOfAdminA = mock(WebSocketSession.class);
        when(sessionOfAdminA.isOpen()).thenReturn(true);

        WebSocketSession sessionOfAdminB = mock(WebSocketSession.class);
        when(sessionOfAdminB.isOpen()).thenReturn(true);

        registry.register("http-session-admin-a", sessionOfAdminA);
        registry.register("http-session-admin-b", sessionOfAdminB);

        registry.closeSessionsFor("http-session-admin-a");

        verify(sessionOfAdminA).close();
        verify(sessionOfAdminB, never()).close();
    }

    @Test
    void closesMultipleWebSocketSessionsUnderSameHttpSession() throws Exception {
        WebSocketSession tab1 = mock(WebSocketSession.class);
        when(tab1.isOpen()).thenReturn(true);
        WebSocketSession tab2 = mock(WebSocketSession.class);
        when(tab2.isOpen()).thenReturn(true);

        registry.register("http-session-admin-a", tab1);
        registry.register("http-session-admin-a", tab2);

        registry.closeSessionsFor("http-session-admin-a");

        verify(tab1).close();
        verify(tab2).close();
    }

    @Test
    void doesNotFailWhenClosingUnknownHttpSessionId() {
        registry.closeSessionsFor("khong-ton-tai");
    }

    @Test
    void skipsClosingWhenSessionAlreadyClosed() throws Exception {
        WebSocketSession alreadyClosed = mock(WebSocketSession.class);
        when(alreadyClosed.isOpen()).thenReturn(false);

        registry.register("http-session-admin-a", alreadyClosed);

        registry.closeSessionsFor("http-session-admin-a");

        verify(alreadyClosed, never()).close();
    }
}