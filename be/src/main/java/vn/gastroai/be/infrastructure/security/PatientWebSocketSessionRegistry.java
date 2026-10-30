package vn.gastroai.be.infrastructure.security;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/** Presence cho mot backend instance: con it nhat mot WebSocket mo thi benh nhan dang online. */
@Component
public class PatientWebSocketSessionRegistry {

    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByPatient =
            new ConcurrentHashMap<>();

    public void register(Long patientId, WebSocketSession session) {
        if (patientId == null) {
            return;
        }
        sessionsByPatient.computeIfAbsent(patientId, ignored -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(Long patientId, WebSocketSession session) {
        if (patientId == null) {
            return;
        }
        sessionsByPatient.computeIfPresent(patientId, (ignored, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public boolean isOnline(Long patientId) {
        Set<WebSocketSession> sessions = sessionsByPatient.get(patientId);
        if (sessions == null) {
            return false;
        }
        sessions.removeIf(session -> !session.isOpen());
        if (sessions.isEmpty()) {
            sessionsByPatient.remove(patientId, sessions);
            return false;
        }
        return true;
    }
}
