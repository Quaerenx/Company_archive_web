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
        assertTrue(page.contains(
                "name=\"customerName\" value=\"${fn:escapeXml(customerName)}\""));
        assertTrue(page.contains("class=\"history-filter-form\""));
        assertTrue(page.contains("name=\"historyYear\""));
        assertTrue(page.contains("name=\"historyVersion\""));
        assertTrue(page.contains("name=\"historyQuery\""));
        assertTrue(page.contains(
                "<c:param name=\"historyYear\" value=\"${historyYear}\" />"));
        assertTrue(page.contains(
                "<c:param name=\"historyVersion\" value=\"${historyVersion}\" />"));
        assertTrue(page.contains(
                "<c:param name=\"historyQuery\" value=\"${historyQuery}\" />"));
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
        assertTrue(page.contains("class=\"usage-chart-scroll\""));
        assertTrue(page.contains("data-ui-scroll-region"));
        assertTrue(page.contains(
                "data-ui-scroll-label=\"라이선스 사용률 추이 차트\""));
        assertTrue(page.contains("class=\"usage-chart-canvas\""));
        assertTrue(page.contains("<details class=\"chart-data-details\""));
        assertTrue(page.contains("<table class=\"chart-data-table ui-table\""));
        assertTrue(page.contains(
                "<caption>라이선스 사용률 추이 상세 데이터</caption>"));
        int chartTableStart = page.indexOf(
                "<table class=\"chart-data-table ui-table\"");
        int chartTableEnd = page.indexOf("</table>", chartTableStart);
        assertTrue(chartTableStart >= 0);
        assertTrue(chartTableEnd > chartTableStart);
        String chartTable = page.substring(chartTableStart, chartTableEnd);
        assertEquals(4, occurrences(chartTable, "<th scope=\"col\""));
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
