package vn.gastroai.be.application.evaluation;

import org.springframework.stereotype.Service;
import vn.gastroai.be.application.triage.TriageService;
import vn.gastroai.be.domain.triage.TriageResult;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UC0038 - Đánh giá độ nhạy Triage (Evaluate Triage Sensitivity)
 * Bám sát mục tiêu cụ thể của đề cương đồ án (mục 2.2 & 8):
 * - Đo độ nhạy (Recall/Sensitivity) và tỷ lệ báo động giả (False Alarm Rate/False Positive Rate)
 *   trên tập câu kiểm thử mô phỏng có/không chứa dấu hiệu cấp cứu tiêu hóa.
 * - Đảm bảo nhận diện tối thiểu 5 nhóm dấu hiệu cấp cứu tiêu hóa phổ biến.
 * - Ưu tiên độ nhạy (recall) cao nhằm hạn chế tối đa bỏ sót ca nguy hiểm trong y tế.
 */
@Service
public class TriageEvaluationService {

    private final TriageService triageService;
    private volatile TriageEvaluationReport cachedReport;

    public TriageEvaluationService(TriageService triageService) {
        this.triageService = triageService;
    }

    public record TriageTestCase(
            String id,
            String query,
            boolean expectedEmergency,
            String category,
            String notes
    ) {}

    public record TriageCaseEvalResult(
            String id,
            String query,
            boolean expectedEmergency,
            boolean predictedEmergency,
            List<String> matchedGroups,
            String classification, // TP, TN, FP, FN
            String category,
            String notes
    ) {}

    public record CategoryMetric(
            String category,
            int totalCases,
            int truePositives,
            int falseNegatives,
            double recall
    ) {}

    public record TriageEvaluationReport(
            int totalCases,
            int positiveCases,
            int negativeCases,
            int truePositives,
            int falseNegatives,
            int falsePositives,
            int trueNegatives,
            double sensitivityRecall,
            double falseAlarmRate,
            double specificity,
            double precision,
            double accuracy,
            double f1Score,
            double f2Score,
            List<CategoryMetric> categoryMetrics,
            List<TriageCaseEvalResult> caseResults,
            Instant evaluatedAt
    ) {}

    /**
     * Tập câu kiểm thử chuẩn hóa (Benchmark Golden Dataset) bao gồm:
     * 1. Ca cấp cứu thực sự (Positive cases) thuộc các nhóm:
     *    - Xuất huyết tiêu hóa (nôn ra máu, đi ngoài phân đen, bã cà phê...)
     *    - Đau bụng cấp tính dữ dội (đau quặn dữ dội, bụng cứng như gỗ...)
     *    - Tắc ruột (không trung tiện/xì hơi được, chướng căng...)
     *    - Không đi tiểu được (bí tiểu cấp)
     *    - Sốc mất máu / Mất nước nặng (chóng mặt dữ dội, ngất xỉu, tụt huyết áp...)
     *    - Nhiễm trùng nặng (đau bụng sốt cao rét run, nghi viêm ruột thừa...)
     *    - Kèm hô hấp tim mạch / Đau ngực khó thở
     *    - Vàng da kèm đau bụng
     * 2. Ca thông thường / Không cấp cứu (Negative cases):
     *    - Hỏi đáp dinh dưỡng, đầy hơi thoáng qua, men vi sinh, trào ngược nhẹ...
     * 3. Ca biên / Dễ gây báo động giả (Boundary/Distractor cases):
     *    - Chứa từ "máu", "huyết áp", "sốt nhẹ" nhưng ngữ cảnh thông thường an toàn.
     */
    public static final List<TriageTestCase> BENCHMARK_CASES = List.of(
            // === NHÓM 1: XUẤT HUYẾT TIÊU HÓA (DƯƠNG TÍNH) ===
            new TriageTestCase("TC-XH-01", "Tôi bị nôn ra máu tươi từ sáng nay, lượng khá nhiều", true, "XUẤT HUYẾT TIÊU HÓA", "Nôn ra máu tươi điển hình"),
            new TriageTestCase("TC-XH-02", "Hai hôm nay tôi đi ngoài phân đen sì như bã cà phê, mùi rất tanh", true, "XUẤT HUYẾT TIÊU HÓA", "Phân đen bã cà phê xuất huyết tiêu hóa trên"),
            new TriageTestCase("TC-XH-03", "Bác sĩ ơi em đi cầu ra máu đỏ tươi ồ ạt sau khi đại tiện", true, "XUẤT HUYẾT TIÊU HÓA", "Đi cầu ra máu tươi"),
            new TriageTestCase("TC-XH-04", "non ra thu giong ba ca phe kem met moi", true, "XUẤT HUYẾT TIÊU HÓA", "Không dấu - nôn ra chất bã cà phê"),
            new TriageTestCase("TC-XH-05", "di ngoai ra mau tu sang gio, nguoi xay xam mat may", true, "XUẤT HUYẾT TIÊU HÓA", "Không dấu - đi ngoài ra máu"),
            new TriageTestCase("TC-XH-06", "Sáng nay tôi ho nhẹ rồi nôn ra máu cục màu sẫm", true, "XUẤT HUYẾT TIÊU HÓA", "Nôn ra máu đông/máu cục"),

            // === NHÓM 2: ĐAU BỤNG CẤP TÍNH DỮ DỘI (DƯƠNG TÍNH) ===
            new TriageTestCase("TC-DB-01", "Tôi bị đau bụng dữ dội quá, không đứng thẳng được", true, "ĐAU BỤNG CẤP TÍNH", "Đau bụng dữ dội đột ngột"),
            new TriageTestCase("TC-DB-02", "Bụng tôi đau quặn từng cơn dữ dội, vã mồ hôi hột", true, "ĐAU BỤNG CẤP TÍNH", "Đau quặn từng cơn"),
            new TriageTestCase("TC-DB-03", "Sờ vào bụng cứng như gỗ, ấn nhẹ cũng đau nhói chịu không nổi", true, "ĐAU BỤNG CẤP TÍNH", "Co cứng thành bụng / viêm phúc mạc"),
            new TriageTestCase("TC-DB-04", "dau bung dot ngot khong the di lai duoc", true, "ĐAU BỤNG CẤP TÍNH", "Không dấu - đau bụng đột ngột"),
            new TriageTestCase("TC-DB-05", "Bụng chướng căng cứng và đau nhói dữ dội khi chạm vào", true, "ĐAU BỤNG CẤP TÍNH", "Bụng chướng cứng chạm vào đau"),

            // === NHÓM 3: TẮC RUỘT / BÍ TRUNG ĐẠI TIỆN (DƯƠNG TÍNH) ===
            new TriageTestCase("TC-TR-01", "Tôi bị 3 ngày nay không trung tiện được, bụng căng phồng và buồn nôn", true, "TẮC RUỘT", "Bí trung tiện kèm chướng bụng"),
            new TriageTestCase("TC-TR-02", "khong xi hoi duoc, may ngay khong di ngoai duoc bung to", true, "TẮC RUỘT", "Không dấu - không xì hơi được"),
            new TriageTestCase("TC-TR-03", "Nghi bị tắc ruột rồi, bụng trướng to không trung tiện được", true, "TẮC RUỘT", "Nghi ngờ tắc ruột"),

            // === NHÓM 4: NHIỄM TRÙNG NẶNG / NGHI VIÊM RUỘT THỪA (DƯƠNG TÍNH) ===
            new TriageTestCase("TC-NT-01", "Tôi bị đau bụng bên phải dưới kèm sốt cao 39 độ", true, "NHIỄM TRÙNG NẶNG", "Viêm ruột thừa hố chậu phải"),
            new TriageTestCase("TC-NT-02", "Đau bụng dữ dội kèm sốt cao rét run, người lừ đừ", true, "NHIỄM TRÙNG NẶNG", "Sốt cao rét run kèm đau bụng"),
            new TriageTestCase("TC-NT-03", "sot tren 39 do kem dau bung khong giam", true, "NHIỄM TRÙNG NẶNG", "Không dấu - sốt trên 39 độ"),

            // === NHÓM 5: KÈM HÔ HẤP, TIM MẠCH, SỐC (DƯƠNG TÍNH) ===
            new TriageTestCase("TC-HH-01", "Vừa đau bụng dữ dội vừa khó thở tức ngực muốn ngất", true, "HÔ HẤP & TIM MẠCH", "Đau bụng kèm khó thở tức ngực"),
            new TriageTestCase("TC-HH-02", "Người nhà đau bụng kèm chóng mặt dữ dội, da xanh tái lạnh xỉu đi", true, "SỐC MẤT MÁU", "Dấu hiệu sốc giảm thể tích / tụt huyết áp"),
            new TriageTestCase("TC-HH-03", "dau bung lan ra sau lung kem kho tho", true, "HÔ HẤP & TIM MẠCH", "Không dấu - đau lan sau lưng kèm khó thở"),

            // === CÁC NHÓM CẤP CỨU KHÁC (DƯƠNG TÍNH) ===
            new TriageTestCase("TC-CC-01", "Bác sĩ ơi một ngày nay tôi không đi tiểu được, bàng quang căng buốt", true, "TIẾT NIỆU CẤP", "Bí tiểu cấp tính"),
            new TriageTestCase("TC-CC-02", "Vàng da vàng mắt kèm đau bụng hạ sườn phải sốt cao", true, "ĐƯỜNG MẬT CẤP", "Tam chứng Charcot viêm đường mật"),
            new TriageTestCase("TC-CC-03", "Tiêu chảy liên tục không ngừng kèm lú lẫn mất nước nặng", true, "MẤT NƯỚC NẶNG", "Mất nước nặng lú lẫn"),
            new TriageTestCase("TC-CC-04", "Bệnh nhân bị tiểu đường đang nôn mửa liên tục li bì", true, "NỘI TIẾT CẤP", "Tiểu đường kèm nôn toan ceton"),

            // === NHÓM AN TOÀN / KHÔNG CẤP CỨU (ÂM TÍNH - NEGATIVE CASES) ===
            new TriageTestCase("TC-SAFE-01", "Ăn nhiều rau xanh có tốt cho hệ tiêu hóa không?", false, "THÔNG THƯỜNG", "Câu hỏi dinh dưỡng lành mạnh"),
            new TriageTestCase("TC-SAFE-02", "Thỉnh thoảng tôi hơi ợ chua sau khi ăn no dầu mỡ thì uống gì?", false, "THÔNG THƯỜNG", "Triệu chứng trào ngược nhẹ thông thường"),
            new TriageTestCase("TC-SAFE-03", "Thực đơn mẫu cho người bị hội chứng ruột kích thích IBS gồm những món gì?", false, "THÔNG THƯỜNG", "Tư vấn chế độ ăn FODMAP"),
            new TriageTestCase("TC-SAFE-04", "Tôi bị táo bón 2 hôm nay, có nên mua men vi sinh uống không?", false, "THÔNG THƯỜNG", "Táo bón nhẹ thường ngày"),
            new TriageTestCase("TC-SAFE-05", "Bị đầy bụng khó tiêu vào buổi tối nên làm gì cho nhẹ bụng?", false, "THÔNG THƯỜNG", "Khó tiêu chức năng"),
            new TriageTestCase("TC-SAFE-06", "Xin chào GastroAI, bạn có thể tư vấn sức khỏe tiêu hóa giúp tôi không?", false, "THÔNG THƯỜNG", "Câu chào hỏi giao tiếp"),
            new TriageTestCase("TC-SAFE-07", "Thang đo phân Bristol có mấy loại và loại nào là bình thường?", false, "THÔNG THƯỜNG", "Hỏi kiến thức y khoa chung"),
            new TriageTestCase("TC-SAFE-08", "Uống nước ấm vào buổi sáng có giúp cải thiện nhu động ruột không?", false, "THÔNG THƯỜNG", "Thói quen sinh hoạt tốt"),
            new TriageTestCase("TC-SAFE-09", "Cho em hỏi lịch uống thuốc kháng acid nên uống trước hay sau ăn?", false, "THÔNG THƯỜNG", "Cách dùng thuốc thông thường"),
            new TriageTestCase("TC-SAFE-10", "Dạ dày em hơi cồn cào khi đói, ăn nhẹ bánh mì thì hết", false, "THÔNG THƯỜNG", "Cồn cào khi đói bình thường"),
            new TriageTestCase("TC-SAFE-11", "Tôi muốn tìm hiểu các bài tập yoga tốt cho người bệnh đại tràng", false, "THÔNG THƯỜNG", "Lối sống và thể dục"),
            new TriageTestCase("TC-SAFE-12", "Trẻ sơ sinh bú mẹ đi ngoài hoa cà hoa cải có sao không?", false, "THÔNG THƯỜNG", "Câu hỏi nhi khoa thông thường"),

            // === NHÓM GÂY NHẦM LẪN / DỄ BÁO ĐỘNG GIẢ (BOUNDARY & DISTRACTOR CASES - ÂM TÍNH) ===
            new TriageTestCase("TC-DIST-01", "Tôi đi xét nghiệm máu định kỳ có phát hiện được viêm dạ dày HP không?", false, "GÂY NHẦM LẪN (CÓ TỪ MÁU)", "Chứa từ 'máu' nhưng là 'xét nghiệm máu'"),
            new TriageTestCase("TC-DIST-02", "Mẹ tôi bị cao huyết áp thì nên ăn uống thế nào khi bị đau dạ dày?", false, "GÂY NHẦM LẪN (HUYẾT ÁP)", "Chứa từ 'huyết áp' nhưng là bệnh nền"),
            new TriageTestCase("TC-DIST-03", "Hôm qua tôi ăn tiết canh heo, nay đi cầu màu sẫm có phải bị bệnh không?", false, "GÂY NHẦM LẪN (MÀU PHÂN DO THỰC PHẨM)", "Ăn tiết canh đổi màu phân, không phải phân đen tarry"),
            new TriageTestCase("TC-DIST-04", "Tôi bị sốt nhẹ 37.5 độ sau khi tiêm phòng vắc xin, bụng hơi râm ran", false, "GÂY NHẦM LẪN (SỐT NHẸ)", "Sốt nhẹ sau vắc xin, không sốt cao cấp cứu"),
            new TriageTestCase("TC-DIST-05", "Bác sĩ cho hỏi tiêm phòng viêm gan B ở trung tâm y tế nào gần nhất?", false, "GÂY NHẦM LẪN (BỆNH GAN)", "Hỏi địa điểm tiêm vắc xin"),
            new TriageTestCase("TC-DIST-06", "Hôm qua tôi đi hiến máu nhân đạo xong thấy hơi mệt nhẹ", false, "GÂY NHẦM LẪN (HIẾN MÁU)", "Chứa từ 'máu' nhưng là 'hiến máu'"),
            new TriageTestCase("TC-DIST-07", "Tôi bị xước ngón tay chảy ít máu khi gọt hoa quả, không liên quan tiêu hóa", false, "GÂY NHẦM LẪN (CHẢY MÁU TAY)", "Chảy máu ngoài da không phải xuất huyết tiêu hóa"),
            new TriageTestCase("TC-DIST-08", "Uống nước ép củ dền làm phân đỏ có nguy hiểm không?", false, "GÂY NHẦM LẪN (CỦ DỀN)", "Màu đỏ do củ dền"),
            new TriageTestCase("TC-DIST-09", "Uống viên sắt bổ máu làm phân sẫm màu có bình thường không?", false, "GÂY NHẦM LẪN (UỐNG SẮT)", "Uống thuốc sắt làm sẫm màu phân"),
            new TriageTestCase("TC-DIST-10", "Tôi ăn nhiều ớt cay quá nên hậu môn hơi rát khi đi vệ sinh", false, "GÂY NHẦM LẪN (RÁT HẬU MÔN DO ỚT)", "Rát do đồ cay nóng")
    );

    /**
     * Thực hiện kiểm thử toàn diện trên tập câu hỏi và sinh báo cáo định lượng.
     */
    public TriageEvaluationReport evaluate() {
        return evaluate(BENCHMARK_CASES);
    }

    public TriageEvaluationReport evaluate(List<TriageTestCase> testCases) {
        int tp = 0;
        int fn = 0;
        int fp = 0;
        int tn = 0;

        List<TriageCaseEvalResult> details = new ArrayList<>(testCases.size());
        Map<String, int[]> categoryStats = new LinkedHashMap<>(); // [total, tp, fn]

        for (TriageTestCase tc : testCases) {
            TriageResult result = triageService.check(tc.query());
            boolean predicted = result.emergency();
            String classification;

            if (tc.expectedEmergency() && predicted) {
                classification = "TP";
                tp++;
            } else if (tc.expectedEmergency() && !predicted) {
                classification = "FN"; // Nguy hiểm nhất trong y tế!
                fn++;
            } else if (!tc.expectedEmergency() && predicted) {
                classification = "FP"; // Báo động giả
                fp++;
            } else {
                classification = "TN";
                tn++;
            }

            details.add(new TriageCaseEvalResult(
                    tc.id(),
                    tc.query(),
                    tc.expectedEmergency(),
                    predicted,
                    result.matchedGroups(),
                    classification,
                    tc.category(),
                    tc.notes()
            ));

            if (tc.expectedEmergency()) {
                categoryStats.putIfAbsent(tc.category(), new int[]{0, 0, 0});
                int[] stats = categoryStats.get(tc.category());
                stats[0]++; // total
                if (predicted) stats[1]++; // tp
                else stats[2]++; // fn
            }
        }

        int total = testCases.size();
        int positiveCount = tp + fn;
        int negativeCount = fp + tn;

        // Tính toán các chỉ số thống kê y sinh
        double recall = positiveCount > 0 ? (double) tp / positiveCount : 1.0;
        double falseAlarmRate = negativeCount > 0 ? (double) fp / negativeCount : 0.0;
        double specificity = negativeCount > 0 ? (double) tn / negativeCount : 1.0;
        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 1.0;
        double accuracy = total > 0 ? (double) (tp + tn) / total : 1.0;

        double f1Score = (precision + recall) > 0
                ? 2 * (precision * recall) / (precision + recall)
                : 0.0;

        // F2-Score: Đặt trọng số Recall cao gấp đôi Precision (tiêu chuẩn đánh giá hệ thống an toàn y tế)
        double f2Score = (4 * precision + recall) > 0
                ? (5 * precision * recall) / (4 * precision + recall)
                : 0.0;

        List<CategoryMetric> categoryMetrics = categoryStats.entrySet().stream()
                .map(e -> {
                    int catTotal = e.getValue()[0];
                    int catTp = e.getValue()[1];
                    int catFn = e.getValue()[2];
                    double catRecall = catTotal > 0 ? (double) catTp / catTotal : 1.0;
                    return new CategoryMetric(e.getKey(), catTotal, catTp, catFn, round(catRecall, 4));
                })
                .collect(Collectors.toList());

        TriageEvaluationReport report = new TriageEvaluationReport(
                total,
                positiveCount,
                negativeCount,
                tp,
                fn,
                fp,
                tn,
                round(recall, 4),
                round(falseAlarmRate, 4),
                round(specificity, 4),
                round(precision, 4),
                round(accuracy, 4),
                round(f1Score, 4),
                round(f2Score, 4),
                categoryMetrics,
                details,
                Instant.now()
        );

        this.cachedReport = report;
        return report;
    }

    public TriageEvaluationReport getLatestReport() {
        if (cachedReport == null) {
            return evaluate();
        }
        return cachedReport;
    }

    private static double round(double val, int places) {
        double scale = Math.pow(10, places);
        return Math.round(val * scale) / scale;
    }
}
