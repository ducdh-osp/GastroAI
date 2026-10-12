package vn.gastroai.be.infrastructure.security;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.gastroai.be.application.auth.CmsAuthService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminWebSocketHandshakeInterceptorTest {

    private final AdminWebSocketHandshakeInterceptor interceptor = new AdminWebSocketHandshakeInterceptor();

    @Test
    void rejectsHandshakeWhenNoHttpSession() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean allowed = interceptor.beforeHandshake(request, response, null, new HashMap<>());

        assertFalse(allowed);
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
    }

    @Test
    void rejectsHandshakeWhenSessionMissingAuthUserType() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.getSession(true);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean allowed = interceptor.beforeHandshake(request, response, null, new HashMap<>());

        assertFalse(allowed);
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
    }

    @Test
    void rejectsHandshakeWhenSessionRoleIsNeitherAdminNorDoctor() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(CmsAuthService.AUTH_USER_TYPE, "PATIENT");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean allowed = interceptor.beforeHandshake(request, response, null, new HashMap<>());

        assertFalse(allowed);
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
    }

    @Test
    void acceptsHandshakeAndStoresHttpSessionIdWhenRoleIsAdmin() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(CmsAuthService.AUTH_USER_TYPE, "ADMIN");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(request, response, null, attributes);

        assertTrue(allowed);
        assertEquals(session.getId(), attributes.get(AdminWebSocketHandshakeInterceptor.HTTP_SESSION_ID_ATTRIBUTE));
    }

    @Test
    void acceptsHandshakeWhenRoleIsDoctor() throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(CmsAuthService.AUTH_USER_TYPE, "DOCTOR");
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();

        ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
        ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

        boolean allowed = interceptor.beforeHandshake(request, response, null, new HashMap<>());

        assertTrue(allowed);
    }
}