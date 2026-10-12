package vn.gastroai.be.api.patient;

import java.time.LocalDate;

/** UC0025 - 1 ngay trong cua so theo doi: co ghi nhat ky (an uong hoac Bristol) hay khong. */
public record AdherenceDayPoint(
        LocalDate date,
        long foodDiaryCount,
        long bristolCount,
        boolean logged
) {
}
