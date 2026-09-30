package vn.gastroai.be.api.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.patient.FoodDiaryService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Cung ly do @Import(SecurityConfig.class) nhu ChatControllerTest - @WebMvcTest khong tu nap
// SecurityConfig that cua app.
@WebMvcTest(FoodDiaryController.class)
@Import(SecurityConfig.class)
class FoodDiaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FoodDiaryService foodDiaryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void listRejectsSizeAboveHundred() throws Exception {
        mockMvc.perform(get("/api/v1/patient/food-diary").with(csrf()).param("size", "1000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void trendRejectsDaysAboveMax() throws Exception {
        mockMvc.perform(get("/api/v1/patient/food-diary/trend").with(csrf()).param("days", "100000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void trendRejectsNonPositiveDays() throws Exception {
        mockMvc.perform(get("/api/v1/patient/food-diary/trend").with(csrf()).param("days", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void createRejectsFutureEatenAt() throws Exception {
        mockMvc.perform(post("/api/v1/patient/food-diary")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"eatenAt\":\"2999-01-01T08:00:00Z\",\"description\":\"Pho bo\"}"))
                .andExpect(status().isBadRequest());
    }
}
