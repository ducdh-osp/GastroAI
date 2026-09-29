package vn.gastroai.be.api.notification;

import java.util.List;

/** UC0016 - ket qua phan trang "bang ghi nhan" xac nhan da uong. */
public record MedicationConfirmationListResponse(
        List<MedicationConfirmationDetail> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
