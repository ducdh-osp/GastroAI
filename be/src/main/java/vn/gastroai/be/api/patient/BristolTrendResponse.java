package vn.gastroai.be.api.patient;

import java.util.List;

/** UC0014 - xu huong Bristol theo thoi gian, dung ve bieu do o FE. */
public record BristolTrendResponse(
        List<BristolLogPoint> points
) {
}
