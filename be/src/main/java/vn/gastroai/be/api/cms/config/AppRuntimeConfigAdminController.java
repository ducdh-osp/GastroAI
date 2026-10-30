package vn.gastroai.be.api.cms.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.audit.AuditLogService;
import vn.gastroai.be.application.config.AppRuntimeConfigService;
import vn.gastroai.be.application.config.AppRuntimeConfigSnapshot;
import vn.gastroai.be.application.config.UpdateAppRuntimeConfigRequest;
import vn.gastroai.be.domain.audit.AuditAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** UC0059 - chỉ Admin (không có Bác sĩ, xem masterplan Actor) được xem/sửa cấu hình RAG/Triage. */
@RestController
@RequestMapping("/api/v1/cms/runtime-config")
public class AppRuntimeConfigAdminController {

    private final AppRuntimeConfigService appRuntimeConfigService;
    private final AuditLogService auditLogService;

    public AppRuntimeConfigAdminController(AppRuntimeConfigService appRuntimeConfigService,
                                           AuditLogService auditLogService) {
        this.appRuntimeConfigService = appRuntimeConfigService;
        this.auditLogService = auditLogService;
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
        AppRuntimeConfigSnapshot before = appRuntimeConfigService.getConfig();
        AppRuntimeConfigSnapshot after = appRuntimeConfigService.updateConfig(request, "ADMIN", adminId);
        auditLogService.record(AuditAction.RUNTIME_CONFIG_UPDATE, "CONFIG", null, describeChanges(before, after));
        return after;
    }

    static String describeChanges(AppRuntimeConfigSnapshot before, AppRuntimeConfigSnapshot after) {
        List<String> changes = new ArrayList<>();
        if (before.ragTopK() != after.ragTopK()) {
            changes.add("Số đoạn RAG (top-k): " + before.ragTopK() + " → " + after.ragTopK());
        }
        if (!Objects.equals(before.ragSimilarityThreshold(), after.ragSimilarityThreshold())) {
            changes.add("Ngưỡng tương đồng: " + similarity(before.ragSimilarityThreshold())
                    + " → " + similarity(after.ragSimilarityThreshold()));
        }
        if (before.triageRealtimeAlertsEnabled() != after.triageRealtimeAlertsEnabled()) {
            changes.add("Cảnh báo triage realtime: " + onOff(before.triageRealtimeAlertsEnabled())
                    + " → " + onOff(after.triageRealtimeAlertsEnabled()));
        }
        return changes.isEmpty() ? "Lưu cấu hình, không thay đổi giá trị" : String.join("; ", changes);
    }

    private static String similarity(Double value) {
        return value == null ? "tắt" : value.toString();
    }

    private static String onOff(boolean value) {
        return value ? "bật" : "tắt";
    }
}