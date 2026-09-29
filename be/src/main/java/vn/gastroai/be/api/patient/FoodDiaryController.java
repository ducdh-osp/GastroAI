package vn.gastroai.be.api.patient;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.patient.FoodDiaryService;

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
        requireAuthenticated(authentication);
        return foodDiaryService.create(Long.valueOf(principal.getName()), request);
    }

    @GetMapping
    public FoodDiaryListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return foodDiaryService.list(Long.valueOf(principal.getName()), page, size);
    }

    @GetMapping("/trend")
    public FoodDiaryTrendResponse trend(
            @RequestParam(defaultValue = "30") int days,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return foodDiaryService.trend(Long.valueOf(principal.getName()), days);
    }

    @PutMapping("/{id}")
    public FoodDiaryEntryResponse update(
            @PathVariable Long id,
            @Valid @RequestBody FoodDiaryEntryRequest request,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        return foodDiaryService.update(Long.valueOf(principal.getName()), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Principal principal, Authentication authentication) {
        requireAuthenticated(authentication);
        foodDiaryService.delete(Long.valueOf(principal.getName()), id);
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new org.springframework.security.access.AccessDeniedException("Chua dang nhap");
        }
    }
}
