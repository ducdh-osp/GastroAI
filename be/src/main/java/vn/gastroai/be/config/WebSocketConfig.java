package vn.gastroai.be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import vn.gastroai.be.infrastructure.security.AdminWebSocketHandlerDecoratorFactory;
import vn.gastroai.be.infrastructure.security.AdminWebSocketHandshakeInterceptor;
import vn.gastroai.be.infrastructure.security.StompClientSendBlockingInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    private final AdminWebSocketHandshakeInterceptor adminWebSocketHandshakeInterceptor;
    private final AdminWebSocketHandlerDecoratorFactory adminWebSocketHandlerDecoratorFactory;
    private final StompClientSendBlockingInterceptor stompClientSendBlockingInterceptor;

    public WebSocketConfig(
            AdminWebSocketHandshakeInterceptor adminWebSocketHandshakeInterceptor,
            AdminWebSocketHandlerDecoratorFactory adminWebSocketHandlerDecoratorFactory,
            StompClientSendBlockingInterceptor stompClientSendBlockingInterceptor) {
        this.adminWebSocketHandshakeInterceptor = adminWebSocketHandshakeInterceptor;
        this.adminWebSocketHandlerDecoratorFactory = adminWebSocketHandlerDecoratorFactory;
        this.stompClientSendBlockingInterceptor = stompClientSendBlockingInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigin)
                .addInterceptors(adminWebSocketHandshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(adminWebSocketHandlerDecoratorFactory);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompClientSendBlockingInterceptor);
    }
}