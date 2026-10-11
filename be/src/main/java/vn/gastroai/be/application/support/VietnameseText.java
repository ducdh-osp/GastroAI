package vn.gastroai.be.application.support;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Chuẩn hoá/tách từ tiếng Việt dùng chung cho các chỗ cần so khớp không phụ thuộc dấu
 * (UC0022 phân tích tương quan thực phẩm - triệu chứng). Logic bỏ dấu giống hệt
 * TriageService.normalize() nhưng tách ra đây để dùng lại được ở nhiều nơi, tránh mỗi chỗ
 * tự viết lại 1 bản (đã có tiền lệ trong dự án vì không tái dùng - xem issue tìm kiếm bài
 * viết kiến thức không bỏ dấu được).
 */
public final class VietnameseText {

    /** Từ phổ biến trong mô tả bữa ăn tiếng Việt không mang ý nghĩa nhận diện món ăn cụ thể -
     * loại ra khi tách từ khoá cho UC0022, tránh những từ này chiếm hết bảng kết quả. */
    private static final Set<String> STOPWORDS = Set.of(
            "va", "voi", "cung", "mon", "an", "uong", "mot", "hai", "ba", "bon", "nam",
            "it", "nhieu", "them", "duoc", "co", "khong", "cua", "la", "the", "nay", "do",
            "day", "chut", "nho", "lon", "to", "chen", "bat", "ly", "coc", "phan", "suat",
            "trong", "ngoai", "tren", "duoi", "sau", "truoc", "khi", "luc", "o", "tai",
            "buoi", "sang", "trua", "chieu", "toi", "khuya", "nua", "con", "rat", "qua",
            "mieng", "loai", "kieu", "theo", "cho");

    private VietnameseText() {
    }

    /** Chữ thường, bỏ dấu - giống TriageService.normalize(), dùng cho so khớp không phụ thuộc dấu. */
    public static String normalize(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    /**
     * Tách 1 câu mô tả thành các từ khoá đã chuẩn hoá, bỏ từ dừng và từ quá ngắn/toàn số.
     * Dùng LinkedHashSet để loại trùng trong CÙNG 1 câu (vd "canh chua canh ca" chỉ tính
     * "canh" 1 lần cho câu đó) nhưng vẫn giữ thứ tự xuất hiện đầu tiên.
     */
    public static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        String normalized = normalize(text);
        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(normalized.split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 2)
                .filter(token -> !token.chars().allMatch(Character::isDigit))
                .filter(token -> !STOPWORDS.contains(token))
                .forEach(tokens::add);
        return tokens;
    }
}
