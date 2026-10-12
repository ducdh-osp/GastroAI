package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0013 - ket qua phan trang danh sach ghi nhan Bristol. */
public record BristolListResponse(
        List<BristolLogResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
