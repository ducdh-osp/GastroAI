package vn.gastroai.be.api.cms.rbac;

import jakarta.validation.constraints.NotNull;
import vn.gastroai.be.domain.admin.CmsRole;

public record UpdateCmsAccountRoleRequest(@NotNull CmsRole role) {
}
