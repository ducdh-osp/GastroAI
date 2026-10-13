package vn.gastroai.be.api.cms.rbac;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.admin.CmsRbacService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsRbacAdminController.class)
@Import(SecurityConfig.class)
class CmsRbacAdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CmsRbacService service;
    @MockitoBean private JwtService jwt;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private RevokedTokenRepository revoked;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAssignContentEditor() throws Exception {
        mockMvc.perform(put("/api/v1/cms/rbac/accounts/DOCTOR/9/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CONTENT_EDITOR\"}"))
                .andExpect(status().isOk());
        verify(service).assignRole(vn.gastroai.be.domain.admin.CmsAccountType.DOCTOR, 9L,
                vn.gastroai.be.domain.admin.CmsRole.CONTENT_EDITOR);
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotAssignRoles() throws Exception {
        mockMvc.perform(put("/api/v1/cms/rbac/accounts/DOCTOR/9/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"CONTENT_EDITOR\"}"))
                .andExpect(status().isForbidden());
    }
}
