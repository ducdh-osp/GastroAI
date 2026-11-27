package vn.gastroai.be.api.cms.rbac;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.admin.CmsRbacService;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.admin.CmsRole;
import vn.gastroai.be.domain.audit.AuditAction;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsRbacAdminController.class)
@Import(SecurityConfig.class)
class CmsRbacAdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CmsRbacService service;
    @MockitoBean private AuditLogService auditLogService;
    @MockitoBean private JwtService jwt;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private RevokedTokenRepository revoked;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAssignContentEditor() throws Exception {
        mockMvc.perform(put("/api/v1/cms/rbac/accounts/9/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CONTENT_EDITOR\"}"))
                .andExpect(status().isOk());
        verify(service).assignRole(9L, CmsRole.CONTENT_EDITOR);
        verify(auditLogService).record(eq(AuditAction.RBAC_ROLE_CHANGE), eq("ADMIN"), eq(9L),
                eq("Vai trò mới: CONTENT_EDITOR"));
    }

    @Test
    @WithMockUser(roles = "CONTENT_EDITOR")
    void contentEditorCannotAssignRoles() throws Exception {
        mockMvc.perform(put("/api/v1/cms/rbac/accounts/9/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CONTENT_EDITOR\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_FORBIDDEN"));
        verify(auditLogService, never()).record(any(), any(), any(), any());
    }

    @Test
    void anonymousRequestGetsUnauthenticatedCode() throws Exception {
        mockMvc.perform(get("/api/v1/cms/rbac/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void sessionIdStaysTheSameAcrossRequests() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String before = session.getId();

        mockMvc.perform(get("/api/v1/cms/rbac/accounts").session(session)).andExpect(status().isOk());

        assertEquals(before, session.getId());
    }
}
