package vn.gastroai.be.api.notification;

import java.time.Instant;

/** UC0016 - 1 dong trong "bang ghi nhan" lich su xac nhan da uong, kem ten thuoc de FE
 * khong phai tu goi them API rieng tra cuu reminder. */
public record MedicationConfirmationDetail(
        Long id,
        Long reminderId,
        String medicineName,
        Instant confirmedAt
) {
}
