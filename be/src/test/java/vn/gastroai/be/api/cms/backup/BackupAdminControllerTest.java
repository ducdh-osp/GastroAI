package vn.gastroai.be.api.cms.backup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.backup.RestoreResult;
import vn.gastroai.be.infrastructure.backup.RestoreService;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BackupAdminController.class)
@Import(SecurityConfig.class)
class BackupAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestoreService restoreService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void restoreReturnsOkWhenBothDatabasesSucceed() throws Exception {
        when(restoreService.restoreAll())
                .thenReturn(new RestoreResult(true, null, true, null));

        mockMvc.perform(post("/api/v1/cms/backup/restore").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postgresOk").value(true))
                .andExpect(jsonPath("$.mysqlOk").value(true));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void restoreReturnsServerErrorWhenRestoreResultHasError() throws Exception {
        when(restoreService.restoreAll())
                .thenReturn(new RestoreResult(false, "must be owner of extension vector", true, null));

        mockMvc.perform(post("/api/v1/cms/backup/restore").with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.postgresOk").value(false))
                .andExpect(jsonPath("$.postgresError").value("must be owner of extension vector"));
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void restoreRejectsDoctorRole() throws Exception {
        mockMvc.perform(post("/api/v1/cms/backup/restore").with(csrf()))
                .andExpect(status().isForbidden());

        verify(restoreService, never()).restoreAll();
    }

    @Test
    void restoreRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/cms/backup/restore").with(csrf()))
                .andExpect(status().isForbidden());

        verify(restoreService, never()).restoreAll();
    }
}