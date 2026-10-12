package vn.gastroai.be.application.triage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.application.support.VietnameseText;
import vn.gastroai.be.domain.triage.TriageKeyword;
import vn.gastroai.be.domain.triage.TriageKeywordGroup;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageKeywordRepository;

import java.util.Arrays;
import java.util.List;

/**
 * UC0054 - CRUD từ khóa dấu hiệu cấp cứu cho màn hình admin CMS. Từ khóa luôn được chuẩn hóa
 * (VietnameseText.normalize() - giống TriageService.normalize()) trước khi lưu, vì
 * DatabaseTriageKeywordSource đưa thẳng giá trị trong DB vào so khớp substring với tin nhắn đã
 * chuẩn hóa - lưu sai dạng sẽ khiến từ khóa không bao giờ khớp được.
 */
@Service
public class TriageKeywordService {

    private final TriageKeywordRepository repository;

    public TriageKeywordService(TriageKeywordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TriageKeywordResponse> list() {
        return repository.findAllByOrderByGroupCodeAscKeywordAsc().stream()
                .map(TriageKeywordService::toResponse)
                .toList();
    }

    public List<TriageKeywordGroupResponse> listGroups() {
        return Arrays.stream(TriageKeywordGroup.values())
                .map(group -> new TriageKeywordGroupResponse(group.name(), group.label()))
                .toList();
    }

    @Transactional
    public TriageKeywordResponse create(TriageKeywordRequest request) {
        TriageKeywordGroup group = parseGroup(request.groupCode());
        String normalized = normalizeKeyword(request.keyword());

        if (repository.existsByGroupCodeAndKeyword(group, normalized)) {
            throw new IllegalStateException("Tu khoa \"" + normalized + "\" da ton tai trong nhom " + group);
        }
        return toResponse(repository.save(new TriageKeyword(group, normalized)));
    }

    @Transactional
    public TriageKeywordResponse update(Long id, TriageKeywordRequest request) {
        TriageKeyword keyword = findOrThrow(id);
        TriageKeywordGroup group = parseGroup(request.groupCode());
        String normalized = normalizeKeyword(request.keyword());

        if (repository.existsByGroupCodeAndKeywordAndIdNot(group, normalized, id)) {
            throw new IllegalStateException("Tu khoa \"" + normalized + "\" da ton tai trong nhom " + group);
        }
        keyword.setGroupCode(group);
        keyword.setKeyword(normalized);
        return toResponse(repository.save(keyword));
    }

    @Transactional
    public void delete(Long id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private TriageKeyword findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay tu khoa id=" + id));
    }

    private static TriageKeywordGroup parseGroup(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("groupCode khong duoc de trong");
        }
        try {
            return TriageKeywordGroup.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("groupCode khong hop le: " + raw);
        }
    }

    private static String normalizeKeyword(String raw) {
        String normalized = VietnameseText.normalize(raw);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("keyword khong duoc de trong");
        }
        return normalized;
    }

    private static TriageKeywordResponse toResponse(TriageKeyword keyword) {
        return new TriageKeywordResponse(
                keyword.getId(),
                keyword.getGroupCode().name(),
                keyword.getGroupCode().label(),
                keyword.getKeyword(),
                keyword.getCreatedAt(),
                keyword.getUpdatedAt());
    }
}
