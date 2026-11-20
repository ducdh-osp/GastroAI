package vn.gastroai.be.api.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.application.notification.PatientNotificationSettingService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.notification.NotificationRecipientType;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientNotificationController.class)
@Import(SecurityConfig.class)
class PatientNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppNotificationService notificationService;

    @MockitoBean
    private PatientNotificationSettingService settingService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "42", roles = "PATIENT")
    void deleteCollectionDeletesAllNotificationsWithoutParsingAPathId() throws Exception {
        mockMvc.perform(delete("/api/v1/patient/notifications").with(csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteAll(NotificationRecipientType.PATIENT, 42L);
    }

    @Test
    @WithMockUser(username = "42", roles = "PATIENT")
    void deleteItemStillUsesNumericNotificationId() throws Exception {
        mockMvc.perform(delete("/api/v1/patient/notifications/7").with(csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).delete(7L, NotificationRecipientType.PATIENT, 42L);
    }
}
