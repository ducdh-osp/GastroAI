package vn.gastroai.be.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StompClientSendBlockingInterceptorTest {

    private final StompClientSendBlockingInterceptor interceptor = new StompClientSendBlockingInterceptor();

    @Test
    void blocksSendFrameFromClient() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/topic/triage-alerts");

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void allowsSubscribeToAllowedDestination() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/triage-alerts");

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    @Test
    void allowsSubscribeToOtherAllowedDestination() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/triage-alerts-status");

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    @Test
    void blocksSubscribeToDisallowedDestination() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/khac");

        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void allowsConnectFrame() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    private static Message<byte[]> stompMessage(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}