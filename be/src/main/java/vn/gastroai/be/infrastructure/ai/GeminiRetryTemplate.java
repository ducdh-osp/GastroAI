package vn.gastroai.be.infrastructure.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Gemini free tier hay tra 503 "model overloaded" thoang qua (Google tai lieu hoa day la loi
 * tam thoi, ho khuyen nghi client tu dong retry) - neu khong retry, 1 lan 503 ngau nhien la
 * nguoi dung thay "AI dang khong hoat dong" ngay lap tuc, du Google thuong tu phuc hoi trong
 * vai giay. CHI retry 503 - KHONG retry 429 (het quota trong ngay, retry ngay lap tuc vo ich,
 * phai doi sang ngay moi het).
 */
@Component
class GeminiRetryTemplate {

    private static final long[] BACKOFF_MS = {500, 1500, 3000};

    <T> T withRetry(Supplier<T> call) {
        for (int attempt = 0; ; attempt++) {
            try {
                return call.get();
            } catch (RestClientResponseException exception) {
                if (exception.getStatusCode().value() != 503 || attempt >= BACKOFF_MS.length) {
                    throw exception;
                }
                sleep(BACKOFF_MS[attempt]);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bi ngat khi cho retry goi Gemini", interrupted);
        }
    }
}
