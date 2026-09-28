package vn.gastroai.be.application.document;

import org.junit.jupiter.api.Test;
import vn.gastroai.be.domain.rag.Chunk;
import vn.gastroai.be.domain.rag.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC0032 - xac nhan fix bao cao cua Duc: chunk khong duoc gop nhieu muc danh so khac
 * chu de vao chung 1 chunk, du cac muc dinh lien nhau khong co dong trong ngan cach
 * (day chinh la nguyen nhan chunk that bi loi trong PDF viem-dai-trang-man.pdf).
 */
class ChunkingServiceTest {

    private final ChunkingService chunkingService = new ChunkingService();

    private static Document documentWithText(String fullText) {
        Document document = new Document();
        document.setId(1L);
        document.setFullText(fullText);
        return document;
    }

    @Test
    void splitsAtSectionHeadingsEvenWithoutBlankLineBetweenThem() {
        // Mo phong dung tinh huong Duc bao cao: 3 muc dinh lien nhau, KHONG co dong
        // trong ngan cach giua muc nay va muc ke tiep (PDF thuc te hay bi vay khi
        // extract xuyen qua ranh gioi trang).
        String text = """
                2.2.2 Viem dai trang do amibe
                Xet nghiem: soi phan tim the ke hoac the tu duong.
                2.2.3 Viem dai trang mang gia
                Xet nghiem thuong qui: CTM, duong huyet, chuc nang gan.
                2.2.4 Viem tui thua dai trang
                Xet nghiem thuong qui: CTM, CRP.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        // Diem quan trong nhat: 3 muc benh khac nhau PHAI nam o 3 chunk rieng, khong
        // duoc gop chung - day chinh la bug that su (chunk dinh nhieu chu de).
        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).getContent().startsWith("2.2.2"));
        assertTrue(chunks.get(1).getContent().startsWith("2.2.3"));
        assertTrue(chunks.get(2).getContent().startsWith("2.2.4"));

        // Moi chunk chi chua DUNG 1 muc, khong lan sang muc ke ben.
        assertTrue(chunks.get(0).getContent().contains("amibe"));
        assertFalse(chunks.get(0).getContent().toLowerCase().contains("mang gia"));
    }

    @Test
    void keepsShortUnrelatedParagraphsTogetherWhenNoHeadingPresent() {
        // Doi chung: van ban KHONG co muc danh so thi van gop nhu cu theo do dai -
        // fix nay khong duoc lam vo hieu logic gop doan ngan da chay dung tu truoc.
        String text = """
                Doan mo dau ngan gon ve benh tieu hoa noi chung.

                Doan thu hai tiep tuc noi ve trieu chung thuong gap.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getContent().contains("mo dau"));
        assertTrue(chunks.get(0).getContent().contains("trieu chung"));
    }

    @Test
    void doesNotMistakeNumericDataForSectionHeading() {
        // "10 mg" hay so lieu dau dong khong duoc nham thanh heading (heading that phai
        // co dang X.Y hoac X.Y.Z, khong phai so don le).
        String text = """
                2.2.5 Dieu tri
                Lieu dung khuyen cao:
                10 mg moi ngay, chia 2 lan uong sau an.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).getContent().contains("10 mg"));
    }

    @Test
    void splitsAtFontHeadingMarkerAndStripsItFromFinalContent() {
        // PdfTextExtractor/DocxTextExtractor chen "## " vao dau dong khi phat hien do la
        // tieu de dua tren font (chu to/in dam), de bat ca tieu de KHONG danh so - mo
        // phong dung dau ra cua extractor sau khi no da danh dau.
        String text = """
                ## BIEN CHUNG THUONG GAP
                Xuat huyet tieu hoa la bien chung pho bien nhat, can theo doi sat.
                ## DIEU TRI
                Dieu tri noi khoa la lua chon dau tay cho hau het cac truong hop benh.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        // 2 tieu de khong danh so PHAI tach thanh 2 chunk rieng, giong het tieu de danh so.
        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).getContent().startsWith("BIEN CHUNG THUONG GAP"));
        assertTrue(chunks.get(1).getContent().startsWith("DIEU TRI"));

        // Marker chi la tin hieu noi bo - khong duoc lo ra trong noi dung chunk luu vao DB.
        assertFalse(chunks.get(0).getContent().contains("##"));
        assertFalse(chunks.get(1).getContent().contains("##"));
    }

    @Test
    void mergesHeadingOnlyChunkIntoFollowingChunk() {
        // Phat hien tu du lieu THAT cua file viem-dai-trang-man.pdf: tieu de CHA (vd
        // "4. DIEU TRI") khong co noi dung rieng - toan bo noi dung nam o muc con ngay
        // sau ("4.1"), nhung muc con do CUNG la ranh gioi moi nen bi cat rieng, de lai
        // tieu de cha tro troi mot minh (chi 11 ky tu, gan nhu vo nghia cho RAG).
        String text = """
                ## 4. DIEU TRI
                4.1 Dieu tri noi khoa la phuong phap dieu tri chinh cho hau het cac truong hop nhe.
                4.2 Dieu tri ngoai khoa duoc chi dinh khi co bien chung nang hon.
                """;

        Document document = documentWithText(text);

        List<Chunk> chunks = chunkingService.chunk(document);

        // Tieu de cha PHAI duoc ghep vao chunk ke tiep (4.1), khong dung rieng mot minh.
        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).getContent().startsWith("4. DIEU TRI"));
        assertTrue(chunks.get(0).getContent().contains("4.1 Dieu tri noi khoa"));
        assertTrue(chunks.get(1).getContent().startsWith("4.2"));
    }
}