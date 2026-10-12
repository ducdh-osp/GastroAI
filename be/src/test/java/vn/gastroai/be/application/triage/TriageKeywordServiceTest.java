package vn.gastroai.be.application.triage;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.support.ResourceNotFoundException;
import vn.gastroai.be.domain.triage.TriageKeyword;
import vn.gastroai.be.domain.triage.TriageKeywordGroup;
import vn.gastroai.be.infrastructure.persistence.postgres.TriageKeywordRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TriageKeywordServiceTest {

    private final TriageKeywordRepository repository = mock(TriageKeywordRepository.class);
    private final TriageKeywordService service = new TriageKeywordService(repository);

    @Test
    void listGroupsReturnsAll13FixedGroupsWithLabel() {
        var groups = service.listGroups();

        assertEquals(TriageKeywordGroup.values().length, groups.size());
        assertEquals("XUAT_HUYET_TIEU_HOA", groups.get(0).code());
        assertEquals("Xuất huyết tiêu hóa", groups.get(0).label());
    }

    @Test
    void createNormalizesVietnameseDiacriticsBeforeSaving() {
        when(repository.save(any())).thenAnswer(invocation -> {
            TriageKeyword saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        TriageKeywordResponse response = service.create(
                new TriageKeywordRequest("DAU_BUNG_CAP_TINH", "Đau Bụng Dữ Dội"));

        assertEquals("dau bung du doi", response.keyword());
        assertEquals("DAU_BUNG_CAP_TINH", response.groupCode());
    }

    @Test
    void createRejectsUnknownGroupCode() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new TriageKeywordRequest("NHOM_KHONG_TON_TAI", "dau bung")));
    }

    @Test
    void createRejectsBlankKeyword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new TriageKeywordRequest("DAU_BUNG_CAP_TINH", "   ")));
    }

    @Test
    void createRejectsDuplicateKeywordInSameGroup() {
        when(repository.existsByGroupCodeAndKeyword(TriageKeywordGroup.TAC_RUOT, "khong xi hoi duoc"))
                .thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.create(
                new TriageKeywordRequest("TAC_RUOT", "khong xi hoi duoc")));
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesGroupAndKeywordOfExistingRow() {
        TriageKeyword existing = new TriageKeyword(TriageKeywordGroup.TAC_RUOT, "khong xi hoi duoc");
        existing.setId(7L);
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(existing));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TriageKeywordResponse response = service.update(7L,
                new TriageKeywordRequest("KHO_NUOT_DAU_KHI_NUOT", "nuot bi dau ran"));

        assertEquals("KHO_NUOT_DAU_KHI_NUOT", response.groupCode());
        assertEquals("nuot bi dau ran", response.keyword());
    }

    @Test
    void updateOnMissingIdThrowsResourceNotFound() {
        when(repository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(99L,
                new TriageKeywordRequest("TAC_RUOT", "khong xi hoi duoc")));
    }

    @Test
    void deleteOnMissingIdThrowsResourceNotFoundAndDoesNotCallRepositoryDelete() {
        when(repository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(99L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteRemovesExistingKeyword() {
        TriageKeyword existing = new TriageKeyword(TriageKeywordGroup.TAC_RUOT, "khong xi hoi duoc");
        existing.setId(7L);
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(existing));

        service.delete(7L);

        verify(repository).deleteById(7L);
    }
}
