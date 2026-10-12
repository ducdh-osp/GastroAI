package vn.gastroai.be.application.triage;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.domain.triage.TriageKeyword;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageKeywordRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UC0054 - nguồn từ khóa cấp cứu đọc từ DB (bảng triage_keywords, quản lý qua màn hình admin CRUD
 * ở CMS), thay cho HardcodedTriageKeywordSource của GĐ3 - xem TriageKeywordSource. Đây là
 * implementation DUY NHẤT mang @Component nên TriageService luôn nhận bean này; dữ liệu khởi tạo
 * (giống hệt HardcodedTriageKeywordSource) đã được chuyển thành dữ liệu seed trong V32.
 */
@Component
public class DatabaseTriageKeywordSource implements TriageKeywordSource {

    private final TriageKeywordRepository repository;

    public DatabaseTriageKeywordSource(TriageKeywordRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> keywordsByGroup() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (TriageKeyword keyword : repository.findAllByOrderByGroupCodeAscKeywordAsc()) {
            result.computeIfAbsent(keyword.getGroupCode().name(), code -> new ArrayList<>())
                    .add(keyword.getKeyword());
        }
        return result;
    }
}
