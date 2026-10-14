package vn.gastroai.be.application.triage;

/** UC0054 - payload tạo/sửa 1 từ khóa, dùng chung cho POST và PUT (cùng 2 field). */
public record TriageKeywordRequest(String groupCode, String keyword) {
}
