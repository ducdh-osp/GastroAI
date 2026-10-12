package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0012 - xu huong nhat ky an uong theo ngay, dung ve bieu do o FE. */
public record FoodDiaryTrendResponse(
        List<DailyCountPoint> points
) {
}
