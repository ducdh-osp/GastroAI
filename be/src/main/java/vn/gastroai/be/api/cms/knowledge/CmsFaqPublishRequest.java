package vn.gastroai.be.api.cms.knowledge;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** UC0062 - Bật/tắt xuất bản FAQ. Xuất bản bắt buộc phải có bác sĩ kiểm duyệt y khoa. */
public record CmsFaqPublishRequest(
        @NotNull(message = "Trạng thái published không được để trống")
        Boolean published,

        @Size(max = 200, message = "Tên bác sĩ kiểm duyệt tối đa 200 ký tự")
        String medicalReviewer,

        Long version
) {
    public CmsFaqPublishRequest(Boolean published) {
        this(published, null, null);
    }
}
