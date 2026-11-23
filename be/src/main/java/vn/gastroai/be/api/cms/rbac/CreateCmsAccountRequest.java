package vn.gastroai.be.api.cms.rbac;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vn.gastroai.be.domain.admin.CmsRole;

/** SCR-AD-14 - tao tai khoan nhan su CMS moi, chi Admin goi duoc. */
public record CreateCmsAccountRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull CmsRole role,
        @NotBlank String password
) {
}
