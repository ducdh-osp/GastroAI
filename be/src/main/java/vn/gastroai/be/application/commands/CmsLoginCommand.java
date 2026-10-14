package vn.gastroai.be.application.commands;

import vn.gastroai.be.domain.admin.CmsAccountType;

/** Input cho CmsAuthService.login() — tương đương LoginCommand nhưng có thêm role (ADMIN/DOCTOR). */
public record CmsLoginCommand(
        String email,
        String password,
        CmsAccountType accountType
) {
}

