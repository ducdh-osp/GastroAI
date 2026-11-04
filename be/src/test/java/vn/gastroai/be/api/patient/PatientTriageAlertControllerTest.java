package vn.gastroai.be.api.patient;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.triage.TriageAlertConversationService;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientTriageAlertController.class)
@Import(SecurityConfig.class)
class PatientTriageAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TriageAlertConversationService conversationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientSeesOwnConversation() throws Exception {
        when(conversationService.getForPatient(1L, 10L)).thenReturn(
                new TriageAlertConversationService.PatientConversationView(10L, "IN_PROGRESS",
                        Instant.parse("2026-09-30T00:00:00Z"),
                        List.of(new TriageAlertConversationService.PatientMessageItem(
                                77L, "Bác sĩ", false, "Ban hay den vien", Instant.parse("2026-09-30T00:06:00Z"))),
                        true));

        mockMvc.perform(get("/api/v1/patient/triage-alerts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canReply").value(true))
                .andExpect(jsonPath("$.messages[0].senderLabel").value("Bác sĩ"))
                .andExpect(jsonPath("$.messages[0].senderId").doesNotExist());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void otherPatientsAlertIsNotFound() throws Exception {
        when(conversationService.getForPatient(1L, 99L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy cảnh báo"));

        mockMvc.perform(get("/api/v1/patient/triage-alerts/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientReplyPassesContent() throws Exception {
        when(conversationService.replyAsPatient(1L, 10L, "Toi dang di"))
                .thenReturn(new TriageAlertConversationService.PatientMessageItem(
                        78L, "Bạn", true, "Toi dang di", Instant.parse("2026-09-30T00:07:00Z")));

        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/messages").with(csrf())
                        .contentType("application/json").content("{\"content\":\"Toi dang di\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mine").value(true));
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientReplyBeforeStaffMessageIsConflict() throws Exception {
        when(conversationService.replyAsPatient(1L, 10L, "alo"))
                .thenThrow(new IllegalStateException("Bác sĩ chưa liên hệ nên bạn chưa thể gửi tin nhắn tại đây"));

        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/messages").with(csrf())
                        .contentType("application/json").content("{\"content\":\"alo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void patientReplyRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/messages").with(csrf())
                        .contentType("application/json").content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(conversationService, never()).replyAsPatient(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "9", roles = "DOCTOR")
    void cmsRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/patient/triage-alerts/10"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/patient/triage-alerts/10/messages").with(csrf())
                        .contentType("application/json").content("{\"content\":\"alo\"}"))
                .andExpect(status().isForbidden());

        verify(conversationService, never()).getForPatient(any(), any());
        verify(conversationService, never()).replyAsPatient(any(), any(), any());
    }

    @Test
    void anonymousIsRejected() throws Exception {
        // SecurityConfig hien tra 403 cho request chua dang nhap.
        mockMvc.perform(get("/api/v1/patient/triage-alerts/10"))
                .andExpect(status().isForbidden());

        verify(conversationService, never()).getForPatient(any(), any());
    }
}
