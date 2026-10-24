package vn.gastroai.be.api.cms.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import vn.gastroai.be.domain.catalog.CatalogType;

/** UC0060 - Request payload cho them/sua danh muc chuan hoa. */
public record CmsCatalogItemRequest(
        @NotNull(message = "Loại danh mục không được để trống")
        CatalogType type,

        @NotBlank(message = "Mã chuẩn hóa không được để trống")
        @Size(max = 80, message = "Mã chuẩn hóa tối đa 80 ký tự")
        String code,

        @NotBlank(message = "Tên hiển thị không được để trống")
        @Size(max = 200, message = "Tên hiển thị tối đa 200 ký tự")
        String name,

        @Size(max = 80, message = "Mã Triage tối đa 80 ký tự")
        String triageCode,

        @PositiveOrZero(message = "Thứ tự sắp xếp phải lớn hơn hoặc bằng 0")
        Integer sortOrder,

        Boolean active
) {
}
