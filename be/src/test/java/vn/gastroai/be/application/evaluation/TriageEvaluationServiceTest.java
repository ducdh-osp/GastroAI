package vn.gastroai.be.application.evaluation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.gastroai.be.application.evaluation.TriageEvaluationService.TriageEvaluationReport;
import vn.gastroai.be.application.evaluation.TriageEvaluationService.TriageTestCase;
import vn.gastroai.be.application.triage.HardcodedTriageKeywordSource;
import vn.gastroai.be.application.triage.TriageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UC0038: Triage Sensitivity Evaluation Tests")
class TriageEvaluationServiceTest {

    private TriageEvaluationService evaluationService;

    @BeforeEach
    void setUp() {
        TriageService triageService = new TriageService(new HardcodedTriageKeywordSource());
        evaluationService = new TriageEvaluationService(triageService);
    }

    @Test
    @DisplayName("Evaluate on comprehensive benchmark dataset achieves high sensitivity (recall >= 95%)")
    void testBenchmarkEvaluationHighSensitivity() {
        TriageEvaluationReport report = evaluationService.evaluate();

        assertNotNull(report);
        assertTrue(report.totalCases() >= 40, "Benchmark dataset should have at least 40 cases");
        assertTrue(report.positiveCases() >= 20, "Should have sufficient positive emergency cases");
        assertTrue(report.negativeCases() >= 15, "Should have sufficient non-emergency safe cases");

        // Đề cương mục 2.2: Lớp Triage nhận diện tối thiểu 5 nhóm dấu hiệu cấp cứu, ưu tiên độ nhạy (recall) cao
        assertTrue(report.sensitivityRecall() >= 0.95,
                "Sensitivity (Recall) must be >= 95%, actual: " + report.sensitivityRecall());

        // Báo động giả (False alarm rate) phải được kiểm soát ở mức hợp lý
        assertTrue(report.falseAlarmRate() <= 0.20,
                "False Alarm Rate should be controlled <= 20%, actual: " + report.falseAlarmRate());

        // F2-Score (Recall trọng số gấp 2 lần) phải cao
        assertTrue(report.f2Score() >= 0.90, "F2-Score should be >= 0.90, actual: " + report.f2Score());

        // Ma trận nhầm lẫn phải nhất quán
        assertEquals(report.totalCases(),
                report.truePositives() + report.falseNegatives() + report.falsePositives() + report.trueNegatives());

        // Kiểm tra phân tích theo danh mục (tối thiểu 5 nhóm cấp cứu)
        assertTrue(report.categoryMetrics().size() >= 5,
                "Must evaluate at least 5 emergency sign categories");

        for (var catMetric : report.categoryMetrics()) {
            assertTrue(catMetric.totalCases() > 0);
            assertTrue(catMetric.recall() >= 0.80,
                    "Recall for category " + catMetric.category() + " should be >= 80%");
        }
    }

    @Test
    @DisplayName("Evaluate on custom test cases correctly calculates metrics")
    void testCustomTestCasesEvaluation() {
        List<TriageTestCase> customCases = List.of(
                new TriageTestCase("C1", "non ra mau rat nhieu", true, "XUAT_HUYET", "Emergency test"),
                new TriageTestCase("C2", "dau bung du doi khong dung day duoc", true, "DAU_BUNG", "Emergency test"),
                new TriageTestCase("C3", "hom nay an gi cho de tieu hoa", false, "THUONG", "Safe test"),
                new TriageTestCase("C4", "toi di xet nghiem mau", false, "DISTRACTOR", "Distractor test")
        );

        TriageEvaluationReport report = evaluationService.evaluate(customCases);

        assertEquals(4, report.totalCases());
        assertEquals(2, report.positiveCases());
        assertEquals(2, report.negativeCases());
        assertEquals(2, report.truePositives());
        assertEquals(0, report.falseNegatives());
        assertEquals(1.0, report.sensitivityRecall());
        assertEquals(0.0, report.falseAlarmRate());
    }

    @Test
    @DisplayName("Cached report is returned on getLatestReport")
    void testGetLatestReport() {
        TriageEvaluationReport first = evaluationService.getLatestReport();
        assertNotNull(first);
        TriageEvaluationReport cached = evaluationService.getLatestReport();
        assertSame(first, cached);
    }
}
