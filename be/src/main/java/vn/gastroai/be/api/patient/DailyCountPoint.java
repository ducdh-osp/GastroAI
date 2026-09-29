package vn.gastroai.be.api.patient;

import java.time.LocalDate;

/** UC0012 - 1 diem tren bieu do xu huong an uong: so bua ghi nhan trong 1 ngay. */
public record DailyCountPoint(
        LocalDate date,
        long count
) {
}
