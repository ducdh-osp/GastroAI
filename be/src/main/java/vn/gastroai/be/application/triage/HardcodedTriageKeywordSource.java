package vn.gastroai.be.application.triage;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * UC0054 - danh sách từ khóa cấp cứu tiêu hóa hardcode tạm cho GĐ3 (xem TriageKeywordSource).
 * Tổng hợp từ dấu hiệu cảnh báo khẩn cấp (red-flag symptoms) của Mayo Clinic, NHS 111 và
 * Cleveland Clinic cho triệu chứng tiêu hóa - KHÔNG tự suy đoán, xem nguồn ở PR mô tả.
 *
 * Từ khóa lưu sẵn dạng thường, không dấu (TriageService cũng chuẩn hóa input theo cách này
 * trước khi so khớp, xem TriageService.normalize()). Mỗi nhóm có nhiều cách nói khác nhau vì
 * UC0034 ưu tiên recall cao hơn precision - thà báo nhầm còn hơn bỏ sót ca thật.
 */
@Component
public class HardcodedTriageKeywordSource implements TriageKeywordSource {

    private static final Map<String, List<String>> KEYWORDS_BY_GROUP = Map.ofEntries(
            // Xuất huyết tiêu hóa - Mayo Clinic: vomiting blood, black tarry stool, rectal bleeding
            Map.entry("XUAT_HUYET_TIEU_HOA", List.of(
                    "di ngoai ra mau", "dai tien ra mau", "phan co mau", "mau trong phan",
                    "phan den", "di ngoai phan den", "non ra mau", "oi ra mau", "non ra mau den",
                    "non ra thu giong ba ca phe")),

            // Đau bụng cấp tính dữ dội - NHS: sudden/severe abdominal pain, rigid/tender abdomen
            Map.entry("DAU_BUNG_CAP_TINH", List.of(
                    "dau bung du doi", "dau bung du doi dot ngot", "dau quan bung du doi",
                    "dau bung dot ngot", "bung cung nhu go", "bung chuong cung",
                    "dau bung khong the dung thang", "dau bung khong the di lai duoc",
                    "cham vao bung dau chot", "an vao bung dau nhoi")),

            // Tắc ruột - NHS/Cleveland: inability to pass stool or gas
            Map.entry("TAC_RUOT", List.of(
                    "khong di ngoai duoc", "khong trung tien duoc", "bi tac ruot",
                    "bung chuong to khong trung tien duoc")),

            // Sốc mất máu / mất nước nặng - Mayo Clinic shock symptoms
            Map.entry("SOC_MAT_MAU", List.of(
                    "chong mat du doi", "ngat xiu", "xiu di", "da xanh tai lanh",
                    "tim dap nhanh kem dau bung", "met lu kem dau bung",
                    "tieu it hoac khong di tieu")),

            // Nhiễm trùng nặng / nghi viêm ruột thừa - Cleveland: fever + abdominal pain
            Map.entry("NHIEM_TRUNG_NANG", List.of(
                    "sot cao kem dau bung", "sot tren 39 do kem dau bung",
                    "nghi viem ruot thua", "dau bung ben phai duoi kem sot")),

            // Đau lan (viêm tụy...) - NHS: pain radiating to chest/back/groin/shoulder
            Map.entry("DAU_LAN_CO_QUAN_KHAC", List.of(
                    "dau bung lan ra sau lung", "dau bung lan len vai", "dau bung lan ra nguc")),

            // Kèm hô hấp/tim mạch - NHS: abdominal pain with chest pain/difficulty breathing
            Map.entry("KEM_HO_HAP_TIM_MACH", List.of(
                    "kem kho tho", "kem dau nguc", "kem tuc nguc", "kho tho kem dau bung",
                    "vua dau bung vua kho tho", "vua dau bung vua dau nguc")),

            // Tiểu đường kèm nôn - NHS: diabetes + vomiting is a specific red flag
            Map.entry("TIEU_DUONG_KEM_NON", List.of(
                    "bi tieu duong dang non", "tieu duong kem non nhieu")),

            // Vàng da kèm đau bụng - NHS
            Map.entry("VANG_DA_KEM_DAU_BUNG", List.of(
                    "vang da vang mat kem dau bung", "vang da kem dau bung")),

            // Mất nước nặng do tiêu chảy kéo dài - NHS: severe dehydration, bleeding + confusion
            Map.entry("MAT_NUOC_NANG", List.of(
                    "tieu chay lien tuc khong ngung", "tieu chay kem lu lan",
                    "mat nuoc nang", "tieu chay kem mat y thuc")));

    @Override
    public Map<String, List<String>> keywordsByGroup() {
        return KEYWORDS_BY_GROUP;
    }
}
