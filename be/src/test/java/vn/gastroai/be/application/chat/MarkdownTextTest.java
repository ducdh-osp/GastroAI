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
}