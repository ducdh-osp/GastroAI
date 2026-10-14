package vn.gastroai.be.api.cms.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.config.AppRuntimeConfigService;
import vn.gastroai.be.application.config.AppRuntimeConfigSnapshot;
import vn.gastroai.be.application.config.UpdateAppRuntimeConfigRequest;

/** UC0059 - chỉ Admin (không có Bác sĩ, xem masterplan Actor) được xem/sửa cấu hình RAG/Triage. */
@RestController
@RequestMapping("/api/v1/cms/runtime-config")
public class AppRuntimeConfigAdminController {

    private final AppRuntimeConfigService appRuntimeConfigService;

    public AppRuntimeConfigAdminController(AppRuntimeConfigService appRuntimeConfigService) {
        this.appRuntimeConfigService = appRuntimeConfigService;
    }

    @GetMapping
    public AppRuntimeConfigSnapshot get() {
        return appRuntimeConfigService.getConfig();
    }

    @PutMapping
    public AppRuntimeConfigSnapshot update(
            @RequestBody UpdateAppRuntimeConfigRequest request,
            Authentication authentication) {
        Long adminId = Long.valueOf(authentication.getName());
        return appRuntimeConfigService.updateConfig(request, "ADMIN", adminId);
    }
}
