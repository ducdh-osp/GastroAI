package vn.gastroai.be.api.cms.triage;

import jakarta.validation.constraints.Size;

public record TriageAlertResolveRequest(
        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note
) {
}
