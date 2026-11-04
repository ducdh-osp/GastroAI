package vn.gastroai.be.api.cms.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import vn.gastroai.be.domain.catalog.FodmapLevel;

public record FodmapFoodRequest(
        @NotBlank(message = "Tên món không được để trống")
        @Size(min = 2, max = 120, message = "Tên món từ 2 đến 120 ký tự")
        String name,

        @Size(max = 10, message = "Tối đa 10 tên gọi khác")
        List<@Size(max = 60, message = "Mỗi tên gọi khác tối đa 60 ký tự") String> aliases,

        @NotNull(message = "Mức FODMAP không được để trống")
        FodmapLevel level,

        @Size(max = 5, message = "Tối đa 5 nhóm FODMAP")
        List<String> groups,

        @Size(max = 100, message = "Khẩu phần tham khảo tối đa 100 ký tự")
        String servingNote,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note,

        @NotBlank(message = "Nguồn tham khảo không được để trống")
        @Size(max = 255, message = "Nguồn tham khảo tối đa 255 ký tự")
        String source
) {
}