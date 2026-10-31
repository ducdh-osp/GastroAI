package vn.gastroai.be.api.cms.evaluation;

import org.springframework.web.bind.annotation.*;
import vn.gastroai.be.application.evaluation.RagEvaluationService;
import vn.gastroai.be.application.evaluation.RagEvaluationService.RagEvaluationReport;
import vn.gastroai.be.application.evaluation.TriageEvaluationService;
import vn.gastroai.be.application.evaluation.TriageEvaluationService.TriageEvaluationReport;

import java.time.Instant;

/**
 * Controller cho UC0037 (Đánh giá độ chính xác truy xuất RAG)
 * và UC0038 (Đánh giá độ nhạy Triage).
 * Cung cấp API báo cáo định lượng và kích hoạt benchmark trực tiếp từ CMS.
 */
@RestController
@RequestMapping("/api/v1/cms/evaluation")
public class CmsEvaluationController {

    private final TriageEvaluationService triageEvaluationService;
    private final RagEvaluationService ragEvaluationService;

    public CmsEvaluationController(
            TriageEvaluationService triageEvaluationService,
            RagEvaluationService ragEvaluationService) {
        this.triageEvaluationService = triageEvaluationService;
        this.ragEvaluationService = ragEvaluationService;
    }

    public record EvaluationSummaryResponse(
            // UC0038 Triage metrics
            double triageSensitivityRecall,
            double triageFalseAlarmRate,
            double triageF2Score,
            int triageTotalCases,
            int triageTruePositives,
            int triageFalseNegatives,
            // UC0037 RAG metrics
            double ragHitRateAt1,
            double ragHitRateAt3,
            double ragHitRateAtK,
            double ragMrr,
            double ragAvgSimilarity,
            int ragTotalQueries,
            int ragTopK,
            String ragRetrievalMode,
            String ragDataSource,
            String ragWarning,
            Instant lastEvaluatedAt
    ) {
        public EvaluationSummaryResponse(
                double triageSensitivityRecall, double triageFalseAlarmRate, double triageF2Score,
                int triageTotalCases, int triageTruePositives, int triageFalseNegatives,
                double ragHitRateAt1, double ragHitRateAt3, double ragHitRateAtK,
                double ragMrr, double ragAvgSimilarity, int ragTotalQueries, int ragTopK,
                Instant lastEvaluatedAt) {
            this(triageSensitivityRecall, triageFalseAlarmRate, triageF2Score,
                    triageTotalCases, triageTruePositives, triageFalseNegatives,
                    ragHitRateAt1, ragHitRateAt3, ragHitRateAtK,
                    ragMrr, ragAvgSimilarity, ragTotalQueries, ragTopK,
                    "VECTOR", "DATABASE", null, lastEvaluatedAt);
        }
    }

    @GetMapping("/summary")
    public EvaluationSummaryResponse getSummary() {
        TriageEvaluationReport triageReport = triageEvaluationService.getLatestReport();
        RagEvaluationReport ragReport = ragEvaluationService.getLatestReport();

        return new EvaluationSummaryResponse(
                triageReport.sensitivityRecall(),
                triageReport.falseAlarmRate(),
                triageReport.f2Score(),
                triageReport.totalCases(),
                triageReport.truePositives(),
                triageReport.falseNegatives(),
                ragReport.hitRateAt1(),
                ragReport.hitRateAt3(),
                ragReport.hitRateAtK(),
                ragReport.mrr(),
                ragReport.averageSimilarity(),
                ragReport.totalQueries(),
                ragReport.topK(),
                ragReport.retrievalMode(),
                ragReport.dataSource(),
                ragReport.warning(),
                Instant.now()
        );
    }

    // === UC0038: TRIAGE SENSITIVITY EVALUATION ===

    @GetMapping("/triage")
    public TriageEvaluationReport getTriageReport() {
        return triageEvaluationService.getLatestReport();
    }

    @PostMapping("/triage/run")
    public TriageEvaluationReport runTriageEvaluation() {
        return triageEvaluationService.evaluate();
    }

    // === UC0037: RAG RETRIEVAL ACCURACY EVALUATION ===

    @GetMapping("/rag")
    public RagEvaluationReport getRagReport() {
        return ragEvaluationService.getLatestReport();
    }

    @PostMapping("/rag/run")
    public RagEvaluationReport runRagEvaluation(@RequestParam(defaultValue = "5") int topK) {
        return ragEvaluationService.evaluate(topK);
    }
}
