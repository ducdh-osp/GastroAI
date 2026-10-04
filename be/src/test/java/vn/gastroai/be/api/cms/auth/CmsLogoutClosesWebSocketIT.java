package vn.gastroai.be.api.cms.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC0067 - xac nhan dang xuat CMS (session.invalidate()) dong luon WebSocket dang mo cung
 * session do, khong de socket "treo" mai sau khi session phia server da bi huy.
 *
 * Can DB thuc (Postgres+MySQL) dang chay voi du lieu seed (V2) nen tu bo qua neu chua set
 * RUN_DB_IT=true, giong RagQueryServiceIT bo qua khi thieu GEMINI_API_KEY.
 *
 * Chay: RUN_DB_IT=true ./mvnw test -Dtest=CmsLogoutClosesWebSocketIT
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_DB_IT", matches = "true")
class CmsLogoutClosesWebSocketIT {

    @Value("${local.server.port}")
    private int port;

    @Value("${app.cors.allowed-origin}")
    private String allowedOrigin;

    @Test
    void logoutClosesWebSocketOpenedWithSameSession() throws Exception {
        String sessionCookie = login();

        CountDownLatch closedLatch = new CountDownLatch(1);
        WebSocketSession socketSession = openWebSocket(sessionCookie, closedLatch);

        try {
            logout(sessionCookie);

            assertTrue(closedLatch.await(5, TimeUnit.SECONDS),
                    "WebSocket phai duoc dong trong 5s sau khi logout");
        } finally {
            if (socketSession.isOpen()) {
                socketSession.close();
            }
        }
    }

    private String login() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/cms/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"admin@gastroai.vn\",\"password\":\"1\",\"role\":\"ADMIN\"}"))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Login phai thanh cong: " + response.body());

        List<String> setCookieHeaders = response.headers().allValues("Set-Cookie");
        assertTrue(!setCookieHeaders.isEmpty(), "Login phai tra ve Set-Cookie (JSESSIONID)");

        // Login doi session (chong session fixation) nen co the co nhieu Set-Cookie - lay
        // gia tri cuoi cung, dung voi session dang hieu luc sau khi login.
        String lastSetCookie = setCookieHeaders.get(setCookieHeaders.size() - 1);
        return lastSetCookie.split(";", 2)[0];
    }

    private WebSocketSession openWebSocket(String sessionCookie, CountDownLatch closedLatch) throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Cookie", sessionCookie);
        headers.add("Origin", allowedOrigin);

        return client.execute(new TextWebSocketHandler() {
            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                closedLatch.countDown();
            }
        }, headers, URI.create("ws://localhost:" + port + "/ws/websocket")).get(5, TimeUnit.SECONDS);
    }

    private void logout(String sessionCookie) throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/cms/auth/logout"))
                .header("Cookie", sessionCookie)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        assertEquals(204, response.statusCode());
    }
}