package vn.gastroai.be.api.patient;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.api.support.AuthenticatedRequest;
import vn.gastroai.be.application.patient.AdherenceService;

import java.security.Principal;

/** UC0025 - streak va ty le tuan thu ghi nhat ky. */
@RestController
@RequestMapping("/api/v1/patient/adherence")
public class AdherenceController {

    private final AdherenceService adherenceService;

    public AdherenceController(AdherenceService adherenceService) {
        this.adherenceService = adherenceService;
    }

    @GetMapping
    public AdherenceResponse adherence(
            @RequestParam(defaultValue = "30") int days,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("days trong khoang 1..365");
        }
        return adherenceService.compute(patientId, days);
    }
}
