package vn.gastroai.be.api.cms.triage;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.triage.TriageAlertResponse;
import vn.gastroai.be.application.triage.TriageAlertService;

import java.util.List;


@RestController
@RequestMapping("/api/v1/cms/triage-alerts")
public class TriageAlertAdminController {

    private final TriageAlertService triageAlertService;

    public TriageAlertAdminController(TriageAlertService triageAlertService) {
        this.triageAlertService = triageAlertService;
    }

    @GetMapping
    public List<TriageAlertResponse> list() {
        return triageAlertService.listRecent();
    }

    @PostMapping("/{alertId}/claim")
    public TriageAlertResponse claim(
            @PathVariable Long alertId,
            Authentication authentication) {

        Long claimerId = Long.valueOf(authentication.getName());
        String claimerType = resolveRole(authentication);
        return triageAlertService.claim(alertId, claimerId, claimerType);
    }

    @PostMapping("/{alertId}/resolve")
    public TriageAlertResponse resolve(
            @PathVariable Long alertId,
            Authentication authentication) {

        Long resolverId = Long.valueOf(authentication.getName());
        String resolverType = resolveRole(authentication);
        return triageAlertService.resolve(alertId, resolverId, resolverType);
    }

    private String resolveRole(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_DOCTOR".equals(authority.getAuthority())) {
                return "DOCTOR";
            }
        }
        return "ADMIN";
    }
}