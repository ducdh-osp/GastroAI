package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0020 - thung rac nhat ky an uong, co phan trang. */
public record FoodDiaryTrashPage(
        List<FoodDiaryTrashItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}