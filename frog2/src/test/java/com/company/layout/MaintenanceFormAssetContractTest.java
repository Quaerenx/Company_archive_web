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
        String fields = readWebapp(
                "WEB-INF/includes/maintenance_form_fields.jspf");

        assertTrue(add.contains("/resources/js/pages/maintenance_calendar.js,"));
        assertTrue(edit.contains("/resources/js/pages/maintenance_calendar.js,"));
        assertTrue(add.contains("/resources/js/pages/maintenance_form.js"));
        assertTrue(edit.contains("/resources/js/pages/maintenance_form.js"));
        assertTrue(add.indexOf("maintenance_calendar.js")
                < add.indexOf("maintenance_form.js"));
        assertTrue(edit.indexOf("maintenance_calendar.js")
                < edit.indexOf("maintenance_form.js"));
        assertTrue(add.contains("data-maintenance-form-mode=\"add\""));
        assertTrue(edit.contains("data-maintenance-form-mode=\"edit\""));
        assertEquals(1, countOccurrences(add, "id=\"maintenanceForm\""));
        assertEquals(1, countOccurrences(edit, "id=\"maintenanceForm\""));
        assertTrue(edit.contains("id=\"deleteFormHeader\""));
        assertTrue(edit.contains("id=\"current_customer_value\""));
        assertTrue(edit.contains("id=\"current_inspector_value\""));
        assertTrue(add.contains(
                "WEB-INF/includes/maintenance_form_fields.jspf"));
        assertTrue(edit.contains(
                "WEB-INF/includes/maintenance_form_fields.jspf"));
        assertTrue(fields.startsWith(
                "<%@ page pageEncoding=\"UTF-8\" %>"));
        assertTrue(fields.contains("name=\"customer_name\""));
        assertTrue(fields.contains("id=\"customer_name_display\""));
        assertEquals(1, countOccurrences(fields, "name=\"inspector_name\""));
        assertTrue(fields.contains("id=\"applyPreviousMaintenanceDefaults\""));
        assertTrue(fields.contains("id=\"duplicateMaintenanceWarning\""));
        assertTrue(fields.contains("class=\"ui-input-with-suffix\""));
        assertTrue(fields.contains("id=\"vertica_version_display\""));
        assertTrue(fields.contains("id=\"vertica_version\""));
        assertTrue(fields.contains("id=\"license_size_gb\""));
        assertTrue(fields.contains("type=\"hidden\""));
        assertTrue(fields.contains("id=\"license_size_gb_display\""));
        assertEquals(4, countOccurrences(
                fields, "maintenance-readonly-output"));
        assertTrue(fields.indexOf("id=\"customer_name_display\"")
                < fields.indexOf("id=\"vertica_version_display\""));
        assertTrue(fields.indexOf("id=\"vertica_version_display\"")
                < fields.indexOf("id=\"inspector_name\""));
        assertTrue(fields.contains("고객사 정보에서 자동 적용"));
        assertTrue(fields.contains("id=\"license_usage_pct_display\""));
        assertTrue(fields.contains("id=\"insertMaintenanceNoteTemplate\""));
        assertTrue(fields.contains("id=\"maintenanceInlineCalendar\""));
        assertTrue(fields.contains("tabindex=\"-1\""));
        assertTrue(fields.contains("id=\"maintenanceCalendarGrid\""));
        assertTrue(fields.contains("role=\"grid\""));
        assertTrue(fields.contains("id=\"maintenanceCalendarPreviousMonth\""));
        assertTrue(fields.contains("id=\"maintenanceCalendarNextMonth\""));
        assertTrue(fields.contains("id=\"maintenanceCalendarToday\""));
        assertTrue(fields.contains("type=\"date\""));
    }

    @Test
    void splitScriptsPreserveModeSpecificBehaviorAndCalendarContract() throws Exception {
        String script = Files.readString(PAGE_SCRIPTS.resolve("maintenance_form.js"));
        String calendar = Files.readString(
                PAGE_SCRIPTS.resolve("maintenance_calendar.js"));

        assertTrue(script.contains("(function()"));
        assertTrue(script.contains("getElementById('maintenanceForm')"));
        assertFalse(script.contains("querySelector('form')"));
        assertFalse(script.contains(
                "/customers?action=getCustomersForMaintenance"));
        assertTrue(script.contains("view: 'formContext'"));
        assertTrue(script.contains("if (!response.ok)"));
        assertFalse(script.contains("toISOString()"));
        assertTrue(script.contains("calculateLicensePercentage"));
        assertTrue(script.contains("toFixed(1)"));
        assertTrue(script.contains("maxLicensePercentage = 1000000"));
        assertFalse(script.contains("used > capacity"));
        assertFalse(script.contains("사용량은 전체 용량보다 클 수 없습니다."));
        assertTrue(script.contains("defaultLicenseSize"));
        assertTrue(script.contains("setFixedVersion"));
        assertTrue(script.contains("renderFixedVersion"));
        assertTrue(script.contains("versionDisplay"));
        assertFalse(script.contains("previousButton.dataset.version"));
        assertFalse(script.contains("setFieldValue(versionField"));
        assertTrue(script.contains("setFixedCapacity"));
        assertTrue(script.contains("renderFixedCapacity"));
        assertTrue(script.contains("licenseSizeDisplay"));
        assertTrue(script.contains("applyPreviousDefaults"));
        assertTrue(script.contains("duplicateMaintenanceWarning"));
        assertTrue(script.contains("beforeunload"));
        assertTrue(script.contains("insertNoteTemplate"));
        assertTrue(script.contains("autoResizeNote"));
        assertTrue(script.contains("정말 삭제하시겠습니까?"));
        assertTrue(script.contains("Frog2MaintenanceCalendar.create"));
        assertTrue(script.contains("calendarController.initialize()"));
        assertTrue(calendar.contains("dateField.type = 'hidden'"));
        assertTrue(calendar.contains(
                "root.classList.add('is-calendar-enhanced')"));
        assertTrue(script.contains("calendarController.focusForValidation()"));
        assertTrue(calendar.contains("function parseDate("));
        assertTrue(calendar.contains("function render("));
        assertTrue(calendar.contains("function selectDate("));
        assertTrue(calendar.contains("case 'ArrowLeft':"));
        assertTrue(calendar.contains("case 'ArrowRight':"));
        assertTrue(calendar.contains("case 'ArrowUp':"));
        assertTrue(calendar.contains("case 'ArrowDown':"));
        assertTrue(calendar.contains("aria-selected"));
    }

    @Test
    void obsoletePageScriptsAreRemoved() {
        assertFalse(Files.exists(PAGE_SCRIPTS.resolve("maintenance_add.js")));
        assertFalse(Files.exists(PAGE_SCRIPTS.resolve("maintenance_edit.js")));
    }

    @Test
    void historyScriptSkipsChartRenderingWhenVendorIsUnavailable() throws Exception {
        String history = Files.readString(PAGE_SCRIPTS.resolve("maintenance_history.js"));

        assertFalse(history.contains("initializeHistoryDisclosures"));
        assertFalse(history.contains("data-history-toggle"));
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
        assertTrue(history.contains("matchMedia('(max-width: 768px)')"));
        assertTrue(history.contains("maxTicksLimit: compactChart ? 5 : 8"));
        assertTrue(history.contains("maxRotation: compactChart ? 0 : 35"));
        assertFalse(history.contains(".history-item"));
        assertFalse(history.contains("item.animate"));
        assertFalse(history.contains("item.addEventListener('click'"));
        assertTrue(readWebapp("maintenance/maintenance_history.jsp")
                .contains("<table class=\"history-comparison-table ui-table ui-data-table\""));
    }

    @Test
    void historyRecordsUseCompactRowsWithIndependentAccessibleDetails()
            throws Exception {
        String page = readWebapp("maintenance/maintenance_history.jsp");
        int tableStart = page.indexOf(
                "<table class=\"history-comparison-table ui-table ui-data-table\"");
        int tableEnd = page.indexOf("</table>", tableStart);
        assertTrue(tableStart >= 0);
        assertTrue(tableEnd > tableStart);
        String comparisonTable = page.substring(tableStart, tableEnd);

        assertTrue(comparisonTable.contains(
                "<caption class=\"sr-only\">"));
        assertEquals(6, countOccurrences(
                comparisonTable, "<th scope=\"col\""));
        assertTrue(comparisonTable.contains("점검일"));
        assertTrue(comparisonTable.contains("이전 대비"));
        assertTrue(comparisonTable.contains(
                "<c:forEach var=\"row\" items=\"${historyRows}\">"));
        assertTrue(comparisonTable.contains(
                "<tbody class=\"history-record-group\">"));
        assertTrue(comparisonTable.contains(
                "<tr class=\"history-summary-row\" data-ui-disclosure-row>"));
        assertTrue(comparisonTable.contains(
                "scope=\"row\""));
        assertTrue(comparisonTable.contains(
                "pattern=\"yyyy.MM.dd\""));
        assertTrue(comparisonTable.contains(
                "<c:out value=\"${row.noteSummary}\" />"));
        assertTrue(comparisonTable.contains(
                "data-ui-disclosure-toggle"));
        assertTrue(comparisonTable.contains(
                "data-ui-disclosure-content"));
        assertTrue(comparisonTable.contains(
                "class=\"history-detail-motion\""));
        assertTrue(comparisonTable.contains(
                "class=\"history-detail-content\""));
        assertTrue(comparisonTable.contains("type=\"button\""));
        assertTrue(comparisonTable.contains("aria-expanded=\"false\""));
        assertTrue(comparisonTable.contains(
                "aria-controls=\"${row.detailId}\""));
        assertTrue(comparisonTable.contains(
                "<tr id=\"${row.detailId}\" class=\"history-detail-row\" hidden>"));
        assertTrue(comparisonTable.contains(
                "<td class=\"history-detail-cell\" colspan=\"6\">"));
        assertFalse(comparisonTable.contains("data-history-toggle"));
        assertFalse(comparisonTable.contains("fa-chevron-down"));
        assertFalse(comparisonTable.contains("history-detail-toggle-cell"));
        assertTrue(comparisonTable.contains(
                "class=\"history-detail-metrics\""));
        assertTrue(comparisonTable.contains(
                "class=\"history-detail-note\""));
        assertTrue(comparisonTable.contains(
                "<div class=\"history-detail-note\"><c:choose>"));
        assertTrue(comparisonTable.contains(
                "</c:choose></div>"));
        assertTrue(comparisonTable.contains(
                "<c:out value=\"${row.record.note}\" />"));
        assertTrue(comparisonTable.contains("특이사항 없음"));
        assertTrue(comparisonTable.contains("수정 일시"));
        assertFalse(comparisonTable.contains("rowspan=\"2\""));
        assertFalse(comparisonTable.contains("history-note-row"));
        assertTrue(page.contains(
                "class=\"history-table-scroll ui-table-wrap\""));
        assertTrue(page.contains("data-ui-scroll-region"));
        assertTrue(page.contains(
                "data-ui-scroll-hint-id=\"historyScrollHint\""));
        assertFalse(page.contains("tabindex=\"0\""));
        assertTrue(page.contains("class=\"history-scroll-hint\""));
        assertFalse(page.contains("<a class=\"history-item\""));
    }

    @Test
    void expandedHistoryUsesFullWidthSummaryAndReadableNoteMetadataSplit()
            throws Exception {
        String page = readWebapp("maintenance/maintenance_history.jsp");

        int detailStart = page.indexOf(
                "<div class=\"history-detail-content\"");
        int detailEnd = page.indexOf("</td>", detailStart);
        assertTrue(detailStart >= 0);
        assertTrue(detailEnd > detailStart);
        String detail = page.substring(detailStart, detailEnd);

        int summary = detail.indexOf(
                "history-detail-summary");
        int lower = detail.indexOf(
                "class=\"history-detail-lower\"");
        int note = detail.indexOf(
                "history-detail-note-section");
        int metadata = detail.indexOf(
                "history-detail-meta-section");
        assertTrue(summary >= 0);
        assertTrue(lower > summary);
        assertTrue(note > lower);
        assertTrue(metadata > note);
        assertTrue(detail.contains(
                "class=\"history-detail-metrics\""));
        assertTrue(detail.contains("class=\"history-detail-meta\""));
    }

    @Test
    void expandedHistoryLayoutKeepsMetricsDenseAndMetadataAligned()
            throws Exception {
        String styles = Files.readString(
                PAGE_STYLES.resolve("maintenance_history.css"));

        assertTrue(styles.contains(
                "grid-template-columns: repeat(4, minmax(0, 1fr));"));
        assertTrue(styles.contains(
                "grid-template-columns: minmax(0, 7fr) minmax(220px, 3fr);"));
        assertTrue(styles.contains(
                ".maintenance-history .history-detail-meta > div"));
        assertTrue(styles.contains(
                "grid-template-columns: 72px minmax(0, 1fr);"));
        assertTrue(styles.contains("white-space: nowrap;"));
        assertTrue(styles.contains(
                ".maintenance-history .history-detail-lower"));
    }

    @Test
    void historyLicenseUsageCombinesCapacityProgressAndAccessibleText()
            throws Exception {
        String page = readWebapp("maintenance/maintenance_history.jsp");
        String styles = Files.readString(
                PAGE_STYLES.resolve("maintenance_history.css"));

        assertTrue(page.contains("class=\"history-license-cell\""));
        assertTrue(page.contains("class=\"history-license-values\""));
        assertTrue(page.contains(
                "test=\"${not empty row.usagePercentage}\""));
        assertTrue(page.contains(
                "class=\"history-license-progress history-license-progress--${row.usageTone}\""));
        assertTrue(page.contains("max=\"100\""));
        assertTrue(page.contains("value=\"${row.usageProgressPercentage}\""));
        assertTrue(page.contains(
                "aria-label=\"라이선스 사용률 ${row.usagePercentage}%, ${row.usageStatusLabel}\""));
        assertTrue(page.contains(
                "history-license-percent--${row.usageTone}"));
        assertTrue(page.contains("${row.usageStatusLabel}"));
        assertTrue(page.contains("${row.usageProgressPercentage}"));
        assertTrue(page.contains("${row.usagePercentage}"));
        assertTrue(page.contains("${row.usedTerabytes}"));
        assertTrue(page.contains("${row.capacityTerabytes}"));
        assertTrue(page.contains("${row.deltaLabel}"));
        assertTrue(page.contains("class=\"history-delta\""));
        assertFalse(page.contains("history-delta--${row.deltaTone}"));

        assertTrue(styles.contains(
                ".maintenance-history .history-license-progress"));
        assertTrue(styles.contains("progress::-webkit-progress-value"));
        assertTrue(styles.contains("progress::-moz-progress-bar"));
        assertTrue(styles.contains(
                ".history-license-progress--warning"));
        assertTrue(styles.contains(
                ".history-license-progress--risk"));
        assertTrue(styles.contains(
                ".history-license-percent--normal"));
        assertTrue(styles.contains(
                ".history-license-percent--warning"));
        assertTrue(styles.contains(
                ".history-license-percent--risk"));
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
        assertTrue(styles.contains("min-width: 880px;"));
        assertTrue(styles.contains(
                ".maintenance-history .history-record-group"));
        assertTrue(styles.contains(
                ".maintenance-history .history-summary-row"));
        assertTrue(styles.contains(
                "block-size: 60px;"));
        assertTrue(styles.contains("cursor: pointer;"));
        assertTrue(styles.contains(
                ".maintenance-history .history-row-toggle"));
        assertTrue(styles.contains(
                ".maintenance-history .history-license-cell"));
        assertTrue(styles.contains("text-align: center;"));
        assertTrue(styles.contains("text-align: left;"));
        assertFalse(styles.contains(".history-detail-toggle__icon"));
        assertTrue(styles.contains(
                ".maintenance-history .history-detail-cell"));
        assertTrue(styles.contains(
                ".maintenance-history .history-detail-note"));
        assertTrue(styles.contains("white-space: pre-wrap;"));
        assertTrue(styles.contains(
                ".maintenance-history .history-detail-note-section"));
        assertTrue(styles.contains("align-items: stretch;"));
        assertTrue(styles.contains("flex: 1 1 auto;"));
        assertTrue(styles.contains("min-block-size: 112px;"));
        assertTrue(styles.contains(
                "padding: var(--space-12) var(--space-16);"));
        assertFalse(styles.contains("min-block-size: 72px;"));
        assertTrue(styles.contains("font-variant-numeric: tabular-nums;"));
        assertTrue(styles.contains("@media (max-width: 1024px)"));
        assertTrue(styles.contains(".history-col-note"));
        assertTrue(styles.contains("@media (max-width: 768px)"));
        assertTrue(styles.contains(".history-col-inspector"));
        assertFalse(styles.contains(".maintenance-history .history-item"));
        assertFalse(styles.contains("!important"));
    }

    @Test
    void mobileHistoryChartKeepsAReadableWidthInsideItsOwnScrollRegion()
            throws Exception {
        String styles = Files.readString(
                PAGE_STYLES.resolve("maintenance_history.css"));

        assertTrue(styles.contains(
                ".maintenance-history .usage-chart-scroll"));
        assertTrue(styles.contains("overscroll-behavior-inline: contain;"));
        assertTrue(styles.contains(
                ".maintenance-history .usage-chart-canvas"));
        assertTrue(styles.contains("min-width: 720px;"));
        assertTrue(styles.contains("@media (max-width: 480px)"));
        assertTrue(styles.contains("flex-direction: column;"));
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
