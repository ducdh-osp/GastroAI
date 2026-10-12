package vn.gastroai.be.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

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
    void patientCanSubscribeOnlyToOwnNotificationDestination() {
        Message<byte[]> own = stompMessage(StompCommand.SUBSCRIBE,
                "/topic/notifications/patient/7", Map.of("PATIENT_ID", 7L));
        Message<byte[]> anotherPatient = stompMessage(StompCommand.SUBSCRIBE,
                "/topic/notifications/patient/8", Map.of("PATIENT_ID", 7L));
        Message<byte[]> triage = stompMessage(StompCommand.SUBSCRIBE,
                "/topic/triage-alerts", Map.of("PATIENT_ID", 7L));

        assertSame(own, interceptor.preSend(own, null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(anotherPatient, null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(triage, null));
    }

    @Test
    void onlyAdminCmsSessionCanSubscribeToAdminNotifications() {
        Message<byte[]> admin = stompMessage(StompCommand.SUBSCRIBE, "/topic/notifications/admin",
                Map.of("HTTP_SESSION_ID", "s1", "CMS_USER_TYPE", "ADMIN"));
        Message<byte[]> doctor = stompMessage(StompCommand.SUBSCRIBE, "/topic/notifications/admin",
                Map.of("HTTP_SESSION_ID", "s2", "CMS_USER_TYPE", "DOCTOR"));

        assertSame(admin, interceptor.preSend(admin, null));
        assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(doctor, null));
    }

    @Test
    void allowsConnectFrame() {
        Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

        Message<?> result = interceptor.preSend(message, null);

        assertSame(message, result);
    }

    private static Message<byte[]> stompMessage(StompCommand command, String destination) {
        return stompMessage(command, destination, null);
    }

    private static Message<byte[]> stompMessage(StompCommand command, String destination,
                                                Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (sessionAttributes != null) {
            accessor.setSessionAttributes(sessionAttributes);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
