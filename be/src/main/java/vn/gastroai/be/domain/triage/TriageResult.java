package vn.gastroai.be.domain.triage;

import java.util.List;

/**
 * UC0034 - kết quả sàng lọc an toàn tầng 1. matchedGroups là danh sách nhóm triệu chứng đã
 * khớp (vd "XUAT_HUYET_TIEU_HOA") - dùng để UC0035 hiển thị cảnh báo phù hợp và UC0036 đính
 * kèm khi đẩy sự kiện sang admin, không cần biết từ khóa cụ thể nào đã khớp.
 *
 * matchedGroups khác rỗng KHÔNG đồng nghĩa emergency=true (xem TriageService.check()) - có
 * thể chỉ khớp nhóm ALARM (sụt cân, khó nuốt...) nên vẫn đáng nhắc bệnh nhân nhưng chưa tới
 * mức gọi 115/báo động CMS. Chỉ dựa vào emergency để quyết định có bắn TriageAlert hay không.
 */
public record TriageResult(boolean emergency, List<String> matchedGroups) {
    public static TriageResult safe() {
        return new TriageResult(false, List.of());
    }
}
