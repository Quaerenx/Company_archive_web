package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DashboardViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void monthCarouselUsesServerLinksAndKeepsTheSelectionSlotFixed() throws Exception {
        String page = read("dashboard.jsp");
        assertTrue(page.contains("items=\"${maintenanceMonthTabs}\""));
        assertTrue(page.contains("class=\"maintenance-month-track\""));
        assertTrue(page.contains("<c:param name=\"maintenanceMonth\" value=\"${monthTab.value}\" />"));
        assertTrue(page.contains("dashboardMaintenanceMonthHelp"));
        assertTrue(page.contains("aria-current=\"page\""));
        String styles = read("resources/css/pages/dashboard.css");
        String slot = cssRule(styles,
                ".dashboard-page .maintenance-month-tabs.is-carousel::before");
        assertTrue(slot.contains("inset-inline-start: 50%;"));
        assertTrue(slot.contains("background: var(--color-surface-selected);"));
        assertTrue(slot.contains("pointer-events: none;"));
        assertTrue(styles.contains("min-block-size: 44px;"));
        assertTrue(cssRule(styles, ".dashboard-page .maintenance-month-tabs")
                .contains("grid-row: 2;"));
        assertTrue(cssRule(styles, ".dashboard-page .maintenance-month-selector-label {")
                .contains("grid-column: 1;"));
        assertTrue(cssRule(styles, ".dashboard-page .maintenance-month-tab {")
                .contains("font-size: var(--font-size-xs);"));
        assertTrue(cssRule(styles, ".dashboard-page .maintenance-month-tab.active")
                .contains("font-size: var(--font-size-sm);"));
    }

    @Test
    void dashboardKeepsMaintenanceContractWithoutQuickActionsOrPersonalHosts() throws Exception {
        String page = read("dashboard.jsp");
        String customerLinks = read("WEB-INF/tags/monthlyMaintenanceCustomerList.tag");
        assertTrue(page.contains("/resources/css/pages/dashboard.css"));
        assertFalse(page.contains("maintenance-kpi-section"));
        assertFalse(page.contains("월간 정기점검 요약"));
        assertFalse(page.contains("monthlyMaintenanceDoneCount"));
        assertFalse(page.contains("dashboard-quick-actions"));
        assertFalse(page.contains("dashboardMenus"));
        assertFalse(page.contains("vmHostBoardBody"));
        assertFalse(page.contains("vmHostModalBackdrop"));
        assertFalse(page.contains("returnTo\" value=\"dashboard"));
        assertFalse(page.contains("dashboard-support"));
        assertFalse(page.contains("외부 참고 링크"));

        assertTrue(page.contains("id=\"maintenanceMonthBoard\""));
        assertTrue(page.contains("id=\"maintenanceMonthBoardBody\""));
        assertTrue(page.contains("id=\"toggleMaintenanceBoardBtn\""));
        assertTrue(tagById(page, "toggleMaintenanceBoardBtn")
                .contains("aria-controls=\"maintenanceMonthBoardBody\""));
        assertTrue(tagById(page, "toggleMaintenanceBoardBtn")
                .contains("aria-expanded=\"true\""));
        assertTrue(page.contains("maintenanceMonthTabs"));
        assertTrue(page.contains("monthlyMaintenanceAssigneeGroups"));
        assertTrue(Pattern.compile(
                "<li\\b[^>]*class=\"[^\"]*maintenance-assignee-customer[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(customerLinks).find());
        assertFalse(Pattern.compile(
                "<article\\b[^>]*class=\"[^\"]*maintenance-record-card[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(page).find(),
                "The typography maintenance view must not restore record cards");
        assertTrue(page.contains("class=\"maintenance-assignee-grid\""));
        assertTrue(page.contains("<c:out value=\"${group.managerName}\" />"));
        assertFalse(page.contains("maintenance-assignee-count"));
        assertFalse(page.contains("fn:length(group.customers)"));
        assertTrue(customerLinks.contains("<c:out value=\"${customer.customerName}\" />"));
        assertTrue(customerLinks.contains("data-maintenance-status=\"${customer.statusCode}\""));
        assertFalse(page.contains("data-license-risk="));
        assertTrue(customerLinks.contains("href=\"${customerHistoryUrl}\""));
        assertTrue(page.contains("aria-current=\"page\""));

        String behavior = page.contains("/resources/js/pages/dashboard.js")
                ? read("resources/js/pages/dashboard.js")
                : page;
        assertTrue(behavior.contains("frog2.dashboard.monthly-maintenance.collapsed"));
        assertTrue(behavior.contains("setAttribute('aria-expanded'"));
        assertTrue(behavior.contains("event.button !== 0"));
        assertTrue(behavior.contains("maintenanceBody.classList.add('is-loading')"));
        assertFalse(behavior.contains("maintenanceItems"));
        assertFalse(behavior.contains("maintenanceGroups"));
        assertFalse(behavior.contains("maintenance-kpi-link"));

        String styles = page.contains("/resources/js/pages/dashboard.js")
                ? read("resources/css/pages/dashboard.css")
                : page;
        assertTrue(styles.contains(".maintenance-assignee-grid"));
        String assigneeGrid = cssRule(
                styles,
                ".dashboard-page .maintenance-assignee-grid");
        assertTrue(assigneeGrid.contains("display: grid;"));
        assertTrue(assigneeGrid.contains("grid-auto-rows: 1fr;"));
        assertFalse(assigneeGrid.contains("align-items: start;"));
        assertTrue(assigneeGrid.contains(
                "grid-template-columns: repeat(2, minmax(0, 1fr));"));
        String assigneeGroup = cssRule(
                styles,
                ".dashboard-page .maintenance-assignee-group");
        assertTrue(page.contains("maintenance-assignee-group ui-subgroup"));
        String sharedStyles = read("resources/css/ui-system.css");
        String subgroup = cssRule(sharedStyles, ".ui-system .ui-subgroup");
        assertTrue(subgroup.contains(
                "background: var(--color-surface-subtle);"));
        assertTrue(subgroup.contains(
                "border-radius: var(--radius-md);"));
        assertFalse(assigneeGroup.contains("align-self: start;"));
        assertTrue(assigneeGroup.contains("block-size: 100%;"));
        assertTrue(assigneeGroup.contains(
                "grid-template-rows: max-content max-content;"));
        assertFalse(assigneeGroup.contains("border-block-end:"));
        String assigneeCustomers = cssRule(
                styles,
                ".dashboard-page .maintenance-assignee-customers");
        assertTrue(assigneeCustomers.contains("align-content: start;"));
        assertTrue(assigneeCustomers.contains("grid-auto-rows: max-content;"));
        assertTrue(styles.contains(".maintenance-assignee-customer::before"));
        assertTrue(styles.contains("background: var(--color-success);"));
        assertTrue(styles.contains(".maintenance-assignee-customer--due::before"));
        String completedPoint = cssRule(
                styles,
                ".dashboard-page .maintenance-assignee-customer::before");
        String pendingPoint = cssRule(
                styles,
                ".dashboard-page .maintenance-assignee-customer--due::before");
        assertTrue(pendingPoint.contains("background: var(--color-text-muted);"));
        assertFalse(completedPoint.contains("border:"));
        assertFalse(pendingPoint.contains("border:"));
        assertTrue(completedPoint.contains("block-size: 6px;"));
        assertTrue(styles.contains(".maintenance-assignee-customer:hover"));
        assertTrue(styles.contains("background: var(--color-surface-hover);"));
        assertTrue(styles.contains("@media (max-width: 768px), (pointer: coarse)"));
        assertTrue(styles.matches(
                "(?s).*@media \\(max-width: 768px\\), \\(pointer: coarse\\).*"
                        + "\\.dashboard-page \\.maintenance-assignee-customer a\\s*\\{[^}]*"
                        + "min-block-size:\\s*44px;.*"));
        String frequency = cssRule(
                styles,
                ".dashboard-page .maintenance-assignee-frequency");
        assertTrue(frequency.contains("margin-inline-start: auto;"));
        assertTrue(frequency.contains("background: transparent;"));
        assertTrue(frequency.contains("border: 0;"));
        assertTrue(frequency.contains("white-space: nowrap;"));
        String monthHeader = cssRule(
                sharedStyles,
                ".ui-system .ui-section-header");
        assertTrue(monthHeader.contains(
                "border-block-end: 1px solid var(--color-divider);"));
        String monthBoard = cssRule(
                sharedStyles,
                ".ui-system .ui-work-surface");
        assertTrue(monthBoard.contains("background: var(--color-surface);"));
        assertTrue(monthBoard.contains(
                "border: 1px solid var(--color-surface-edge);"));
        assertTrue(monthBoard.contains("box-shadow: var(--shadow-sm);"));
        assertTrue(sharedStyles.contains(
                ".ui-system :where(.ui-work-surface, .table-container)"));
        assertTrue(page.contains("maintenance-status-legend"));
        assertTrue(page.contains("<ul class=\"maintenance-status-legend\""));
        assertTrue(page.contains("aria-label=\"정기점검 상태 범례\""));
        assertTrue(page.contains(
                "<li class=\"maintenance-status-legend__item maintenance-status-legend__item--done\""));
        assertTrue(page.contains(
                "<li class=\"maintenance-status-legend__item maintenance-status-legend__item--due\""));
        assertTrue(page.contains(
                "<li class=\"maintenance-status-legend__frequency\">분기</li>"));
        String statusLegend = cssRule(
                styles,
                ".dashboard-page .maintenance-status-legend");
        assertTrue(statusLegend.contains("list-style: none;"));
        assertTrue(statusLegend.contains("padding: 0;"));
        String legendFrequency = cssRule(
                styles,
                ".dashboard-page .maintenance-status-legend__frequency");
        assertTrue(legendFrequency.contains("background: transparent;"));
        assertTrue(legendFrequency.contains("border: 0;"));
        assertTrue(page.contains(
                "ui-work-surface ui-work-surface--padded"));
        assertTrue(sharedStyles.contains(
                ".ui-work-surface--padded"));
        assertFalse(styles.contains(".dashboard-support"));
        assertTrue(styles.contains("container-type: inline-size"));
        assertTrue(styles.contains("@container dashboard-maintenance"));
        assertTrue(styles.contains("@media (max-width: 768px)"));
    }

    @Test
    void personalSummaryCountsRegistrationsAndLinksToTheSameSelectedMonth()
            throws Exception {
        String page = read("dashboard.jsp");
        int personal = page.indexOf("data-personal-section=\"dashboard-maintenance\"");
        int global = page.indexOf("data-global-section=\"dashboard-maintenance\"");
        String personalSection = page.substring(personal, global);
        assertTrue(personalSection.contains("maintenanceDataLoaded and personalMaintenanceAssignedCount gt 0"));
        assertTrue(personalSection.contains("personal-maintenance-summary__counts"));
        for (String count : new String[] {"Target", "Registered", "Unregistered"}) {
            assertTrue(personalSection.contains(
                    "<c:out value=\"${personalMaintenance" + count + "Count}\" />"));
        }
        assertTrue(personalSection.contains("<dt>이력 등록</dt>"));
        assertTrue(personalSection.contains("<dt>이력 미등록</dt>"));
        assertTrue(personalSection.contains("<c:param name=\"maintenanceMonth\" value=\"${maintenanceMonthParam}\" />"));
        assertTrue(personalSection.contains("href=\"<c:out value='${personalMaintenanceManageUrl}' />\""));
        int manageUrlStart = personalSection.indexOf("<c:url var=\"personalMaintenanceManageUrl\"");
        String manageUrl = personalSection.substring(
                manageUrlStart, personalSection.indexOf("</c:url>", manageUrlStart));
        assertFalse(manageUrl.contains("<c:param name=\"registrationStatus\""));
        int unregisteredUrlStart = personalSection.indexOf(
                "<c:url var=\"personalMaintenanceUnregisteredUrl\"");
        String unregisteredUrl = personalSection.substring(
                unregisteredUrlStart, personalSection.indexOf("</c:url>", unregisteredUrlStart));
        assertTrue(unregisteredUrl.contains(
                "<c:param name=\"registrationStatus\" value=\"unregistered\" />"));
    }

    private static String tagById(String source, String id) {
        var matcher = Pattern.compile(
                "<[^>]+\\bid=\"" + Pattern.quote(id) + "\"[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(source);
        assertTrue(matcher.find(), () -> "Element is missing: #" + id);
        return matcher.group();
    }

    private static String cssRule(String source, String selector) {
        int selectorStart = source.indexOf(selector);
        assertTrue(selectorStart >= 0, () -> "Missing selector: " + selector);
        int ruleStart = source.indexOf('{', selectorStart);
        int ruleEnd = source.indexOf('}', ruleStart);
        assertTrue(ruleStart >= 0 && ruleEnd > ruleStart, selector);
        return source.substring(ruleStart + 1, ruleEnd);
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
