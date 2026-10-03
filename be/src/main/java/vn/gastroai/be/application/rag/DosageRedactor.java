package vn.gastroai.be.application.rag;

import java.util.regex.Pattern;

/**
 * Lọc liều lượng thuốc cụ thể (vd "25mg", "40mg/kg/ngay", "2 vien/ngay") khỏi nội dung chunk
 * TRƯỚC khi đưa vào prompt cho Gemini và trước khi hiển thị làm trích dẫn nguồn cho bệnh nhân.
 * Tài liệu nguồn (docs/sample-documents) là phác đồ dành cho bác sĩ, có ghi liều chính xác mà
 * bệnh nhân (người dùng cuối) không nên tự áp dụng khi chưa qua bác sĩ/dược sĩ.
 *
 * Đây là lớp phòng thủ thứ 2 (defense-in-depth) - lớp 1 là chỉ dẫn trong SYSTEM_PROMPT_PREFIX
 * (RagQueryService/StreamingRagQueryService) yêu cầu Gemini tự diễn giải chung, không đọc lại
 * số liệu. Lớp này đảm bảo ngay cả khi Gemini không tuân thủ chỉ dẫn, hoặc khi chunk được hiển
 * thị trực tiếp làm trích dẫn nguồn (không qua Gemini), số liệu cũng không còn trong văn bản.
 *
 * Regex có thể bắt nhầm 1 số giá trị không phải liều thuốc (vd kết quả xét nghiệm "120mg/dL") -
 * đánh đổi chấp nhận được vì đây là lớp phòng thủ bổ sung, không phải lớp duy nhất, và rủi ro
 * che mất vài giá trị khác nhỏ hơn rủi ro lộ liều thuốc thật.
 */
final class DosageRedactor {

    private static final String REPLACEMENT = "[liều lượng cụ thể đã được ẩn - vui lòng hỏi bác sĩ/dược sĩ]";

    /** Số + đơn vị liều lượng thuốc (mg, mcg, g, ml, IU/UI), có thể kèm /kg hoặc /ngày/lần. */
    private static final Pattern DOSE_WITH_UNIT = Pattern.compile(
            "\\d+([.,]\\d+)?\\s?(mg|mcg|μg|ug|g|ml|IU|UI)\\b\\s*(/\\s*(kg|ngày|ngay|lần|lan|day))?",
            Pattern.CASE_INSENSITIVE);

    /** Số + đơn vị đếm (viên/ống/gói), có thể kèm "/ngày" hoặc "mỗi ngày". */
    private static final Pattern DOSE_WITH_COUNT_UNIT = Pattern.compile(
            "\\d+\\s?(viên|vien|ống|ong|gói|goi)\\b\\s*(/\\s*(ngày|ngay|lần|lan))?",
            Pattern.CASE_INSENSITIVE);

    private DosageRedactor() {
    }

    static String redact(String text) {
        if (text == null || text.isBlank()) return text;
        String result = DOSE_WITH_UNIT.matcher(text).replaceAll(REPLACEMENT);
        return DOSE_WITH_COUNT_UNIT.matcher(result).replaceAll(REPLACEMENT);
    }
}
