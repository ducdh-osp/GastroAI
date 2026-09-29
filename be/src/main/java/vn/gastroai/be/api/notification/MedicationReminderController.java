package vn.gastroai.be.api.notification;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.notification.MedicationReminderService;

import java.security.Principal;
import java.util.List;

/**
 * UC0015 (lich nhac uong thuoc) + UC0016 (xac nhan da uong). Package Java la
 * api.notification (khop domain.notification da pre-scaffold), nhung URL van dat duoi
 * /api/v1/patient/** de tan dung rule Security co san (hasRole("PATIENT")) - khong can
 * sua SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/patient/medications")
public class MedicationReminderController {

    private final MedicationReminderService medicationReminderService;

    public MedicationReminderController(MedicationReminderService medicationReminderService) {
        this.medicationReminderService = medicationReminderService;
    }

    @PostMapping
    public MedicationReminderResponse create(
            @Valid @RequestBody MedicationReminderRequest request,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return medicationReminderService.create(Long.valueOf(principal.getName()), request);
    }

    @GetMapping
    public List<MedicationReminderResponse> list(Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return medicationReminderService.list(Long.valueOf(principal.getName()));
    }

    @PutMapping("/{id}")
    public MedicationReminderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody MedicationReminderRequest request,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return medicationReminderService.update(Long.valueOf(principal.getName()), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        medicationReminderService.delete(Long.valueOf(principal.getName()), id);
    }

    @PostMapping("/{id}/confirmations")
    public MedicationConfirmationResponse confirmDose(
            @PathVariable Long id,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return medicationReminderService.confirmDose(Long.valueOf(principal.getName()), id);
    }

    @GetMapping("/confirmations")
    public MedicationConfirmationListResponse listConfirmations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        return medicationReminderService.listConfirmations(Long.valueOf(principal.getName()), page, size);
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
    }
}
