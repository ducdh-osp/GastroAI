package vn.gastroai.be.application.commands;

import vn.gastroai.be.domain.admin.CmsRole;

public record CmsLoginCommand(
        String email,
        String password,
        CmsRole role
) {
}

