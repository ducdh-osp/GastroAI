package vn.gastroai.be.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.domain.admin.Admin;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.infrastructure.persistence.mysql.AdminRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminSessionFilterTest {

    private AdminRepository adminRepository;
    private AdminSessionFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockHttpSession session = new MockHttpSession();

    /** Ghi lại quyền mà các bước sau bộ lọc (SecurityConfig, controller) sẽ nhìn thấy. */
    private Authentication seenByNextStep;
    private final MockFilterChain chain = new MockFilterChain() {
        @Override
        public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            seenByNextStep = SecurityContextHolder.getContext().getAuthentication();
        }
    };

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        adminRepository = mock(AdminRepository.class);
        ObjectProvider<AdminRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(adminRepository);
        filter = new AdminSessionFilter(provider);
        request.setSession(session);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loggedIn(Long accountId, int sessionVersion) {
        session.setAttribute(CmsAuthService.AUTH_USER_ID, accountId);
        session.setAttribute(CmsAuthService.AUTH_SESSION_VERSION, sessionVersion);
    }

    private void account(Long id, CmsRole role, int sessionVersion) {
        Admin account = new Admin();
        account.setId(id);
        account.setRole(role);
        account.setSessionVersion(sessionVersion);
        when(adminRepository.findById(id)).thenReturn(Optional.of(account));
    }

    @Test
    void validSessionGetsRoleFromDatabase() throws Exception {
        loggedIn(1L, 0);
        account(1L, CmsRole.CONTENT_EDITOR, 0);

        filter.doFilter(request, response, chain);

        assertEquals("1", seenByNextStep.getName());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_CONTENT_EDITOR")),
                List.copyOf(seenByNextStep.getAuthorities()));
    }

    @Test
    void roleIsNotSavedIntoSession() throws Exception {
        loggedIn(1L, 0);
        account(1L, CmsRole.ADMIN, 0);

        filter.doFilter(request, response, chain);

        assertNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void sessionFromBeforePasswordChangeLosesEveryLeftoverRole() throws Exception {
        loggedIn(1L, 0);
        account(1L, CmsRole.ADMIN, 1);
        // Giả lập quyền cũ mà Spring Security đã nạp sẵn từ phiên trước khi tới bộ lọc.
        SecurityContext leftover = SecurityContextHolder.createEmptyContext();
        leftover.setAuthentication(new UsernamePasswordAuthenticationToken(
                "1", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        SecurityContextHolder.setContext(leftover);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, leftover);

        filter.doFilter(request, response, chain);

        assertNull(seenByNextStep);
        assertNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void deletedAccountIsTreatedAsNotLoggedIn() throws Exception {
        loggedIn(1L, 0);
        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertNull(seenByNextStep);
    }

    @Test
    void requestWithoutCmsSessionKeepsOtherAuthentication() throws Exception {
        // Ví dụ request của bệnh nhân: không có phiên CMS, quyền do bộ lọc JWT lo, không được xoá.
        Authentication patient = new UsernamePasswordAuthenticationToken(
                "9", null, List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(patient);
        SecurityContextHolder.setContext(context);

        filter.doFilter(request, response, chain);

        assertSame(patient, seenByNextStep);
        verifyNoInteractions(adminRepository);
    }
}