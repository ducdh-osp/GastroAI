package vn.gastroai.be.api.cms.triage;

import org.springframework.data.domain.Page;
import vn.gastroai.be.application.triage.TriageAlertResponse;

import java.util.List;

public record TriageAlertPageResponse(
        List<TriageAlertResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static TriageAlertPageResponse from(Page<TriageAlertResponse> page) {
        return new TriageAlertPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
