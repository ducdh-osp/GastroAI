package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.patient.BristolLogService;

import java.security.Principal;

/** UC0013 (ghi nhan Bristol) + UC0014 (xem xu huong theo thoi gian). */
@RestController
@RequestMapping("/api/v1/patient/bristol-logs")
public class BristolLogController {

    private final BristolLogService bristolLogService;

    public BristolLogController(BristolLogService bristolLogService) {
        this.bristolLogService = bristolLogService;
    }

    @PostMapping
    public BristolLogResponse create(
            @Valid @RequestBody BristolLogRequest request,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return bristolLogService.create(Long.valueOf(principal.getName()), request);
    }

    @GetMapping
    public BristolListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        return bristolLogService.list(Long.valueOf(principal.getName()), page, size);
    }

    @GetMapping("/trend")
    public BristolTrendResponse trend(
            @RequestParam(defaultValue = "30") int days,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("days trong khoang 1..365");
        }
        return bristolLogService.trend(Long.valueOf(principal.getName()), days);
    }

    @PutMapping("/{id}")
    public BristolLogResponse update(
            @PathVariable Long id,
            @Valid @RequestBody BristolLogRequest request,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return bristolLogService.update(Long.valueOf(principal.getName()), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        bristolLogService.delete(Long.valueOf(principal.getName()), id);
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
    }
}
