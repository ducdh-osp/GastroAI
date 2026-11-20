package vn.gastroai.be.api.cms.triage;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.gastroai.be.domain.triage.TriageContactResult;

/** Admin ghi lai ket qua lien he o buoc "Can can thiep" (SCR-AD-13 buoc 3). */
public record TriageAlertContactRequest(
        @NotNull(message = "Vui lòng chọn kết quả liên hệ")
        TriageContactResult result,
        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note
) {
}
