package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0011 - ket qua phan trang danh sach nhat ky an uong. */
public record FoodDiaryListResponse(
        List<FoodDiaryEntryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
