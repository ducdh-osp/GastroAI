package vn.gastroai.be.application.support;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VietnameseTextTest {

    @Test
    void normalizeLowercasesAndStripsDiacriticsIncludingDinhStroke() {
        assertEquals("dau bung du doi", VietnameseText.normalize("Đau Bụng Dữ Dội"));
    }

    @Test
    void normalizeHandlesNullAsEmptyString() {
        assertEquals("", VietnameseText.normalize(null));
    }

    @Test
    void tokenizeExtractsFoodKeywordsAndDropsStopwordsAndNumbers() {
        Set<String> tokens = VietnameseText.tokenize("2 bát phở bò và một chút rau thơm");
        assertTrue(tokens.contains("pho"));
        assertTrue(tokens.contains("bo"));
        assertTrue(tokens.contains("rau"));
        assertTrue(tokens.contains("thom"));
        assertFalse(tokens.contains("2"));
        assertFalse(tokens.contains("bat"));
        assertFalse(tokens.contains("va"));
        assertFalse(tokens.contains("mot"));
        assertFalse(tokens.contains("chut"));
    }

    @Test
    void tokenizeDeduplicatesRepeatedWordInSameSentence() {
        Set<String> tokens = VietnameseText.tokenize("canh chua nấu canh cá");
        long canhCount = tokens.stream().filter(t -> t.equals("canh")).count();
        assertEquals(1, canhCount);
    }

    @Test
    void tokenizeReturnsEmptySetForBlankOrNull() {
        assertTrue(VietnameseText.tokenize(null).isEmpty());
        assertTrue(VietnameseText.tokenize("   ").isEmpty());
    }

    @Test
    void tokenizeDropsSingleCharacterTokens() {
        Set<String> tokens = VietnameseText.tokenize("ăn ô mai");
        assertFalse(tokens.contains("o"));
        assertTrue(tokens.contains("mai"));
    }
}
