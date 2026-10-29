package vn.gastroai.be.api.cms.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.cms.dashboard.DashboardAdminService;
import vn.gastroai.be.application.cms.dashboard.DashboardExportService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardAdminController.class)
@Import(SecurityConfig.class)
class DashboardAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardAdminService dashboardAdminService;

    @MockitoBean
    private DashboardExportService dashboardExportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private DashboardStatsResponse mockResponse() {
        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                5L, 30L, 60L, 10L, 5L, 2L, 2L, 1L, 25L, 3L, 20L, 2L, 90.9
        );
        return new DashboardStatsResponse(
                summary,
                List.of(new DailyChatMetric("2026-10-29", 10L, 1L, 5L, 5L)),
                List.of(new SymptomMetric("HEARTBURN", "Ợ nóng", 15L, 0L, 60.0)),
                List.of(new TriageGroupMetric("XUAT_HUYET_TIEU_HOA", "Xuất huyết tiêu hóa", 3L, 60.0)),
                List.of(new TriageStatusMetric("NEW", "Mới phát sinh", 2L, 40.0)),
                30,
                "2026-09-30",
                "2026-10-29"
        );
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getStats_returnsOkForAdmin() throws Exception {
        when(dashboardAdminService.getStats(30)).thenReturn(mockResponse());

        mockMvc.perform(get("/api/v1/cms/dashboard/stats?days=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalPatients").value(5))
                .andExpect(jsonPath("$.summary.totalMessages").value(60))
                .andExpect(jsonPath("$.topSymptoms[0].symptomCode").value("HEARTBURN"));
    }

    @Test
    @WithMockUser(username = "2", roles = "PATIENT")
    void getStats_rejectsPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/dashboard/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void exportReport_returnsExcel() throws Exception {
        when(dashboardAdminService.getStats(30)).thenReturn(mockResponse());
        when(dashboardExportService.exportExcel(any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/v1/cms/dashboard/export?format=EXCEL&days=30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void exportReport_returnsPdf() throws Exception {
        when(dashboardAdminService.getStats(30)).thenReturn(mockResponse());
        when(dashboardExportService.exportPdf(any())).thenReturn(new byte[]{4, 5, 6});

        mockMvc.perform(get("/api/v1/cms/dashboard/export?format=PDF&days=30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
