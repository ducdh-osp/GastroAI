package vn.gastroai.be.api.patient;

import java.time.Instant;

/** UC0013 - 1 lan ghi nhan tinh trang tieu hoa theo thang Bristol. */
public record BristolLogResponse(
        Long id,
        Instant loggedAt,
        Integer bristolType,
        String notes
) {
}
