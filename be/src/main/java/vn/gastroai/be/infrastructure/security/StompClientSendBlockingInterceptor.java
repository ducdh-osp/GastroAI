package vn.gastroai.be.infrastructure.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class StompClientSendBlockingInterceptor implements ChannelInterceptor {

    private static final Set<String> CMS_DESTINATIONS = Set.of(
            "/topic/triage-alerts", "/topic/triage-alerts-status");
    private static final String ADMIN_NOTIFICATION_DESTINATION = "/topic/notifications/admin";
    private static final String PATIENT_NOTIFICATION_PREFIX = "/topic/notifications/patient/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            throw new MessageDeliveryException(message, "Client khong duoc phep gui tin qua WebSocket");
        }
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            Map<String, Object> attributes = accessor.getSessionAttributes();
            boolean cmsSession = attributes != null
                    && attributes.get(AdminWebSocketHandshakeInterceptor.HTTP_SESSION_ID_ATTRIBUTE) != null;
            String cmsRole = attributes == null ? null
                    : String.valueOf(attributes.get(AdminWebSocketHandshakeInterceptor.CMS_USER_TYPE_ATTRIBUTE));
            Object patientId = attributes == null ? null
                    : attributes.get(PatientWebSocketHandshakeInterceptor.PATIENT_ID_ATTRIBUTE);
            // /ws is already protected by AdminWebSocketHandshakeInterceptor. Keeping the
            // no-session branch preserves the existing interceptor unit contract; a patient
            // handshake always carries PATIENT_ID and is therefore denied these destinations.
            boolean allowedCms = patientId == null && CMS_DESTINATIONS.contains(destination);
            boolean allowedAdminNotification = cmsSession && "ADMIN".equals(cmsRole)
                    && ADMIN_NOTIFICATION_DESTINATION.equals(destination);
            boolean allowedPatient = patientId != null
                    && (PATIENT_NOTIFICATION_PREFIX + patientId).equals(destination);
            if (!allowedCms && !allowedAdminNotification && !allowedPatient) {
                throw new MessageDeliveryException(message, "Khong duoc phep dang ky kenh nay");
            }
        }
        return message;
    }
}
