package vn.gastroai.be.api.patient;

import jakarta.validation.constraints.NotNull;
import vn.gastroai.be.domain.triage.TriagePatientResponse;

/** Benh nhan tra loi "Ban co on khong?": SAFE (An toan) hoac NEED_HELP (Can ho tro). */
public record TriageCheckInRequest(
        @NotNull(message = "Vui lòng chọn An toàn hoặc Cần hỗ trợ")
        TriagePatientResponse response
) {
}
