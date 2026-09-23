package vn.gastroai.be.api.cms.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.gastroai.be.domain.admin.CmsRole;

/** UC0045 - body của POST /api/v1/cms/auth/login. FE phải tự chọn role (ADMIN/DOCTOR). */
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

