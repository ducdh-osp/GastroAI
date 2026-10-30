package vn.gastroai.be.api.cms.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.notification.EmailOutboxService;
import vn.gastroai.be.application.notification.EmailTemplateContent;
import vn.gastroai.be.application.notification.EmailTemplateService;
import vn.gastroai.be.application.notification.EmailTemplateView;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.domain.notification.EmailTemplateCode;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailTemplateAdminController.class)
@Import(SecurityConfig.class)
class EmailTemplateAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailTemplateService templateService;

    @MockitoBean
    private EmailOutboxService outboxService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void adminCanListTemplates() throws Exception {
        when(templateService.list()).thenReturn(List.of(view()));

        mockMvc.perform(get("/api/v1/cms/email-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("ACCOUNT_VERIFICATION"))
                .andExpect(jsonPath("$[0].allowedVariables[0]").exists());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void doctorCannotReadTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/cms/email-templates"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(templateService);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void updatePassesAdminIdAndVersion() throws Exception {
        EmailTemplateContent expectedContent = new EmailTemplateContent(
                "Tiêu đề mới", "<p>{{verificationUrl}}</p>", "{{verificationUrl}}");
        when(templateService.update(eq(EmailTemplateCode.ACCOUNT_VERIFICATION), eq(expectedContent),
                eq(2L), eq("ADMIN"), eq(7L))).thenReturn(view());

        mockMvc.perform(put("/api/v1/cms/email-templates/ACCOUNT_VERIFICATION")
                        .contentType("application/json")
                        .content("""
                                {"subjectTemplate":"Tiêu đề mới",
                                 "htmlBodyTemplate":"<p>{{verificationUrl}}</p>",
                                 "textBodyTemplate":"{{verificationUrl}}","version":2}
                                """))
                .andExpect(status().isOk());

        verify(templateService).update(EmailTemplateCode.ACCOUNT_VERIFICATION,
                expectedContent, 2L, "ADMIN", 7L);
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMIN")
    void sendTestValidatesRecipientAndQueuesEmail() throws Exception {
        mockMvc.perform(post("/api/v1/cms/email-templates/PASSWORD_RESET/test")
                        .contentType("application/json")
                        .content("{\"recipientEmail\":\"admin@example.com\"}"))
                .andExpect(status().isAccepted());

        verify(outboxService).enqueueTest(EmailTemplateCode.PASSWORD_RESET, "admin@example.com");

        mockMvc.perform(post("/api/v1/cms/email-templates/PASSWORD_RESET/test")
                        .contentType("application/json")
                        .content("{\"recipientEmail\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());
    }

    private EmailTemplateView view() {
        return new EmailTemplateView(
                "ACCOUNT_VERIFICATION", "Xác thực tài khoản", "Tiêu đề",
                "<p>{{verificationUrl}}</p>", "{{verificationUrl}}",
                List.of("userName", "verificationUrl"), 2L,
                Instant.parse("2026-10-29T10:00:00Z"), "ADMIN", 7L);
    }
}
