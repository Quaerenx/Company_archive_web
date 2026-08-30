package com.company.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SearchQueryPolicyTest {
    @Test
    void trimsAndBoundsSearchQueriesByCodePointLength() {
        assertNull(SearchQueryPolicy.normalize(null));
        assertNull(SearchQueryPolicy.normalize("   "));
        assertEquals("고객", SearchQueryPolicy.normalize("  고객  "));
        assertThrows(
                IllegalArgumentException.class,
                () -> SearchQueryPolicy.normalize("a"));
        assertThrows(
                IllegalArgumentException.class,
                () -> SearchQueryPolicy.normalize("가".repeat(101)));
    }

    @Test
    void buildsLiteralLikeAndRegexPatterns() {
        assertEquals(
                "%need!%!_!!le%",
                SearchQueryPolicy.literalContainsLikePattern(
                        "need%_!le"));
        assertEquals(
                "need\\x{20}\\[le\\]\\.\\*\\#",
                SearchQueryPolicy.literalContainsRegex(
                        "need [le].*#"));
    }
}
