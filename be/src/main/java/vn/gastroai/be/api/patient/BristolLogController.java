package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.patient.BristolLogService;
import vn.gastroai.be.api.support.AuthenticatedRequest;

import java.security.Principal;
import java.util.List;

/** UC0013 (ghi nhan Bristol) + UC0014 (xem xu huong theo thoi gian) + UC0020 (thung rac). */
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
        return bristolLogService.create(AuthenticatedRequest.patientId(principal, authentication), request);
    }

    @GetMapping
    public BristolListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        return bristolLogService.list(patientId, page, size);
    }

    @GetMapping("/trend")
    public BristolTrendResponse trend(
            @RequestParam(defaultValue = "30") int days,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("days trong khoang 1..365");
        }
        return bristolLogService.trend(patientId, days);
    }

    @PutMapping("/{id}")
    public BristolLogResponse update(
            @PathVariable Long id,
            @Valid @RequestBody BristolLogRequest request,
            Principal principal, Authentication authentication) {
        return bristolLogService.update(AuthenticatedRequest.patientId(principal, authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Principal principal, Authentication authentication) {
        bristolLogService.delete(AuthenticatedRequest.patientId(principal, authentication), id);
    }

    @GetMapping("/trash")
    public List<BristolTrashItem> trash(
            Principal principal, Authentication authentication) {
        return bristolLogService.listTrash(AuthenticatedRequest.patientId(principal, authentication));
    }

    @PostMapping("/{id}/restore")
    public BristolLogResponse restore(
            @PathVariable Long id,
            Principal principal, Authentication authentication) {
        return bristolLogService.restore(AuthenticatedRequest.patientId(principal, authentication), id);
    }
}