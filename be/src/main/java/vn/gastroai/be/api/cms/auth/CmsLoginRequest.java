package vn.gastroai.be.api.cms.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CmsLoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}