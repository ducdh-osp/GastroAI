package vn.gastroai.be.domain.triage;

/**
 * UC0054 - danh sách CỐ ĐỊNH các nhóm dấu hiệu cấp cứu mà admin được chọn khi thêm từ khóa mới.
 * Nhóm KHÔNG phải CRUD được (chỉ từ khóa/câu nói trong nhóm mới CRUD được) vì mỗi nhóm đã được
 * TriageService.WARNING_GROUPS gắn với 1 hoặc nhiều WarningSign cụ thể và TriageService.EMERGENCY_SIGNS
 * quyết định nhóm đó có phải "khẩn cấp thật" (emergency=true, gọi 115) hay chỉ là nhóm cảnh báo
 * (ALARM feature) hay không. Nếu cho tạo nhóm mới tự do, nhóm đó sẽ không bao giờ được set
 * emergency=true (vì EMERGENCY_GROUP_CODES suy ra từ WARNING_GROUPS cố định) - admin sẽ ngỡ là đã
 * thêm được 1 nhóm khẩn cấp mới nhưng thực ra không có tác dụng gì.
 *
 * QUAN TRỌNG: tên hằng số (.name()) ở đây phải khớp CHÍNH XÁC với các String nhóm dùng trong
 * TriageService.WARNING_GROUPS/HardcodedTriageKeywordSource - xem TriageKeywordGroupTest.
 */
public enum TriageKeywordGroup {
    XUAT_HUYET_TIEU_HOA("Xuất huyết tiêu hóa"),
    DAU_BUNG_CAP_TINH("Đau bụng cấp tính dữ dội"),
    TAC_RUOT("Tắc ruột"),
    KHONG_THE_DI_TIEU("Không thể đi tiểu"),
    SOC_MAT_MAU("Sốc mất máu / mất nước nặng"),
    NHIEM_TRUNG_NANG("Nhiễm trùng nặng / nghi viêm ruột thừa"),
    DAU_LAN_CO_QUAN_KHAC("Đau lan sang cơ quan khác"),
    KEM_HO_HAP_TIM_MACH("Kèm khó thở / đau ngực"),
    TIEU_DUONG_KEM_NON("Tiểu đường kèm nôn"),
    VANG_DA_KEM_DAU_BUNG("Vàng da kèm đau bụng"),
    MAT_NUOC_NANG("Mất nước nặng do tiêu chảy kéo dài"),
    SUT_CAN_KHONG_RO_NGUYEN_NHAN("Sụt cân không rõ nguyên nhân"),
    KHO_NUOT_DAU_KHI_NUOT("Khó / đau khi nuốt");

    private final String label;

    TriageKeywordGroup(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
