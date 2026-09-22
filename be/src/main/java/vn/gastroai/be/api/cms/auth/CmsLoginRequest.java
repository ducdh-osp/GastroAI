package vn.gastroai.be.api.cms.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.gastroai.be.domain.admin.CmsRole;

public record CmsLoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password,

        @NotNull
        CmsRole role

) {
}

