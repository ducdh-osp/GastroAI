package vn.gastroai.be.application.rag;

import org.springframework.stereotype.Service;
import vn.gastroai.be.application.config.AppRuntimeConfigService;
import vn.gastroai.be.application.config.AppRuntimeConfigSnapshot;
import vn.gastroai.be.infrastructure.ai.GeminiChatClient;
import vn.gastroai.be.infrastructure.ai.GeminiEmbeddingClient;
import vn.gastroai.be.infrastructure.ai.ImagePart;
import vn.gastroai.be.infrastructure.rag.EmbeddingStore;
import vn.gastroai.be.infrastructure.rag.SimilarChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * UC0028 + UC0029 nối liền nhau: sinh embedding cho câu hỏi -> truy xuất top-k
 * chunk gần nhất
 * -> đưa vào prompt cho Gemini sinh câu trả lời. Đây là "lõi" RAG — chưa gắn
 * API/controller
 * nào (đó là việc của UC0017 Chat với AI, ngoài phạm vi 3 UC này), chỉ cần gọi
 * answer() là
 * test được toàn bộ pipeline.
 */
@Service
public class RagQueryService {
    // Van dung du topK chunk de sinh cau tra loi (nhieu ngu canh hon = tra loi day
    // du hon),
    // nhung chi hien toi da 2 nguon dau tien (da sap xep gan nhat truoc, xem
    // EmbeddingStore.findTopK) cho FE - hien het ca 5 nguon lam UI dai/roi mat,
    // trong khi 2
    // nguon lien quan nhat da du de nguoi dung tin tuong cau tra loi co can cu.
    private static final int MAX_DISPLAYED_SOURCES = 2;

    private final GeminiEmbeddingClient embeddingClient;
    private final EmbeddingStore embeddingStore;
    private final GeminiChatClient chatClient;
    private final AppRuntimeConfigService appRuntimeConfigService;

    public RagQueryService(
            GeminiEmbeddingClient embeddingClient,
            EmbeddingStore embeddingStore,
            GeminiChatClient chatClient,
            AppRuntimeConfigService appRuntimeConfigService) {
        this.embeddingClient = embeddingClient;
        this.embeddingStore = embeddingStore;
        this.chatClient = chatClient;
        this.appRuntimeConfigService = appRuntimeConfigService;
    }

    public String answer(String question) {
        return answerWithSources(question).answer();
    }

    /**
     * UC0029 - nhu answer(), nhung tra ve them danh sach nguon (RagSource) da dung
     * de sinh cau tra loi.
     */
    public RagAnswer answerWithSources(String question) {
        return answerWithSources(question, List.of(), null);
    }

    /**
     * images/attachedDocumentText: anh/tai lieu benh nhan dinh kem tin nhan chat (khac tai
     * lieu trong kho tri thuc RAG o tren) - images di thang vao Gemini qua inlineData,
     * attachedDocumentText la text da trich xuat san (PdfTextExtractor/DocxTextExtractor) tu
     * file dinh kem dang pdf/docx, duoc noi them vao cuoi prompt nhu 1 doan ngu canh nua.
     */
    public RagAnswer answerWithSources(String question, List<ImagePart> images, String attachedDocumentText) {
        AppRuntimeConfigSnapshot config = appRuntimeConfigService.getConfig();
        float[] queryVector = embeddingClient.embed(question);
        List<SimilarChunk> context = applySimilarityThreshold(
                embeddingStore.findTopK(queryVector, config.ragTopK()), config.ragSimilarityThreshold());

        String attachmentContext = buildAttachmentContext(attachedDocumentText);

        if (context.isEmpty()) {
            // Chưa có tài liệu nào trong kho tri thức (hoặc UC0031/032 của Thăng chưa xong)
            // —
            // vẫn trả lời được nhưng phải nói rõ KHÔNG có nguồn, tránh Gemini tự bịa thông
            // tin
            // y tế mà không có căn cứ.
            String noContextPrompt = SYSTEM_PROMPT_NO_CONTEXT + attachmentContext;
            String noContextAnswer = chatClient.generate(noContextPrompt, question, images);
            return new RagAnswer(noContextAnswer, List.of(), generateRelatedQuestions(question, noContextAnswer));
        }

        String contextText = context.stream()
                .map(chunk -> "- " + DosageRedactor.redact(chunk.content()))
                .collect(Collectors.joining("\n"));
        String systemPrompt = SYSTEM_PROMPT_PREFIX + contextText + attachmentContext;
        String generatedAnswer = chatClient.generate(systemPrompt, question, images);

        List<RagSource> sources = buildSources(chatClient, context);
        return new RagAnswer(generatedAnswer, sources, generateRelatedQuestions(question, generatedAnswer));
    }
     /**
     * Tach rieng de StreamingRagQueryService dung lai y het - dam bao luong
     * stream va luong thuong xu ly tai lieu dinh kem (ke ca buoc che lieu
     * thuoc qua DosageRedactor) giong nhau tuyet doi, khong lech nhau.
     */
    public static String buildAttachmentContext(String attachedDocumentText) {
        return (attachedDocumentText == null || attachedDocumentText.isBlank())
                ? ""
                : "\n\nNoi dung tai lieu benh nhan vua gui kem (co the chua lieu luong - van ap " +
                  "dung dung chi dan ve lieu luong o tren):\n" + DosageRedactor.redact(attachedDocumentText);
    }

    /**
     * UC0059 - loc bo chunk qua xa so voi cau hoi (distance > threshold), ngoai viec gioi han
     * so luong qua top-k. threshold=null (mac dinh cu, chua config qua CMS) giu nguyen hanh vi
     * cu - khong loc gi them, chi lay top-k. Tach rieng de StreamingRagQueryService dung lai y
     * het, giong buildAttachmentContext() o tren.
     */
    public static List<SimilarChunk> applySimilarityThreshold(List<SimilarChunk> chunks, Double threshold) {
        if (threshold == null) {
            return chunks;
        }
        return chunks.stream().filter(chunk -> chunk.distance() <= threshold).toList();
    }

    /**
     * Danh sach nguon hien cho nguoi dung (UC0029) - chunk goc bi cat theo so tu luc chunking
     * (UC0031/032) nen co the bat dau/ket thuc giua cau, lai cong voi loi chinh ta co san trong
     * tai lieu goc. Goi Gemini VIET LAI cho de doc hon (sua cau cut, loi chinh ta) truoc khi
     * hien - neu buoc nay loi/tra ve sai so luong thi fallback ve chunk da che lieu nguyen van,
     * GIONG HET pattern cua generateRelatedQuestions: khong duoc lam hong cau tra loi chinh.
     * Tach static de StreamingRagQueryService dung lai y het.
     */
    public static List<RagSource> buildSources(GeminiChatClient chatClient, List<SimilarChunk> context) {
        List<SimilarChunk> topChunks = context.stream().limit(MAX_DISPLAYED_SOURCES).toList();
        List<String> redactedOriginals = topChunks.stream()
                .map(chunk -> DosageRedactor.redact(chunk.content()))
                .toList();
        List<String> readable = rewriteSnippetsForReadability(chatClient, redactedOriginals);

        List<RagSource> sources = new ArrayList<>();
        for (int i = 0; i < topChunks.size(); i++) {
            sources.add(new RagSource(topChunks.get(i).documentTitle(), readable.get(i), topChunks.get(i).sourceUrl()));
        }
        return sources;
    }

    private static final Pattern SOURCE_MARKER = Pattern.compile("===\\s*(\\d+)\\s*===");

    private static List<String> rewriteSnippetsForReadability(GeminiChatClient chatClient, List<String> redactedOriginals) {
        if (redactedOriginals.isEmpty()) {
            return redactedOriginals;
        }
        try {
            StringBuilder prompt = new StringBuilder();
            for (int i = 0; i < redactedOriginals.size(); i++) {
                prompt.append("===").append(i + 1).append("===\n").append(redactedOriginals.get(i)).append('\n');
            }
            String raw = chatClient.generate(SYSTEM_PROMPT_REWRITE_SOURCES, prompt.toString());
            List<String> rewritten = parseNumberedBlocks(raw, redactedOriginals.size());
            if (rewritten.size() != redactedOriginals.size()) {
                // Gemini tra ve khong du/sai dinh dang - khong doan mo, dung nguyen ban goc.
                return redactedOriginals;
            }
            // Che lieu lan 2 tren chinh VAN BAN DA VIET LAI - phong truong hop Gemini vo tinh
            // dien lai so lieu/lieu luong khi "lam muot" cau chu (xem DosageRedactor javadoc).
            return rewritten.stream().map(DosageRedactor::redact).toList();
        } catch (RuntimeException exception) {
            return redactedOriginals;
        }
    }

    private static List<String> parseNumberedBlocks(String raw, int expectedCount) {
        Matcher matcher = SOURCE_MARKER.matcher(raw);
        // markerStarts[i] = vi tri bat dau cua marker "===N===" thu i, contentStarts[i] = vi tri
        // ngay sau marker do - noi dung cua khoi i la tu contentStarts[i] den markerStarts[i+1]
        // (hoac het chuoi voi khoi cuoi), tranh an ca marker tiep theo vao noi dung.
        List<Integer> markerStarts = new ArrayList<>();
        List<Integer> contentStarts = new ArrayList<>();
        while (matcher.find()) {
            markerStarts.add(matcher.start());
            contentStarts.add(matcher.end());
        }
        List<String> blocks = new ArrayList<>();
        for (int i = 0; i < contentStarts.size(); i++) {
            int start = contentStarts.get(i);
            int end = i + 1 < markerStarts.size() ? markerStarts.get(i + 1) : raw.length();
            String block = raw.substring(start, Math.min(end, raw.length())).trim();
            if (!block.isBlank()) {
                blocks.add(block);
            }
        }
        return blocks.size() == expectedCount ? blocks : List.of();
    }

    /**
     * Goi y cau hoi lien quan la tinh nang phu (khong phai ly do chinh nguoi dung
     * hoi) - goi
     * rieng 1 lan Gemini SAU KHI da co cau tra loi chinh, thay vi gop chung vao 1
     * lan goi, de
     * khong dung vao prompt/logic sinh cau tra loi chinh dang chay on dinh. Neu
     * buoc nay loi
     * (Gemini timeout, tra ve rong...) chi tra danh sach rong - KHONG duoc lam hong
     * cau tra
     * loi chinh da co san.
     */
    private List<String> generateRelatedQuestions(String question, String answer) {
        try {
            String userPrompt = "Cau hoi: " + question + "\nCau tra loi: " + answer;
            String raw = chatClient.generate(SYSTEM_PROMPT_RELATED_QUESTIONS, userPrompt);
            return raw.lines()
                    .map(line -> line.replaceFirst("^[-*\\d.)\\s]+", "").trim())
                    .filter(line -> !line.isBlank())
                    .limit(3)
                    .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static final String SYSTEM_PROMPT_PREFIX = """
            Ban la tro ly AI cua GastroAI, chuyen tu van ve suc khoe tieu hoa. CHI tra loi dua \
            tren ngu canh duoc cung cap ben duoi, khong tu bia them thong tin y khoa. Neu ngu \
            canh khong du de tra loi, hay noi ro dieu do va khuyen nguoi dung gap bac si. Luon \
            nhac nguoi dung day chi la thong tin tham khao, khong thay the chan doan y te.

            Ngu canh co the trich tu tai lieu chuyen mon danh cho bac si (phac do dieu tri), \
            KHONG danh cho benh nhan tu ap dung. Neu ngu canh co lieu luong thuoc cu the (so mg, \
            so vien/ngay, so lan/ngay...), TUYET DOI khong doc lai nguyen lieu do cho nguoi dung. \
            Chi noi chung la can dung thuoc theo dung chi dinh cua bac si/duoc si, va khuyen \
            nguoi dung hoi truc tiep bac si/duoc si de biet lieu luong chinh xac phu hop voi ho.

            Ngu canh:
            """;

    private static final String SYSTEM_PROMPT_NO_CONTEXT = """
            Ban la tro ly AI cua GastroAI, chuyen tu van ve suc khoe tieu hoa. Kho tri thuc \
            hien chua co tai lieu nao lien quan. Hay noi ro la chua co du lieu de tra loi \
            chinh xac, va khuyen nguoi dung gap bac si neu can thiet. Khong tu bia thong tin y khoa.
            """;

    private static final String SYSTEM_PROMPT_REWRITE_SOURCES = """
            Ban se nhan mot hoac vai doan trich NGUYEN VAN tu tai lieu y khoa, moi doan danh so \
            theo dinh dang "===N===". Cac doan nay bi cat theo so tu luc xu ly tai lieu nen co \
            the bat dau/ket thuc giua cau, va co the lan loi chinh ta co san trong tai lieu goc.

            Hay viet lai TUNG doan cho ro rang, mach lac va de doc hon: noi lien cau bi cat o \
            dau/cuoi doan, sua loi chinh ta/dau cau, nhung TUYET DOI KHONG duoc them, bot, suy \
            dien, hay dien vao bat ky thong tin y khoa/so lieu/lieu luong nao khong co san trong \
            doan goc - kem ca khi doan goc dang bi cat cut, cung KHONG duoc tu "doan" noi dung \
            con thieu. Neu 1 doan da du ro rang thi giu gan nhu nguyen van.

            Tra ve DUNG so doan vua nhan, DUNG dinh dang danh so "===N===" nhu dau vao (N bat dau \
            tu 1), khong them loi dan, khong giai thich gi them ngoai noi dung da viet lai cua \
            tung doan.
            """;

    private static final String SYSTEM_PROMPT_RELATED_QUESTIONS = """
            Dua tren cau hoi va cau tra loi tu van suc khoe tieu hoa duoi day, hay de xuat dung \
            3 cau hoi lien quan ma nguoi dung co the muon hoi tiep. Moi cau hoi ngan gon, viet \
            tren 1 dong rieng, KHONG danh so, KHONG them giai thich hay ky tu nao khac ngoai \
            chinh cau hoi.
            """;
}
