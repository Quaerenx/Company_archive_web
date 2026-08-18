package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReadabilityRefinementContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void narrowReadingRailStaysFixedWhileMutedTextGainsContrast() throws Exception {
        String tokens = read("resources/css/tokens.css");

        assertTrue(tokens.contains("--page-content-max-width: 1018px;"));
        assertTrue(tokens.contains("--palette-text-muted: #5B6672;"));
    }

    @Test
    void dashboardAssigneeGroupsUseEqualHeight() throws Exception {
        String dashboard = read("resources/css/pages/dashboard.css");

        assertTrue(dashboard.contains("grid-auto-rows: 1fr;"));
        assertFalse(dashboard.contains("align-items: start;"));
        assertFalse(dashboard.contains("align-self: start;"));
        assertTrue(dashboard.contains("block-size: 100%;"));
    }

    @Test
    void meetingTextEnhancementKeepsUntrustedContentAsText() throws Exception {
        String page = read("meeting/meeting_view.jsp");
        String behavior = read("resources/js/pages/meeting_view.js");
        String styles = read("resources/css/pages/meeting_view.css");

        assertTrue(page.contains("data-meeting-text"));
        assertTrue(behavior.contains("enhanceMeetingText"));
        assertTrue(behavior.contains("document.createTextNode"));
        assertTrue(behavior.contains("heading.textContent"));
        assertTrue(behavior.contains("item.textContent"));
        assertTrue(behavior.contains("container.replaceChildren(fragment)"));
        assertFalse(behavior.contains("innerHTML"));
        assertTrue(styles.contains(".meeting-text-divider"));
        assertTrue(styles.contains(".meeting-text-list"));
    }

    @Test
    void sharedTablesStayDenseAndEmptyRowsStayCentered() throws Exception {
        String styles = read("resources/css/ui-system.css");

        assertTrue(styles.contains("block-size: 44px;"));
        assertTrue(styles.contains("inline-size: 148px;"));
        assertTrue(styles.contains("inline-size: 104px;"));
        assertTrue(styles.contains(".ui-data-table td.empty-state"));
        assertTrue(styles.contains("text-align: center;"));
    }

    @Test
    void historyChartLimitsXAxisLabelsAtEveryViewport() throws Exception {
        String behavior = read("resources/js/pages/maintenance_history.js");

        assertTrue(behavior.contains("autoSkip: true"));
        assertTrue(behavior.contains("maxTicksLimit: compactChart ? 5 : 8"));
        assertTrue(behavior.contains("maxRotation: compactChart ? 0 : 35"));
    }

    @Test
    void shortPagesKeepTheFooterAtTheViewportEnd() throws Exception {
        String styles = read("resources/css/ambient-background.css");

        assertTrue(styles.contains("display: flex;"));
        assertTrue(styles.contains("flex-direction: column;"));
        assertTrue(styles.contains("margin-block-start: auto;"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
