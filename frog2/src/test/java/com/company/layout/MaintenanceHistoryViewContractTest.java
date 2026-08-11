package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MaintenanceHistoryViewContractTest {
    private static final Path HISTORY = Path.of(
            "src/main/webapp/maintenance/maintenance_history.jsp");

    @Test
    void historyUsesBoundedPageMetadataAndPreservesTheCustomerInLinks()
            throws Exception {
        String page = Files.readString(HISTORY);

        assertTrue(page.contains("${totalCount}"));
        assertFalse(page.contains("${records.size()}"));
        assertTrue(page.contains("maintenanceHistoryPreviousUrl"));
        assertTrue(page.contains("maintenanceHistoryNextUrl"));
        assertTrue(page.contains("name=\"historyPage\""));
        assertTrue(page.contains("name=\"customerName\" value=\"${customerName}\""));
        assertTrue(page.contains("<t:tableFooter"));
        assertTrue(page.contains("paginationLabel=\"정기점검 이력 페이지\""));
    }

    @Test
    void licenseChartHasAReadableSummaryAndServerRenderedDataTable()
            throws Exception {
        String page = Files.readString(HISTORY);

        assertTrue(page.contains("id=\"licenseUsageChartTitle\""));
        assertTrue(page.contains("id=\"licenseUsageChartSummary\""));
        assertTrue(page.contains("role=\"img\""));
        assertTrue(page.contains("aria-labelledby=\"licenseUsageChartTitle\""));
        assertTrue(page.contains("aria-describedby=\"licenseUsageChartSummary\""));
        assertTrue(page.contains("<details class=\"chart-data-details\""));
        assertTrue(page.contains("<table class=\"chart-data-table ui-table\""));
        assertTrue(page.contains(
                "<caption>라이선스 사용률 추이 상세 데이터</caption>"));
        assertEquals(4, occurrences(page, "<th scope=\"col\""));
        assertTrue(page.contains("<c:forEach var=\"pt\" items=\"${usageSeries}\">"));
        assertTrue(page.contains("<c:out value=\"${pt.date}\" />"));
        assertTrue(page.contains(
                "<c:out value=\"${pt.pct}\" default=\"-\" />"));
        assertTrue(page.contains(
                "<c:out value=\"${pt.usedTb}\" default=\"-\" />"));
        assertTrue(page.contains(
                "<c:out value=\"${pt.sizeTb}\" default=\"-\" />"));
    }

    private static int occurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
