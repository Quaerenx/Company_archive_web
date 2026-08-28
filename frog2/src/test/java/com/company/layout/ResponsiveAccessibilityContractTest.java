package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponsiveAccessibilityContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void viewportHeightContractsPreferDynamicViewportUnits() throws Exception {
        String base = read("resources/css/base.css");
        String ui = read("resources/css/ui-system.css");
        String header = read("resources/css/pages/header.css");
        String myPage = read("resources/css/pages/mypage_hosts.css");

        assertTrue(base.indexOf("min-height: 100vh;")
                < base.indexOf("min-height: 100dvh;"));
        assertTrue(ui.indexOf("max-block-size: calc(100vh - var(--space-32));")
                < ui.indexOf("max-block-size: calc(100dvh - var(--space-32));"));
        assertTrue(header.contains("max-height: calc(100dvh - 88px);"));
        assertTrue(myPage.contains("max-height: calc(100dvh - 48px);"));
        assertTrue(myPage.contains("max-height: calc(100dvh - 24px);"));
    }

    @Test
    void standaloneErrorPagesDeclareMobileViewport() throws Exception {
        for (String pagePath : List.of("error/404.jsp", "error/500.jsp")) {
            String page = read(pagePath);
            assertTrue(page.contains(
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\""),
                    pagePath);
        }
    }

    @Test
    void overflowingDataTablesBecomeNamedKeyboardRegionsOnlyWhenNeeded()
            throws Exception {
        String uiCss = read("resources/css/ui-system.css");
        String uiScript = read("resources/js/ui-system.js");

        assertTrue(uiCss.contains(".ui-system .ui-table-wrap {"));
        assertTrue(uiCss.contains("overflow-x: auto;"));
        assertTrue(uiCss.contains(
                ".ui-system .ui-table-wrap[tabindex=\"0\"]:focus-visible"));
        assertTrue(uiCss.contains("scrollbar-color: var(--color-border-strong)"));

        assertTrue(uiScript.contains("function updateScrollableTableRegions()"));
        assertTrue(uiScript.contains("region.scrollWidth > region.clientWidth + 1"));
        assertTrue(uiScript.contains(
                ".ui-work-surface, [data-ui-table-surface]"));
        assertTrue(uiScript.contains("region.setAttribute('tabindex', '0')"));
        assertTrue(uiScript.contains("region.setAttribute('role', 'region')"));
        assertTrue(uiScript.contains("region.removeAttribute('tabindex')"));
        assertTrue(uiScript.contains("region.dataset.uiScrollLabel"));

        for (String pagePath : List.of(
                "customers/customers_list.jsp",
                "meeting/meeting_list.jsp",
                "troubleshooting/troubleshooting_list.jsp",
                "mypage/monthly_customer_response.jsp",
                "WEB-INF/views/filerepo/list.jsp",
                "WEB-INF/includes/mypage/host_manager.jspf",
                "vm_hosts/list.jsp")) {
            String page = read(pagePath);
            assertTrue(page.contains("data-ui-scroll-region"), pagePath);
            assertTrue(page.contains("data-ui-scroll-label="), pagePath);
        }
    }

    @Test
    void sharedBreakpointsCoverRequiredCompactWidths() throws Exception {
        String base = read("resources/css/base.css");
        String ui = read("resources/css/ui-system.css");
        String header = read("resources/css/pages/header.css");

        assertTrue(base.contains("@media (max-width: 480px)"));
        assertTrue(base.contains("@media (max-width: 768px)"));
        assertTrue(base.contains("@media (max-width: 1024px)"));
        assertTrue(ui.contains("@media (max-width: 480px)"));
        assertTrue(ui.contains("@media (max-width: 768px)"));
        assertTrue(header.contains("@media (max-width: 768px)"));
    }

    @Test
    void compactFilterControlsRemainTouchSizedOnCoarseAndMobileViewports()
            throws Exception {
        String ui = read("resources/css/ui-system.css");
        int touchRulesStart = ui.indexOf(
                "@media (pointer: coarse), (max-width: 768px)");
        int touchRulesEnd = ui.indexOf(
                "@media (max-width: 768px)", touchRulesStart);

        assertTrue(touchRulesStart > 0);
        assertTrue(touchRulesEnd > touchRulesStart);
        String touchRules = ui.substring(touchRulesStart, touchRulesEnd);
        assertTrue(touchRules.contains("form.ui-form--compact :is("));
        assertTrue(touchRules.contains(
                "min-block-size: var(--control-height-md);"));
    }

    @Test
    void skipTargetDoesNotExposeAViewportSizedBrowserOutline() throws Exception {
        String header = read("includes/header.jsp");
        String base = read("resources/css/base.css");

        assertTrue(header.contains(
                "<main id=\"main-content\" class=\"app-main\" tabindex=\"-1\">"));
        assertTrue(base.contains(".ui-system .app-main:focus"));
        assertTrue(base.contains("outline: none;"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
