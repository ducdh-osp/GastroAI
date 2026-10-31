package vn.gastroai.be.api.cms.evaluation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.evaluation.RagEvaluationService;
import vn.gastroai.be.application.evaluation.RagEvaluationService.RagEvaluationReport;
import vn.gastroai.be.application.evaluation.TriageEvaluationService;
import vn.gastroai.be.application.evaluation.TriageEvaluationService.TriageEvaluationReport;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CmsEvaluationController.class)
@Import(SecurityConfig.class)
@DisplayName("CmsEvaluationController HTTP & Role-Gate Tests")
class CmsEvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriageEvaluationService triageEvaluationService;

    @MockitoBean
    private RagEvaluationService ragEvaluationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    private TriageEvaluationReport mockTriageReport() {
        return new TriageEvaluationReport(
                20, 15, 5, 15, 0, 1, 4,
                1.0, 0.20, 0.80, 0.9375, 0.95, 0.9677, 0.9868,
                List.of(), List.of(), Instant.now()
        );
    }

    private RagEvaluationReport mockRagReport() {
        return new RagEvaluationReport(
                22, 5, 20, 22, 22,
                0.9091, 1.0, 1.0, 0.9545,
                0.80, 1.0, 0.875, 45.2,
                "VECTOR", "DATABASE", null,
                List.of(), Instant.now()
        );
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getSummary_returnsOkForAdmin() throws Exception {
        when(triageEvaluationService.getLatestReport()).thenReturn(mockTriageReport());
        when(ragEvaluationService.getLatestReport()).thenReturn(mockRagReport());

        mockMvc.perform(get("/api/v1/cms/evaluation/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triageSensitivityRecall").value(1.0))
                .andExpect(jsonPath("$.ragHitRateAt1").value(0.9091))
                .andExpect(jsonPath("$.ragRetrievalMode").value("VECTOR"))
                .andExpect(jsonPath("$.ragDataSource").value("DATABASE"));
    }

    @Test
    @WithMockUser(username = "2", roles = "DOCTOR")
    void getSummary_returnsOkForDoctor() throws Exception {
        when(triageEvaluationService.getLatestReport()).thenReturn(mockTriageReport());
        when(ragEvaluationService.getLatestReport()).thenReturn(mockRagReport());

        mockMvc.perform(get("/api/v1/cms/evaluation/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.triageSensitivityRecall").value(1.0))
                .andExpect(jsonPath("$.ragMrr").value(0.9545));
    }

    @Test
    @WithMockUser(username = "3", roles = "PATIENT")
    void getSummary_rejectsPatientRole() throws Exception {
        mockMvc.perform(get("/api/v1/cms/evaluation/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSummary_rejectsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/cms/evaluation/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getTriageReport_returnsOkForAdmin() throws Exception {
        when(triageEvaluationService.getLatestReport()).thenReturn(mockTriageReport());

        mockMvc.perform(get("/api/v1/cms/evaluation/triage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(20))
                .andExpect(jsonPath("$.sensitivityRecall").value(1.0));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void runTriageEvaluation_invokesServiceAndReturnsReport() throws Exception {
        when(triageEvaluationService.evaluate()).thenReturn(mockTriageReport());

        mockMvc.perform(post("/api/v1/cms/evaluation/triage/run").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensitivityRecall").value(1.0));

        verify(triageEvaluationService).evaluate();
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void getRagReport_returnsOkForAdmin() throws Exception {
        when(ragEvaluationService.getLatestReport()).thenReturn(mockRagReport());

        mockMvc.perform(get("/api/v1/cms/evaluation/rag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQueries").value(22))
                .andExpect(jsonPath("$.hitRateAt1").value(0.9091))
                .andExpect(jsonPath("$.retrievalMode").value("VECTOR"))
                .andExpect(jsonPath("$.dataSource").value("DATABASE"));
    }

    @Test
    @WithMockUser(username = "1", roles = "ADMIN")
    void runRagEvaluation_passesTopKParameterToService() throws Exception {
        when(ragEvaluationService.evaluate(eq(7))).thenReturn(mockRagReport());

        mockMvc.perform(post("/api/v1/cms/evaluation/rag/run").with(csrf()).param("topK", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hitRateAt1").value(0.9091));

        verify(ragEvaluationService).evaluate(7);
    }

    @Test
    @WithMockUser(username = "3", roles = "PATIENT")
    void runRagEvaluation_rejectsPatientRole() throws Exception {
        mockMvc.perform(post("/api/v1/cms/evaluation/rag/run").with(csrf()).param("topK", "5"))
                .andExpect(status().isForbidden());
    }
}
