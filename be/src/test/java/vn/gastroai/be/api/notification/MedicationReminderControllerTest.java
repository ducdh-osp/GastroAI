package vn.gastroai.be.api.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.notification.MedicationReminderService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MedicationReminderController.class)
@Import(SecurityConfig.class)
class MedicationReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicationReminderService medicationReminderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void createRejectsBlankMedicineName() throws Exception {
        mockMvc.perform(post("/api/v1/patient/medications")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"medicineName\":\"\",\"dosage\":\"20mg\",\"timeOfDay\":\"08:00:00\",\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void listConfirmationsRejectsSizeAboveHundred() throws Exception {
        mockMvc.perform(get("/api/v1/patient/medications/confirmations").with(csrf()).param("size", "999"))
                .andExpect(status().isBadRequest());
    }

}
