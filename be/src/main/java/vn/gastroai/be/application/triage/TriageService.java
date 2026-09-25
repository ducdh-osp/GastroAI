package vn.gastroai.be.application.triage;

import org.springframework.stereotype.Service;
import vn.gastroai.be.domain.triage.TriageResult;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * UC0034 - sàng lọc an toàn tầng 1: đối sánh từ khóa dấu hiệu cấp cứu trên câu hỏi/tin nhắn
 * của người dùng, ưu tiên recall cao (thà báo nhầm còn hơn bỏ sót ca thật) nên dùng substring
 * match đơn giản, không dùng NLP/ML - dễ audit, dễ giải thích khi bảo vệ.
 *
 * Chưa nối vào luồng chat thật (UC0017) - đó là bước tích hợp làm sau, khi controller chat đã
 * có. Gọi check() độc lập là dùng/test được ngay.
 */
@Service
public class TriageService {
    private final TriageKeywordSource keywordSource;

    public TriageService(TriageKeywordSource keywordSource) {
        this.keywordSource = keywordSource;
    }

    public TriageResult check(String message) {
        String normalized = normalize(message);
        List<String> matchedGroups = new ArrayList<>();

        for (Map.Entry<String, List<String>> group : keywordSource.keywordsByGroup().entrySet()) {
            boolean matched = group.getValue().stream().anyMatch(normalized::contains);
            if (matched) {
                matchedGroups.add(group.getKey());
            }
        }

        return matchedGroups.isEmpty() ? TriageResult.safe() : new TriageResult(true, matchedGroups);
    }

    /**
     * Chuẩn hóa về chữ thường, bỏ dấu tiếng Việt (vd "Đau Bụng Dữ Dội" -> "dau bung du doi")
     * để so khớp không phụ thuộc người dùng có gõ dấu hay không. "đ"/"Đ" không tách được dấu
     * qua NFD (là ký tự Unicode riêng, không phải "d" + dấu gạch) nên phải thay thủ công.
     */
    private static String normalize(String text) {
        String lower = text.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
