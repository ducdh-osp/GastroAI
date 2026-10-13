package vn.gastroai.be.api.cms.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.admin.AdminPatientManagementService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsUserAdminController.class)
@Import(SecurityConfig.class)
class CmsUserAdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AdminPatientManagementService service;
    @MockitoBean private JwtService jwt;
    @MockitoBean private PatientRepository patients;
    @MockitoBean private RevokedTokenRepository revoked;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanLockPatient() throws Exception {
        mockMvc.perform(post("/api/v1/cms/users/7/lock"))
                .andExpect(status().isOk());
        verify(service).lock(7L);
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void doctorCannotListOrLockPatients() throws Exception {
        mockMvc.perform(get("/api/v1/cms/users")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/cms/users/7/lock")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTENT_EDITOR")
    void contentEditorCannotManagePatients() throws Exception {
        mockMvc.perform(get("/api/v1/cms/users")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "PATIENT")
    void patientCannotAccessAdministration() throws Exception {
        mockMvc.perform(get("/api/v1/cms/users")).andExpect(status().isForbidden());
    }
}
