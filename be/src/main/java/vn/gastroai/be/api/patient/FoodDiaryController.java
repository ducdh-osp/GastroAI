package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.patient.FoodDiaryService;
import vn.gastroai.be.api.support.AuthenticatedRequest;

import java.security.Principal;

/** UC0011 (CRUD nhat ky an uong) + UC0012 (xem lich su/bieu do). */
@RestController
@RequestMapping("/api/v1/patient/food-diary")
public class FoodDiaryController {

    private final FoodDiaryService foodDiaryService;

    public FoodDiaryController(FoodDiaryService foodDiaryService) {
        this.foodDiaryService = foodDiaryService;
    }

    @PostMapping
    public FoodDiaryEntryResponse create(
            @Valid @RequestBody FoodDiaryEntryRequest request,
            Principal principal, Authentication authentication) {
        return foodDiaryService.create(AuthenticatedRequest.patientId(principal, authentication), request);
    }

    @GetMapping
    public FoodDiaryListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        // Cung gioi han nhu MeController.history() - chan client tu gui size cuc lon lam
        // qua tai truy van/response (vd size=2000000000).
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page >= 0 va size trong khoang 1..100");
        }
        return foodDiaryService.list(patientId, page, size);
    }

    @GetMapping("/trend")
    public FoodDiaryTrendResponse trend(
            @RequestParam(defaultValue = "30") int days,
            Principal principal, Authentication authentication) {
        Long patientId = AuthenticatedRequest.patientId(principal, authentication);
        // Gioi han tren de tranh client gui days cuc lon lam vong lap fill-0 va ket qua
        // JSON phinh to khong kiem soat (vd days=100000000).
        if (days < 1 || days > 365) {
            throw new IllegalArgumentException("days trong khoang 1..365");
        }
        return foodDiaryService.trend(patientId, days);
    }

    @PutMapping("/{id}")
    public FoodDiaryEntryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FoodDiaryEntryRequest request,
            Principal principal, Authentication authentication) {
        return foodDiaryService.update(AuthenticatedRequest.patientId(principal, authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Principal principal, Authentication authentication) {
        foodDiaryService.delete(AuthenticatedRequest.patientId(principal, authentication), id);
    }
}
