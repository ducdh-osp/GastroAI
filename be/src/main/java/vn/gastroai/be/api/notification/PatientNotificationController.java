package vn.gastroai.be.api.notification;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.api.support.AuthenticatedRequest;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.domain.notification.NotificationRecipientType;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/patient/notifications")
public class PatientNotificationController {

    private final AppNotificationService service;

    public PatientNotificationController(AppNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationPageResponse list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          Principal principal, Authentication authentication) {
        Long patientId = patientId(principal, authentication);
        validatePage(page, size);
        return service.listPatient(patientId, PageRequest.of(page, size));
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse unreadCount(Principal principal, Authentication authentication) {
        return new UnreadNotificationCountResponse(service.unreadPatient(patientId(principal, authentication)));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, Principal principal, Authentication authentication) {
        service.markRead(id, NotificationRecipientType.PATIENT, patientId(principal, authentication));
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Principal principal, Authentication authentication) {
        service.markAllRead(NotificationRecipientType.PATIENT, patientId(principal, authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal, Authentication authentication) {
        service.delete(id, NotificationRecipientType.PATIENT, patientId(principal, authentication));
    }

    private Long patientId(Principal principal, Authentication authentication) {
        return AuthenticatedRequest.patientId(principal, authentication);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
    }
}
