package vn.gastroai.be.api.patient;

import java.time.Instant;

/** UC0014 - 1 diem tren bieu do xu huong Bristol: moi lan ghi nhan la 1 diem, khong gop nhom. */
public record BristolLogPoint(
        Instant loggedAt,
        Integer bristolType
) {
}
