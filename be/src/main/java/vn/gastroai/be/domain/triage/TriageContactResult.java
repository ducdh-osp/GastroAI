package vn.gastroai.be.domain.triage;

/**
 * Ket qua lien he Admin ghi lai o buoc "Can can thiep". He thong khong co kenh tu goi nguoi than
 * (SMS da cat, khong co goi tu dong) nen Admin tu goi theo so trong ho so roi ghi ket qua o day.
 */
public enum TriageContactResult {
    RELATIVE_REACHED("Đã gọi người thân - người thân sẽ kiểm tra"),
    RELATIVE_NO_ANSWER("Gọi người thân không nghe máy"),
    RELATIVE_CONFIRMED_SAFE("Người thân xác nhận bệnh nhân an toàn"),
    PATIENT_REACHED("Đã gọi được cho bệnh nhân"),
    CALLED_EMERGENCY("Đã gọi 115");

    private final String label;

    TriageContactResult(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
