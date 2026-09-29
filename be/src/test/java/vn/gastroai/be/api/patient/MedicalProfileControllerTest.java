package vn.gastroai.be.api.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.patient.MedicalProfileService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicalProfileController.class)
@Import(SecurityConfig.class)
class MedicalProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicalProfileService medicalProfileService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void rejectsHeightOutOfRange() throws Exception {
        mockMvc.perform(put("/api/v1/patient/medical-profile")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"heightCm\":999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void rejectsFutureDateOfBirth() throws Exception {
        mockMvc.perform(put("/api/v1/patient/medical-profile")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"dateOfBirth\":\"2099-01-01\"}"))
                .andExpect(status().isBadRequest());
    }
}
