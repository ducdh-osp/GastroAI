package vn.gastroai.be.api.cms.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.auth.CmsAuthService;
import vn.gastroai.be.application.commands.CmsLoginCommand;
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
            HttpServletRequest httpRequest) {

        return cmsAuthService.login(
                new CmsLoginCommand(
                        request.email(),
                        request.password(),
                        request.role()),
                httpRequest);
    }

    @PostMapping("/api/v1/cms/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        cmsAuthService.logout(request);
    }
}
