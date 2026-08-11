package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CarbonIconUsageContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void uiIconTokensFollowCarbonArtboardSizes() throws Exception {
        String tokens = read("resources/css/tokens.css");

        assertTrue(tokens.contains("--icon-size-sm: 16px;"));
        assertTrue(tokens.contains("--icon-size-md: 20px;"));
        assertTrue(tokens.contains("--icon-size-lg: 24px;"));
        assertTrue(tokens.contains("--icon-size-xl: 32px;"));
        assertTrue(tokens.contains("--illustration-icon-size-md: 48px;"));
        assertFalse(tokens.contains("--icon-size-xs:"));
        assertFalse(tokens.contains("--icon-size-2xl:"));
        assertFalse(tokens.contains("--icon-size-3xl:"));
        assertFalse(tokens.contains("--icon-size-4xl:"));
        assertFalse(tokens.contains("--icon-size-5xl:"));
        assertFalse(tokens.contains("--icon-size-6xl:"));
    }

    @Test
    void iconOnlyControlsExposeNamesAndFortyFourPixelTargets() throws Exception {
        String tokens = read("resources/css/tokens.css");
        String uiSystem = read("resources/css/ui-system.css");
        String meetings = read("meeting/meeting_list.jsp");
        String tableFooter = read("WEB-INF/tags/tableFooter.tag");

        assertTrue(tokens.contains("--control-height-md: 44px;"));
        assertTrue(uiSystem.contains(".ui-system .ui-touch-target"));
        assertTrue(uiSystem.contains("min-block-size: var(--control-height-md);"));
        assertTrue(uiSystem.contains("min-inline-size: var(--control-height-md);"));
        assertTrue(uiSystem.contains(".ui-system .ui-pagination__link"));
        assertFalse(uiSystem.contains("min-block-size: 40px;"));
        assertFalse(uiSystem.contains("min-inline-size: 40px;"));
        assertTrue(meetings.contains("<t:tableFooter"));
        assertTrue(tableFooter.contains("aria-label=\"${previousLabel}\""));
        assertTrue(tableFooter.contains("aria-label=\"${nextLabel}\""));
        assertTrue(tableFooter.contains("aria-hidden=\"true\">&lsaquo;</span>"));
        assertTrue(tableFooter.contains("aria-hidden=\"true\">&rsaquo;</span>"));
        assertFalse(meetings.contains("fa-angle-double-left"));
        assertFalse(meetings.contains("fa-angle-double-right"));
    }

    @Test
    void pairedIconsUseTheLabelColorAndCenteredAlignment() throws Exception {
        String uiSystem = read("resources/css/ui-system.css");
        String components = read("resources/css/components.css");
        String dashboard = read("resources/css/pages/dashboard.css");
        String myPage = read("mypage/mypage.jsp");
        String maintenanceCards = read("resources/css/pages/maintenance_cards.css");
        String maintenanceHistory = read("resources/css/pages/maintenance_history.css");

        assertTrue(uiSystem.contains(
                ":is(a, button, h1, h2, h3, h4, label) > i[class*=\"fa-\"]"));
        assertTrue(uiSystem.contains("align-self: center;"));
        assertTrue(uiSystem.contains("color: currentColor;"));
        assertTrue(uiSystem.contains(
                ":is(a, button, label) > i[class*=\"fa-\"]"));
        assertTrue(uiSystem.contains("font-size: var(--icon-size-sm);"));
        assertTrue(components.contains(".card-header i"));
        assertFalse(dashboard.contains(".maintenance-month-title > i"));
        assertTrue(dashboard.contains(".maintenance-assignee-customer::before"));
        assertFalse(myPage.contains("vm-board-title\">\n                <i"));
        assertTrue(maintenanceCards.contains(".maintenance-management .customer-name i { color: currentColor;"));
        assertFalse(maintenanceHistory.contains(
                ".maintenance-history .inspection-date i"));
        assertTrue(maintenanceHistory.contains(
                ".maintenance-history .license-usage-icon"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
