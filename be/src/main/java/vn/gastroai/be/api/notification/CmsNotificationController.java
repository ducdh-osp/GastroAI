package vn.gastroai.be.api.notification;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.notification.AppNotificationService;
import vn.gastroai.be.domain.notification.NotificationRecipientType;

@RestController
@RequestMapping("/api/v1/cms/notifications")
public class CmsNotificationController {

    private final AppNotificationService service;

    public CmsNotificationController(AppNotificationService service) {
        this.service = service;
    }

    @GetMapping
    public NotificationPageResponse list(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          Authentication authentication) {
        validateCms(authentication);
        validatePage(page, size);
        return service.listAdmin(Long.valueOf(authentication.getName()), PageRequest.of(page, size));
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse unreadCount(Authentication authentication) {
        validateCms(authentication);
        return new UnreadNotificationCountResponse(service.unreadAdmin(Long.valueOf(authentication.getName())));
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id, Authentication authentication) {
        Long adminId = cmsId(authentication);
        service.markRead(id, NotificationRecipientType.ADMIN, adminId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Authentication authentication) {
        service.markAllRead(NotificationRecipientType.ADMIN, cmsId(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        service.delete(id, NotificationRecipientType.ADMIN, cmsId(authentication));
    }

    private Long cmsId(Authentication authentication) {
        validateCms(authentication);
        return Long.valueOf(authentication.getName());
    }

    private void validateCms(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Khong co quyen truy cap");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
    }
}
