package vn.gastroai.be.api.cms.user;

import org.springframework.data.domain.Page;

import java.util.List;

public record CmsPatientPageResponse(
        List<CmsPatientResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CmsPatientPageResponse from(Page<CmsPatientResponse> result) {
        return new CmsPatientPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }
}
