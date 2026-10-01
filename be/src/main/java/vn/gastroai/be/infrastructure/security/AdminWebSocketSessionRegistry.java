package vn.gastroai.be.infrastructure.security;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class AdminWebSocketSessionRegistry {

    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByHttpSessionId =
            new ConcurrentHashMap<>();

    public void register(String httpSessionId, WebSocketSession wsSession) {
        if (httpSessionId == null) {
            return;
        }
        sessionsByHttpSessionId
                .computeIfAbsent(httpSessionId, key -> new CopyOnWriteArraySet<>())
                .add(wsSession);
    }

    public void unregister(String httpSessionId, WebSocketSession wsSession) {
        if (httpSessionId == null) {
            return;
        }
        sessionsByHttpSessionId.computeIfPresent(httpSessionId, (key, sessions) -> {
            sessions.remove(wsSession);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public void closeSessionsFor(String httpSessionId) {
        Set<WebSocketSession> sessions = sessionsByHttpSessionId.remove(httpSessionId);
        if (sessions == null) {
            return;
        }
        for (WebSocketSession wsSession : sessions) {
            try {
                if (wsSession.isOpen()) {
                    wsSession.close();
                }
            } catch (IOException exception) {
                // Bo qua - session co the da rot ket noi tu truoc, khong can lam gi them.
            }
        }
    }
}