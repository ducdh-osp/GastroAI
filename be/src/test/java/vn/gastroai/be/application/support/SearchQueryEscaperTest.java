package vn.gastroai.be.application.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchQueryEscaperTest {
    @Test
    void escapesEscapeCharacterAndSqlLikeWildcards() {
        assertEquals("100!%!_!\\!!", SearchQueryEscaper.escapeLike("100%_\\!"));
    }
}
