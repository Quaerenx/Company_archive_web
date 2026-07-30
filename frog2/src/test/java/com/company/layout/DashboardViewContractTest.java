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
    void dashboardKeepsMaintenanceVmHostAndVisualContract() throws Exception {
        String page = read("dashboard.jsp");
        assertTrue(page.contains("/resources/css/pages/dashboard.css"));
        assertKpiLink(page, "done", "monthlyMaintenanceDoneCount");
        assertKpiLink(page, "due", "monthlyMaintenanceDueCount");
        assertKpiLink(page, "attention", "monthlyMaintenanceAttentionCount");
        assertKpiLink(page, "license-risk", "monthlyMaintenanceLicenseRiskCount");
        int kpiStart = page.indexOf("monthlyMaintenanceDoneCount");
        int vmBoardStart = page.indexOf("class=\"card dashboard-card vm-board");
        assertTrue(kpiStart >= 0 && vmBoardStart > kpiStart,
                "The maintenance KPIs must precede the auxiliary VM board");
        for (String metric : new String[] {
                "monthlyMaintenanceDoneCount",
                "monthlyMaintenanceDueCount",
                "monthlyMaintenanceAttentionCount",
                "monthlyMaintenanceLicenseRiskCount"
        }) {
            assertTrue(page.indexOf(metric) < vmBoardStart,
                    () -> metric + " must precede the auxiliary VM board");
        }

        assertTrue(page.contains("id=\"maintenanceMonthBoard\""));
        assertTrue(page.contains("id=\"maintenanceMonthBoardBody\""));
        assertTrue(page.contains("id=\"toggleMaintenanceBoardBtn\""));
        assertTrue(tagById(page, "toggleMaintenanceBoardBtn")
                .contains("aria-controls=\"maintenanceMonthBoardBody\""));
        assertTrue(tagById(page, "toggleMaintenanceBoardBtn")
                .contains("aria-expanded=\"true\""));
        assertTrue(page.contains("maintenanceMonthTabs"));
        assertTrue(page.contains("monthlyMaintenanceCards"));
        assertTrue(Pattern.compile(
                "<article\\b[^>]*class=\"[^\"]*maintenance-record-card[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(page).find());
        assertFalse(Pattern.compile(
                "<a\\b[^>]*class=\"[^\"]*maintenance-record-card[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(page).find(),
                "A maintenance card must not be one large link");
        assertTrue(Pattern.compile(
                "<a\\b[^>]*class=\"[^\"]*maintenance-detail-link[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(page).find());
        assertTrue(page.contains("<c:out value=\"${record.customerName}\" />"));
        assertTrue(page.contains("aria-current=\"page\""));

        assertTrue(page.contains("id=\"vmHostBoardBody\""));
        assertTrue(page.contains("id=\"toggleVmHostBoardBtn\""));
        assertTrue(tagById(page, "toggleVmHostBoardBtn")
                .contains("aria-controls=\"vmHostBoardBody\""));
        assertTrue(tagById(page, "toggleVmHostBoardBtn")
                .contains("aria-expanded=\"true\""));
        assertTrue(page.contains("id=\"openVmHostAddBtn\""));
        assertTrue(page.contains("id=\"vmHostModalBackdrop\""));
        assertTrue(page.contains("id=\"vmHostSaveForm\""));
        assertTrue(page.contains("id=\"vmHostDeleteForm\""));
        assertTrue(page.contains("data-original-ip=\"<c:out value='${vmHostOriginalIp}'/>\""));
        assertFalse(page.contains("data-original-ip=\"<c:out value='${vmHostForm.ip}'/>\""));
        assertTrue(Pattern.compile(
                "<button\\b[^>]*class=\"[^\"]*vm-edit-btn[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(page).find());
        assertTrue(page.contains("<c:out value=\"${host.ip}\" />"));
        assertFalse(page.contains("vm-row-clickable"));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(page.contains("name=\"action\" value=\"save\""));
        assertTrue(page.contains("name=\"action\" value=\"delete\""));
        for (String field : new String[] {
                "ip", "purpose", "osInfo", "verticaVersion", "remoteHost", "note"
        }) {
            assertTrue(page.contains("name=\"" + field + "\""), field);
        }

        String behavior = page.contains("/resources/js/pages/dashboard.js")
                ? read("resources/js/pages/dashboard.js")
                : page;
        assertTrue(behavior.contains("frog2.dashboard.personal-hosts.collapsed"));
        assertTrue(behavior.contains("frog2.dashboard.monthly-maintenance.collapsed"));
        assertTrue(behavior.contains("populateModal"));
        assertTrue(behavior.contains("querySelectorAll('.vm-edit-btn')"));
        assertTrue(behavior.contains(".dataset.ip"));
        assertTrue(behavior.contains("setAttribute('aria-expanded'"));
        assertTrue(behavior.contains("event.key === 'Tab'"));
        assertTrue(behavior.contains("document.activeElement"));
        assertTrue(behavior.contains("focusable"));
        assertTrue(behavior.contains("previouslyFocusedElement"));
        assertTrue(behavior.contains(".focus()"));
        assertTrue(behavior.contains("event.key === 'Escape'"));
        assertTrue(behavior.contains("해당 호스트를 삭제하시겠습니까?"));
        assertTrue(behavior.contains("event.button !== 0"));
        assertTrue(behavior.contains("maintenanceBody.classList.add('is-loading')"));
        assertTrue(behavior.contains("link.setAttribute('aria-current', 'true')"));

        String styles = page.contains("/resources/js/pages/dashboard.js")
                ? read("resources/css/pages/dashboard.css")
                : page;
        assertTrue(styles.contains("grid-template-columns: repeat(2, minmax(0, 1fr))"));
        assertTrue(styles.contains("width: min(720px, 100%)"));
        assertTrue(styles.contains("z-index: 2000"));
        assertTrue(styles.contains("@media (max-width: 840px)"));
    }

    private static void assertKpiLink(
            String page, String status, String valueExpression) {
        Pattern linkedValue = Pattern.compile(
                "<a\\b(?=[^>]*class=\"[^\"]*maintenance-kpi-link[^\"]*\")"
                        + "(?=[^>]*data-status=\"" + Pattern.quote(status) + "\")"
                        + "[^>]*>.*?\\$\\{"
                        + Pattern.quote(valueExpression)
                        + "\\}.*?</a>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        assertTrue(linkedValue.matcher(page).find(),
                () -> valueExpression + " must be rendered inside a real link");
    }

    private static String tagById(String source, String id) {
        var matcher = Pattern.compile(
                "<[^>]+\\bid=\"" + Pattern.quote(id) + "\"[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(source);
        assertTrue(matcher.find(), () -> "Element is missing: #" + id);
        return matcher.group();
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
