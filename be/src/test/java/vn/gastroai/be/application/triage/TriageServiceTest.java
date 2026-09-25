package vn.gastroai.be.application.triage;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.triage.TriageResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test thuần logic (không @SpringBootTest, không cần GEMINI_API_KEY/DB) vì TriageService chỉ
 * so khớp string, không gọi API/DB nào.
 */
class TriageServiceTest {

    private final TriageService triageService = new TriageService(new HardcodedTriageKeywordSource());

    @Test
    void matchesEmergencyKeywordRegardlessOfDiacritics() {
        TriageResult result = triageService.check("Tôi bị đau bụng dữ dội quá, không đứng thẳng được");
        assertTrue(result.emergency());
        assertTrue(result.matchedGroups().contains("DAU_BUNG_CAP_TINH"));
    }

    @Test
    void matchesEvenWithoutVietnameseDiacritics() {
        TriageResult result = triageService.check("di ngoai ra mau tu sang gio");
        assertTrue(result.emergency());
        assertTrue(result.matchedGroups().contains("XUAT_HUYET_TIEU_HOA"));
    }

    @Test
    void matchesMultipleGroupsWhenBothPresent() {
        TriageResult result = triageService.check("Đau bụng dữ dội kèm khó thở, người nhà đang chóng mặt");
        assertTrue(result.emergency());
        assertTrue(result.matchedGroups().contains("DAU_BUNG_CAP_TINH"));
        assertTrue(result.matchedGroups().contains("KEM_HO_HAP_TIM_MACH"));
    }

    @Test
    void doesNotFlagOrdinaryQuestion() {
        TriageResult result = triageService.check("Ăn nhiều rau có tốt cho tiêu hóa không?");
        assertFalse(result.emergency());
        assertEquals(0, result.matchedGroups().size());
    }
}
