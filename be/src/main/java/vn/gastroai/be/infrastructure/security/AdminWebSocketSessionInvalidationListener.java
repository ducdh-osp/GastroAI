package vn.gastroai.be.infrastructure.security;

import org.springframework.context.event.EventListener;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

@Component
public class AdminWebSocketSessionInvalidationListener {

    private final AdminWebSocketSessionRegistry sessionRegistry;

    public AdminWebSocketSessionInvalidationListener(AdminWebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @EventListener
    public void onHttpSessionDestroyed(HttpSessionDestroyedEvent event) {
        sessionRegistry.closeSessionsFor(event.getId());
    }
}