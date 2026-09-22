package vn.gastroai.be.api.cms.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.application.commands.LoginCommand;
import vn.gastroai.be.application.readmodel.CmsAuthResult;

@RestController
public class CmsAuthController {

    private final CmsAuthService cmsAuthService;

    public CmsAuthController(CmsAuthService cmsAuthService) {
        this.cmsAuthService = cmsAuthService;
    }

    @PostMapping("/api/v1/cms/auth/login")
    public CmsAuthResult login(
            @Valid @RequestBody CmsLoginRequest request,
            HttpSession session) {

        return cmsAuthService.login(
                new LoginCommand(
                        request.email(),
                        request.password()),
                session
        );
    }
}