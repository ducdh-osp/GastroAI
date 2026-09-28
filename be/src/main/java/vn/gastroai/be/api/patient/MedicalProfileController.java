package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.gastroai.be.application.patient.MedicalProfileService;

import java.security.Principal;

/** UC0009 (khai bao) + UC0010 (cap nhat) ho so benh ly ca nhan cua benh nhan dang dang nhap. */
@RestController
@RequestMapping("/api/v1/patient/medical-profile")
public class MedicalProfileController {

    private final MedicalProfileService medicalProfileService;

    public MedicalProfileController(MedicalProfileService medicalProfileService) {
        this.medicalProfileService = medicalProfileService;
    }

    @GetMapping
    public MedicalProfileResponse getProfile(Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        Long patientId = Long.valueOf(principal.getName());
        return medicalProfileService.getProfile(patientId);
    }

    @PutMapping
    public MedicalProfileResponse upsertProfile(
            @Valid @RequestBody MedicalProfileRequest request,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        Long patientId = Long.valueOf(principal.getName());
        return medicalProfileService.upsertProfile(patientId, request);
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
    }
}
