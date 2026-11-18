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
        sessions.forEach(AdminWebSocketSessionRegistry::closeQuietly);
    }

    /**
     * Đóng mọi kết nối realtime của một tài khoản CMS, ở mọi phiên và mọi máy.
     * Dùng sau khi đổi mật khẩu: phiên cũ đã hết hiệu lực thì kết nối của nó cũng phải dừng nhận cảnh báo.
     */
    public void closeSessionsOfAccount(Long accountId) {
        if (accountId == null) {
            return;
        }
        sessionsByHttpSessionId.forEach((httpSessionId, sessions) -> sessions.stream()
                .filter(wsSession -> accountId.equals(wsSession.getAttributes()
                        .get(AdminWebSocketHandshakeInterceptor.CMS_USER_ID_ATTRIBUTE)))
                .forEach(wsSession -> {
                    unregister(httpSessionId, wsSession);
                    closeQuietly(wsSession);
                }));
    }

    private static void closeQuietly(WebSocketSession wsSession) {
        try {
            if (wsSession.isOpen()) {
                wsSession.close();
            }
        } catch (IOException exception) {
            // Bo qua - session co the da rot ket noi tu truoc, khong can lam gi them.
        }
    }
}