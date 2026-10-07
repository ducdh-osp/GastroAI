package vn.gastroai.be.api.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.patient.HealthReportService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cung ly do @Import(SecurityConfig.class) nhu FoodDiaryControllerTest - @WebMvcTest khong tu
// nap SecurityConfig that cua app.
@WebMvcTest(HealthReportController.class)
@Import(SecurityConfig.class)
class HealthReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthReportService healthReportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void exportPdfReturnsPdfWithAttachmentHeaderOnSuccess() throws Exception {
        byte[] fakePdf = "%PDF-fake".getBytes();
        when(healthReportService.export(anyLong(), any(), any())).thenReturn(fakePdf);

        mockMvc.perform(get("/api/v1/patient/health-report/pdf")
                        .with(csrf())
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"nhat-ky-suc-khoe_2026-09-01_2026-09-30.pdf\""))
                .andExpect(content().bytes(fakePdf));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void exportPdfReturnsBadRequestWhenServiceRejectsRange() throws Exception {
        when(healthReportService.export(anyLong(), any(), any()))
                .thenThrow(new IllegalArgumentException("Chỉ xuất tối đa 90 ngày mỗi lần"));

        mockMvc.perform(get("/api/v1/patient/health-report/pdf")
                        .with(csrf())
                        .param("from", "2026-01-01")
                        .param("to", "2026-09-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ xuất tối đa 90 ngày mỗi lần"));
    }
    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void exportPdfReturnsBadRequestWhenFromParamMissing() throws Exception {
        mockMvc.perform(get("/api/v1/patient/health-report/pdf")
                        .with(csrf())
                        .param("to", "2026-09-30"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void exportPdfForbiddenForAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/patient/health-report/pdf")
                        .with(csrf())
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30"))
                .andExpect(status().isForbidden());
    }
}