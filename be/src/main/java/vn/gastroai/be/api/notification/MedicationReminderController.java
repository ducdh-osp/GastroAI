package vn.gastroai.be.api.notification;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.notification.MedicationDueNotificationService;
import vn.gastroai.be.application.notification.MedicationReminderService;
import vn.gastroai.be.api.support.AuthenticatedRequest;

import java.security.Principal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * UC0015 (lich nhac uong thuoc) + UC0016 (xac nhan da uong) + UC0068 (nhac uong thuoc in-app/websocket).
 */
@RestController
@RequestMapping("/api/v1/patient/medications")
public class MedicationReminderController {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MedicationReminderService medicationReminderService;
    private final MedicationDueNotificationService medicationDueNotificationService;

    public MedicationReminderController(
            MedicationReminderService medicationReminderService,
            MedicationDueNotificationService medicationDueNotificationService) {
        this.medicationReminderService = medicationReminderService;
        this.medicationDueNotificationService = medicationDueNotificationService;
    }

    @PostMapping
    public MedicationReminderResponse create(
            @Valid @RequestBody MedicationReminderRequest request,
            Principal principal, Authentication authentication) {
        return medicationReminderService.create(AuthenticatedRequest.patientId(principal, authentication), request);
    }

    @GetMapping
    public List<MedicationReminderResponse> list(Principal principal, Authentication authentication) {
        return medicationReminderService.list(AuthenticatedRequest.patientId(principal, authentication));
    }

    @PutMapping("/{id}")
    public MedicationReminderResponse update(
            @PathVariable Long id,
            @Valid @RequestBody MedicationReminderRequest request,
            Principal principal, Authentication authentication) {
        return medicationReminderService.update(AuthenticatedRequest.patientId(principal, authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @RequestParam Long version,
            Principal principal, Authentication authentication) {
        medicationReminderService.delete(AuthenticatedRequest.patientId(principal, authentication), id, version);
    }

    @PostMapping("/{id}/confirmations")
    public MedicationConfirmationResponse confirmDose(
            @PathVariable Long id,
            @Valid @RequestBody MedicationConfirmationRequest request,
            Principal principal, Authentication authentication) {
        return medicationReminderService.confirmDose(
                AuthenticatedRequest.patientId(principal, authentication), id, request);
    }

    @GetMapping("/confirmations")
    public MedicationConfirmationListResponse listConfirmations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        return medicationReminderService.listConfirmations(patientId, page, size);
    }

    @PostMapping("/check-due")
    public Map<String, Object> checkDueDoses(Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        int created = medicationDueNotificationService.notifyDueDosesForPatient(
                patientId, now.toLocalDate(), now.toLocalTime().withSecond(0).withNano(0));
        return Map.of("created", created, "timestamp", now.toInstant().toString());
    }
}
