package vn.gastroai.be.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.gastroai.be.application.notification.EmailDeliveryService;

import java.time.Instant;

@Component
public class EmailOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxScheduler.class);

    private final EmailDeliveryService deliveryService;
    private final int batchSize;

    public EmailOutboxScheduler(EmailDeliveryService deliveryService,
                                @Value("${app.mail.worker-batch-size:20}") int batchSize) {
        this.deliveryService = deliveryService;
        this.batchSize = Math.max(1, batchSize);
    }

    @Scheduled(fixedDelayString = "${app.mail.worker-delay-ms:15000}")
    public void deliverPendingEmails() {
        Instant now = Instant.now();
        try {
            for (int processed = 0; processed < batchSize; processed++) {
                if (!deliveryService.processNext(now)) {
                    return;
                }
            }
        } catch (RuntimeException exception) {
            log.error("Khong the xu ly email outbox: {}", exception.getMessage(), exception);
        }
    }
}
