package vn.gastroai.be.api.cms.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.config.AppRuntimeConfigService;
import vn.gastroai.be.application.config.AppRuntimeConfigSnapshot;
import vn.gastroai.be.application.config.UpdateAppRuntimeConfigRequest;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppRuntimeConfigAdminController.class)
@Import(SecurityConfig.class)
class AppRuntimeConfigAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppRuntimeConfigService appRuntimeConfigService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void getReturnsCurrentConfigForAdmin() throws Exception {
        when(appRuntimeConfigService.getConfig())
                .thenReturn(new AppRuntimeConfigSnapshot(5, null, true, null, null));

        mockMvc.perform(get("/api/v1/cms/runtime-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ragTopK").value(5))
                .andExpect(jsonPath("$.triageRealtimeAlertsEnabled").value(true));
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void getRejectsDoctorRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/runtime-config"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void getRejectsPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/runtime-config"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void updatePassesAuthenticatedAdminIdToService() throws Exception {
        UpdateAppRuntimeConfigRequest request = new UpdateAppRuntimeConfigRequest(10, 0.8, false);
        when(appRuntimeConfigService.updateConfig(request, "ADMIN", 7L))
                .thenReturn(new AppRuntimeConfigSnapshot(10, 0.8, false, "ADMIN", 7L));

        mockMvc.perform(put("/api/v1/cms/runtime-config")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"ragTopK\":10,\"ragSimilarityThreshold\":0.8,\"triageRealtimeAlertsEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ragTopK").value(10))
                .andExpect(jsonPath("$.updatedByType").value("ADMIN"));

        verify(appRuntimeConfigService).updateConfig(eq(request), eq("ADMIN"), eq(7L));
    }
}
