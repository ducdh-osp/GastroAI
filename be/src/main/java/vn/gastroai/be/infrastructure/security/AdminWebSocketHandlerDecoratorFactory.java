package vn.gastroai.be.infrastructure.security;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Component
public class AdminWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private final AdminWebSocketSessionRegistry sessionRegistry;

    public AdminWebSocketHandlerDecoratorFactory(AdminWebSocketSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                Object httpSessionId = session.getAttributes()
                        .get(AdminWebSocketHandshakeInterceptor.HTTP_SESSION_ID_ATTRIBUTE);
                if (httpSessionId != null) {
                    sessionRegistry.register(httpSessionId.toString(), session);
                }
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                Object httpSessionId = session.getAttributes()
                        .get(AdminWebSocketHandshakeInterceptor.HTTP_SESSION_ID_ATTRIBUTE);
                if (httpSessionId != null) {
                    sessionRegistry.unregister(httpSessionId.toString(), session);
                }
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }
}