package vn.gastroai.be.infrastructure.security;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Set;


@Component
public class StompClientSendBlockingInterceptor implements ChannelInterceptor {

    private static final Set<String> ALLOWED_SUBSCRIBE_DESTINATIONS = Set.of(
            "/topic/triage-alerts", "/topic/triage-alerts-status");

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new MessageDeliveryException(message, "Client không được phép gửi tin qua WebSocket");
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && !ALLOWED_SUBSCRIBE_DESTINATIONS.contains(accessor.getDestination())) {
            throw new MessageDeliveryException(message, "Không được phép đăng ký kênh này");
        }

        return message;
    }
}