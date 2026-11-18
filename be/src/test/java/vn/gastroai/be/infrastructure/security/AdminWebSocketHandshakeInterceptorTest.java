package vn.gastroai.be.infrastructure.security;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminWebSocketHandshakeInterceptorTest {

    private AdminRepository adminRepository;
    private AdminWebSocketHandshakeInterceptor interceptor;

    private final MockHttpServletRequest servletRequest = new MockHttpServletRequest();
    private final MockHttpServletResponse servletResponse = new MockHttpServletResponse();
    private final Map<String, Object> attributes = new HashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        adminRepository = mock(AdminRepository.class);
        ObjectProvider<AdminRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(adminRepository);
        interceptor = new AdminWebSocketHandshakeInterceptor(provider);
    }

    private boolean handshake() {
        return interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                null,
                attributes);
    }

    private HttpSession loggedIn(Long accountId, int sessionVersion) {
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(CmsAuthService.AUTH_USER_ID, accountId);
        session.setAttribute(CmsAuthService.AUTH_SESSION_VERSION, sessionVersion);
        return session;
    }

    private Admin account(Long id, CmsRole role, int sessionVersion) {
        Admin account = new Admin();
        account.setId(id);
        account.setRole(role);
        account.setSessionVersion(sessionVersion);
        when(adminRepository.findById(id)).thenReturn(Optional.of(account));
        return account;
    }

    @Test
    void rejectsHandshakeWhenNoHttpSession() {
        assertFalse(handshake());
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
    }

    @Test
    void rejectsHandshakeWhenSessionIsNotLoggedIn() {
        servletRequest.getSession(true);

        assertFalse(handshake());
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
    }

    @Test
    void acceptsAdminAndStoresSessionInfo() {
        HttpSession session = loggedIn(1L, 0);
        account(1L, CmsRole.ADMIN, 0);

        assertTrue(handshake());
        assertEquals(session.getId(), attributes.get(AdminWebSocketHandshakeInterceptor.HTTP_SESSION_ID_ATTRIBUTE));
        assertEquals(1L, attributes.get(AdminWebSocketHandshakeInterceptor.CMS_USER_ID_ATTRIBUTE));
        assertEquals("ADMIN", attributes.get(AdminWebSocketHandshakeInterceptor.CMS_USER_TYPE_ATTRIBUTE));
    }

    @Test
    void rejectsContentEditor() {
        loggedIn(3L, 0);
        account(3L, CmsRole.CONTENT_EDITOR, 0);

        assertFalse(handshake());
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
    }

    @Test
    void rejectsSessionOpenedBeforePasswordChange() {
        loggedIn(1L, 0);
        account(1L, CmsRole.ADMIN, 1);

        assertFalse(handshake());
        assertEquals(HttpStatus.FORBIDDEN.value(), servletResponse.getStatus());
    }

    @Test
    void rejectsDeletedAccount() {
        loggedIn(1L, 0);
        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        assertFalse(handshake());
    }
}