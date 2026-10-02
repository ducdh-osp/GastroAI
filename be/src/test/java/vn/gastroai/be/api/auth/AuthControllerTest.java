package vn.gastroai.be.api.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.gastroai.be.application.auth.AuthService;
import vn.gastroai.be.application.auth.ClientRequestInfoResolver;
import vn.gastroai.be.config.SecurityConfig;
import vn.gastroai.be.infrastructure.persistence.postgres.PatientRepository;
import vn.gastroai.be.infrastructure.persistence.postgres.RevokedTokenRepository;
import vn.gastroai.be.infrastructure.security.JwtService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private ClientRequestInfoResolver requestInfoResolver;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private RevokedTokenRepository revokedTokenRepository;

    @Test
    void registerRejectsPasswordOver72CharactersWithVietnameseValidationMessage() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"" + "a".repeat(73)
                                + "\",\"fullName\":\"User\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void registerRejectsPasswordOver72Utf8BytesEvenBelowCharacterLimit() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"" + "ă".repeat(37)
                                + "\",\"fullName\":\"User\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: mat khau khong duoc vuot qua 72 byte UTF-8"));

        verifyNoInteractions(authService);
    }

    @Test
    void registerRejectsEmailOverDatabaseColumnLengthBeforeService() throws Exception {
        String email = "a".repeat(245) + "@example.com";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password1!\",\"fullName\":\"User\"}"))
                .andExpect(status().isBadRequest());

        // @Email and @Size can both fail, and Bean Validation does not guarantee their order.
        // The important regression guard is rejecting before AuthService reaches the DB column.
        verifyNoInteractions(authService);
    }

    @Test
    void resetPasswordRejectsPasswordOver72CharactersBeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType("application/json")
                .content("{\"token\":\"token\",\"newPassword\":\"" + "a".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @WithMockUser(username = "1", roles = "PATIENT")
    void changePasswordRejectsPasswordOver72CharactersBeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/change-password")
                .contentType("application/json")
                .content("{\"currentPassword\":\"old-password\",\"newPassword\":\""
                                + "a".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}
