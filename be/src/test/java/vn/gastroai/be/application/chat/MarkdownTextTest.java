package vn.gastroai.be.application.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarkdownTextTest {

    @Test
    void convertsHeadingToPlainText() {
        assertEquals("Lời khuyên", MarkdownText.toPlain("## Lời khuyên"));
    }

    @Test
    void convertsBulletToDot() {
        assertEquals("• Uống nước", MarkdownText.toPlain("- Uống nước"));
    }

    @Test
    void removesBoldMarkers() {
        assertEquals("Lưu ý: nghỉ ngơi", MarkdownText.toPlain("**Lưu ý:** nghỉ ngơi"));
    }

    @Test
    void convertsLinkToTextWithUrlInParentheses() {
        assertEquals("Bộ Y tế (https://moh.gov.vn)",
                MarkdownText.toPlain("[Bộ Y tế](https://moh.gov.vn)"));
    }

    @Test
    void keepsNumberedListAsIs() {
        assertEquals("1. Bước một", MarkdownText.toPlain("1. Bước một"));
    }

    @Test
    void nullBecomesEmptyString() {
        assertEquals("", MarkdownText.toPlain(null));
    }
        @Test
    void removesHorizontalRule() {
        assertEquals("", MarkdownText.toPlain("---"));
    }

    @Test
    void removesItalicMarkers() {
        assertEquals("nghiêng", MarkdownText.toPlain("*nghiêng*"));
    }

    // ===== UC0068 - 5 truong hop reviewer bao sai, xem bang trong yeu cau cua Duc =====

    @Test
    void doesNotItalicizeAsteriskUsedAsMultiplicationSign() {
        // "2 * 3 lần" - dau * co dau cach hai ben, khong phai in nghieng.
        assertEquals("2 * 3 lần", MarkdownText.toPlain("2 * 3 lần"));
    }

    @Test
    void doesNotItalicizeAsteriskBetweenWordsOrNumbers() {
        // Tung quat hon: * ke ngay chu/so (khong co khoang trang) cung khong phai in nghieng.
        assertEquals("giá 100*200 đồng", MarkdownText.toPlain("giá 100*200 đồng"));
        assertEquals("a*b*c (biểu thức)", MarkdownText.toPlain("a*b*c (biểu thức)"));
    }

    @Test
    void headingRequiresSpaceAfterHashMarks() {
        // "#1 nguyên nhân" khong co dau cach sau # nen KHONG phai de muc, giu nguyen chu thuong.
        assertEquals("#1 nguyên nhân", MarkdownText.toPlain("#1 nguyên nhân"));
    }

    @Test
    void convertsPlusBulletToDotSameAsDashAndAsterisk() {
        assertEquals("• Ăn chín", MarkdownText.toPlain("+ Ăn chín"));
    }

    @Test
    void keepsEscapedAsteriskAsLiteralTextWithoutBackslash() {
        // \*dac biet\* - dau \ la ky tu thoat, phai giu lai dau * nhu chu thuong (khong in
        // nghieng, khong con dau \).
        assertEquals("Giá *đặc biệt*", MarkdownText.toPlain("Giá \\*đặc biệt\\*"));
    }

    @Test
    void keepsEscapedHashAsLiteralTextInsteadOfHeading() {
        assertEquals("# không phải đề mục", MarkdownText.toPlain("\\# không phải đề mục"));
    }

    @Test
    void convertsTableRowToPipeSeparatedPlainText() {
        assertEquals("Thuốc | Liều", MarkdownText.toPlain("| Thuốc | Liều |"));
    }

    @Test
    void removesMarkdownTableSeparatorRow() {
        assertEquals("", MarkdownText.toPlain("|---|---|"));
        // Co dau hai cham can le (:---, ---:) van la dong ke bang, phai bo.
        assertEquals("", MarkdownText.toPlain("| :--- | ---: |"));
    }

    @Test
    void stripsBoldMarkersInsideTableCell() {
        assertEquals("Thuốc | Liều", MarkdownText.toPlain("| **Thuốc** | Liều |"));
    }
}