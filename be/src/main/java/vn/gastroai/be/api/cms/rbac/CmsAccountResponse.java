package vn.gastroai.be.api.cms.rbac;

import vn.gastroai.be.domain.admin.CmsRole;

import java.time.Instant;

/** Tai khoan noi bo duoc hien thi trong trang phan quyen. */
public record CmsAccountResponse(
        Long id,
        String email,
        String fullName,
        CmsRole role,
        Instant createdAt,
        boolean adminLocked
) {
}
