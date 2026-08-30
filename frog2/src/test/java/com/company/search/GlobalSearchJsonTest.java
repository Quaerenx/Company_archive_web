package com.company.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class GlobalSearchJsonTest {
    @Test
    void encodesResultsWithTheCurrentContextPath() {
        String json = GlobalSearchJson.encode(
                "조폐\"공사",
                "/frog2",
                new GlobalSearchOutcome(
                        List.of(new GlobalSearchResult(
                                "고객사",
                                "조폐\"공사",
                                "Vertica 12.0.2-1",
                                "/customers?view=detail&customerName=%EC%A1%B0%ED%8F%90")),
                        List.of("자료실")));

        assertEquals(
                "{\"query\":\"조폐\\\"공사\",\"partial\":true,"
                        + "\"unavailableCategories\":[\"자료실\"],\"results\":[{"
                        + "\"category\":\"고객사\","
                        + "\"label\":\"조폐\\\"공사\","
                        + "\"description\":\"Vertica 12.0.2-1\","
                        + "\"url\":\"/frog2/customers?view=detail&customerName=%EC%A1%B0%ED%8F%90\"}]}",
                json);
    }

    @Test
    void identifiesWhenEverySearchSourceIsUnavailable() {
        GlobalSearchOutcome outcome = new GlobalSearchOutcome(
                List.of(),
                List.of(
                        "고객사",
                        "고객사 히스토리",
                        "트러블슈팅",
                        "회의록",
                        "자료실"));

        assertEquals(true, outcome.partial());
        assertEquals(true, outcome.allSourcesUnavailable());
    }

    @Test
    void rejectsExternalOrProtocolRelativePaths() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GlobalSearchResult(
                        "고객사", "Acme", "", "https://example.com"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GlobalSearchResult(
                        "고객사", "Acme", "", "//example.com"));
    }
}
