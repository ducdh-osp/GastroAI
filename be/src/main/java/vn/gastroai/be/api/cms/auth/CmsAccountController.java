package vn.gastroai.be.api.cms.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.auth.ChangePasswordRequest;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.domain.admin.CmsAccountType;

/** Tai khoan CMS dang dang nhap tu quan ly (doi mat khau). Khong dat duoi /cms/auth/** vi duong do permitAll. */
@RestController
public class CmsAccountController {

    private final CmsAuthService cmsAuthService;

    public CmsAccountController(CmsAuthService cmsAuthService) {
        this.cmsAuthService = cmsAuthService;
    }

    @PostMapping("/api/v1/cms/account/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(CmsAuthService.AUTH_USER_ID);
        Object accountType = session == null ? null : session.getAttribute(CmsAuthService.AUTH_ACCOUNT_TYPE);
        if (userId == null || accountType == null) {
            throw new AccessDeniedException("Chua dang nhap");
        }

        cmsAuthService.changePassword(
                CmsAccountType.valueOf(accountType.toString()),
                Long.valueOf(userId.toString()),
                body.currentPassword(),
                body.newPassword(),
                request);

        // Service da commit xong moi huy phien hien tai - commit loi thi nguoi dung van dang nhap.
        session.invalidate();
    }
}