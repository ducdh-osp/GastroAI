package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0020 - thung rac nhat ky Bristol, co phan trang. */
public record BristolTrashPage(
        List<BristolTrashItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}