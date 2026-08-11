package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MaintenanceFormAssetContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path PAGE_SCRIPTS = WEBAPP.resolve("resources/js/pages");
    private static final Path PAGE_STYLES = WEBAPP.resolve("resources/css/pages");

    @Test
    void addAndEditPagesUseOneExplicitFormContract() throws Exception {
        String add = readWebapp("maintenance/maintenance_add.jsp");
        String edit = readWebapp("maintenance/maintenance_edit.jsp");

        assertTrue(add.contains("/resources/js/pages/maintenance_form.js"));
        assertTrue(edit.contains("/resources/js/pages/maintenance_form.js"));
        assertTrue(add.contains("data-maintenance-form-mode=\"add\""));
        assertTrue(edit.contains("data-maintenance-form-mode=\"edit\""));
        assertEquals(1, countOccurrences(add, "id=\"maintenanceForm\""));
        assertEquals(1, countOccurrences(edit, "id=\"maintenanceForm\""));
        assertTrue(edit.contains("id=\"deleteFormHeader\""));
        assertTrue(edit.contains("id=\"current_customer_value\""));
        assertTrue(edit.contains("id=\"current_inspector_value\""));
    }

    @Test
    void sharedScriptPreservesModeSpecificBehaviorAndFailsClosed() throws Exception {
        String script = Files.readString(PAGE_SCRIPTS.resolve("maintenance_form.js"));

        assertTrue(script.contains("(function()"));
        assertTrue(script.contains("getElementById('maintenanceForm')"));
        assertFalse(script.contains("querySelector('form')"));
        assertEquals(1, countOccurrences(
                script, "/customers?action=getCustomersForMaintenance"));
        assertTrue(script.contains("if (!response.ok)"));
        assertTrue(script.contains("function ensureOption("));
        assertTrue(script.contains("current_customer_value"));
        assertTrue(script.contains("current_inspector_value"));
        assertTrue(script.contains("getFullYear()"));
        assertTrue(script.contains("getMonth() + 1"));
        assertTrue(script.contains("getDate()"));
        assertFalse(script.contains("toISOString()"));
        assertTrue(script.contains("optionsUnavailable = true"));
        assertTrue(script.contains("고객사 및 점검자 정보를 불러오지 못했습니다."));
        assertFalse(script.contains("직접 입력"));
        assertTrue(script.contains("정말 삭제하시겠습니까?"));
    }

    @Test
    void obsoletePageScriptsAreRemoved() {
        assertFalse(Files.exists(PAGE_SCRIPTS.resolve("maintenance_add.js")));
        assertFalse(Files.exists(PAGE_SCRIPTS.resolve("maintenance_edit.js")));
    }

    @Test
    void historyScriptSkipsChartRenderingWhenVendorIsUnavailable() throws Exception {
        String history = Files.readString(PAGE_SCRIPTS.resolve("maintenance_history.js"));

        assertTrue(history.contains("typeof window.Chart !== 'function'"));
        assertTrue(history.contains("new window.Chart("));
        assertTrue(history.contains("usage: cssColor('--color-chart-usage')"));
        assertTrue(history.contains("used: cssColor('--color-chart-used')"));
        assertTrue(history.contains("capacity: cssColor('--color-chart-capacity')"));
        assertEquals(4, countOccurrences(history, "chartColors.usage"));
        assertEquals(4, countOccurrences(history, "chartColors.used"));
        assertEquals(4, countOccurrences(history, "chartColors.capacity"));
        assertTrue(history.contains("borderDash: [6, 4]"));
        assertTrue(history.contains("pointStyle: 'circle'"));
        assertTrue(history.contains("pointStyle: 'triangle'"));
        assertTrue(history.contains("pointStyle: 'rectRot'"));
        assertTrue(history.contains("prefers-reduced-motion: reduce"));
        assertTrue(history.contains(
                "animation: reduceMotion ? false : undefined"));
        assertFalse(history.contains(".history-item"));
        assertFalse(history.contains("item.animate"));
        assertFalse(history.contains("item.addEventListener('click'"));
        assertTrue(readWebapp("maintenance/maintenance_history.jsp")
                .contains("<table class=\"history-comparison-table\""));
    }

    @Test
    void historyRecordsUseAccessibleTwoRowComparisonGroups() throws Exception {
        String page = readWebapp("maintenance/maintenance_history.jsp");
        int tableStart = page.indexOf(
                "<table class=\"history-comparison-table\"");
        int tableEnd = page.indexOf("</table>", tableStart);
        assertTrue(tableStart >= 0);
        assertTrue(tableEnd > tableStart);
        String comparisonTable = page.substring(tableStart, tableEnd);

        assertTrue(comparisonTable.contains(
                "<caption class=\"sr-only\">"));
        assertEquals(7, countOccurrences(
                comparisonTable, "<th scope=\"col\""));
        assertTrue(comparisonTable.contains("점검월"));
        assertTrue(comparisonTable.contains("이전 대비"));
        assertTrue(comparisonTable.contains(
                "<c:forEach var=\"row\" items=\"${historyRows}\">"));
        assertTrue(comparisonTable.contains(
                "<tbody class=\"history-record-group\">"));
        assertTrue(comparisonTable.contains(
                "scope=\"rowgroup\" rowspan=\"2\""));
        assertTrue(comparisonTable.contains(
                "<tr class=\"history-metric-row\">"));
        assertTrue(comparisonTable.contains(
                "<tr class=\"history-note-row\">"));
        assertTrue(comparisonTable.contains(
                "<td class=\"history-note-cell\" colspan=\"6\">"));
        assertTrue(comparisonTable.contains(
                "<div class=\"history-note-layout\">"));
        assertTrue(comparisonTable.contains(
                "<c:out value=\"${row.record.note}\" />"));
        assertTrue(comparisonTable.contains("기록 없음"));
        assertTrue(page.contains("class=\"history-table-scroll\""));
        assertTrue(page.contains("tabindex=\"0\""));
        assertTrue(page.contains("class=\"history-scroll-hint\""));
        assertFalse(page.contains("<a class=\"history-item\""));
    }

    @Test
    void historyLicenseUsageUsesCompactTextWithADecorativeProgressRing()
            throws Exception {
        String page = readWebapp("maintenance/maintenance_history.jsp");
        String styles = Files.readString(
                PAGE_STYLES.resolve("maintenance_history.css"));

        assertTrue(page.contains("class=\"history-usage-value\""));
        assertTrue(page.contains(
                "test=\"${not empty row.usagePercentage}\""));
        assertTrue(page.contains(
                "class=\"license-usage-icon\" aria-hidden=\"true\""));
        assertTrue(page.contains("focusable=\"false\""));
        assertTrue(page.contains("pathLength=\"100\""));
        assertTrue(page.contains("license-usage-icon__track"));
        assertTrue(page.contains("license-usage-icon__value"));
        assertTrue(page.contains("${row.usageProgressPercentage}"));
        assertTrue(page.contains("${row.usagePercentage}"));
        assertTrue(page.contains("${row.usedTerabytes}"));
        assertTrue(page.contains("${row.capacityTerabytes}"));
        assertTrue(page.contains("${row.deltaLabel}"));
        assertTrue(page.contains("history-delta--${row.deltaTone}"));

        assertTrue(styles.contains(
                ".maintenance-history .license-usage-icon"));
        assertTrue(styles.contains("inline-size: 14px;"));
        assertTrue(styles.contains("block-size: 14px;"));
        assertTrue(styles.contains("var(--color-chart-usage)"));
    }

    @Test
    void historyComparisonTableIsDenseScopedAndScrollsOnlyInsideItsWrapper()
            throws Exception {
        String styles = Files.readString(
                PAGE_STYLES.resolve("maintenance_history.css"));

        assertTrue(styles.contains(
                ".maintenance-history .history-table-scroll"));
        assertTrue(styles.contains("overflow-x: auto;"));
        assertTrue(styles.contains(
                ".maintenance-history .history-comparison-table"));
        assertTrue(styles.contains("min-width: 760px;"));
        assertTrue(styles.contains(
                ".maintenance-history .history-record-group"));
        assertTrue(styles.contains(
                ".maintenance-history .history-note-cell"));
        assertTrue(styles.contains(
                ".maintenance-history .history-note-layout"));
        assertTrue(styles.contains("white-space: pre-wrap;"));
        assertTrue(styles.contains("font-variant-numeric: tabular-nums;"));
        assertFalse(styles.contains(".maintenance-history .history-item"));
        assertFalse(styles.contains("!important"));
    }

    private static String readWebapp(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }

    private static int countOccurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
