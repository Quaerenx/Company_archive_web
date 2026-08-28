package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DesignFeedbackRefinementContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void compactKoreanTypeKeepsAReadableMinimumAndUiLeading() throws Exception {
        String tokens = read("resources/css/tokens.css");
        String login = read("resources/css/login_style.css");

        assertTrue(tokens.contains("--font-size-xs: 0.8125rem;"));
        assertTrue(tokens.contains("--font-size-2xs: var(--font-size-xs);"));
        assertTrue(tokens.contains("--font-size-sm: 0.9375rem;"));
        assertTrue(tokens.contains("--line-height-ui: 1.5;"));
        assertFalse(tokens.contains("--font-size-xs: 0.8rem;"));
        assertTrue(login.contains(".login-page #userId"));
        assertTrue(login.contains("letter-spacing: 0;"));
    }

    @Test
    void structuralSurfacesUseTheMiddleBorderTier() throws Exception {
        String system = read("resources/css/ui-system.css");
        String components = read("resources/css/components.css");
        String header = read("resources/css/pages/header.css");

        assertTrue(system.contains(
                ".ui-system .table-container:not(.ui-work-surface) {\n"
                        + "    background: var(--color-surface);\n"
                        + "    border: 1px solid var(--color-surface-edge);\n"
                        + "    box-shadow: var(--shadow-sm);"));
        assertTrue(system.contains(
                ".ui-system .ui-form-card {\n"
                        + "    background: var(--color-surface-elevated);\n"
                        + "    border: 1px solid var(--color-surface-edge);"));
        assertTrue(components.contains(
                ".modal-content {\n"
                        + "    background-color: var(--color-surface-elevated);\n"
                        + "    border: 1px solid var(--color-surface-edge);"));
        assertTrue(header.contains(
                ".main-nav .dropdown-menu {"));
        assertTrue(header.contains(
                "border: 1px solid var(--color-surface-edge);"));
    }

    @Test
    void pageHeadersStayQuieterThanInteractiveWorkSurfaces() throws Exception {
        String tokens = read("resources/css/tokens.css");
        String components = read("resources/css/components.css");
        String system = read("resources/css/ui-system.css");

        assertTrue(tokens.contains(
                "--color-surface-subtle: var(--palette-canvas);"));
        assertTrue(tokens.contains(
                "--color-surface-muted: var(--palette-surface-muted);"));
        assertTrue(tokens.contains(
                "--color-surface-hover: var(--palette-ambient);"));
        assertTrue(tokens.contains(
                "--color-text-disabled: var(--palette-border-strong);"));
        assertTrue(components.contains(
                ".page-header {\n"
                        + "    background: var(--color-surface-elevated);\n"
                        + "    border: 1px solid var(--color-border);"));
        assertTrue(components.contains("    box-shadow: none;"));
        assertTrue(system.contains(
                ".ui-system .ui-work-surface {\n"
                        + "    background: var(--color-surface);\n"
                        + "    border: 1px solid var(--color-surface-edge);\n"
                        + "    box-shadow: var(--shadow-sm);"));
    }

    @Test
    void riskProgressAddsANonColorPatternWithoutChangingApprovedColors()
            throws Exception {
        String tokens = read("resources/css/tokens.css");
        String history = read("resources/css/pages/maintenance_history.css");

        assertTrue(tokens.contains("--palette-success: #347A58;"));
        assertTrue(tokens.contains("--palette-warning: #D4A900;"));
        assertTrue(tokens.contains("--palette-danger: #B64B4B;"));
        assertTrue(tokens.contains("--progress-risk-fill: repeating-linear-gradient("));
        assertTrue(tokens.contains(
                "var(--palette-brand-hover) var(--space-4) 6px"));
        assertFalse(tokens.contains(
                "var(--color-danger-bg) var(--space-4) 6px"));
        assertTrue(history.contains(
                ".history-license-progress--risk::-webkit-progress-value {\n"
                        + "    background: var(--progress-risk-fill);"));
        assertTrue(history.contains(
                ".history-license-progress--risk::-moz-progress-bar {\n"
                        + "    background: var(--progress-risk-fill);"));
        assertTrue(history.contains(
                ".history-license-percent--warning {\n"
                        + "    background: var(--color-warning-bg);\n"
                        + "    border-inline-start: 3px solid var(--color-warning-accent);"));
    }

    @Test
    void neutralMissingValuesUseOneHyphenConvention() throws Exception {
        String history = read("maintenance/maintenance_history.jsp");
        String fields = read("WEB-INF/includes/maintenance_form_fields.jspf");

        assertFalse(history.contains("—"));
        assertFalse(fields.contains("—"));
        assertTrue(history.contains("특이사항 없음"));
        assertTrue(history.contains("default=\"-\""));
    }

    @Test
    void humanReadableDatesUseOneIsoStyle() throws Exception {
        for (String page : new String[] {
                "maintenance/maintenance_history.jsp",
                "troubleshooting/troubleshooting_view.jsp",
                "meeting/meeting_list.jsp",
                "meeting/meeting_view.jsp",
                "meeting/meeting_edit.jsp",
                "WEB-INF/includes/maintenance_form_fields.jspf",
                "WEB-INF/includes/mypage/recent_activity.jspf"
        }) {
            String source = read(page);
            assertFalse(source.contains("pattern=\"yyyy.MM"), page);
            assertFalse(source.contains("pattern=\"yyyy년"), page);
        }
    }

    @Test
    void customerTableUsesSemanticAlignmentClasses() throws Exception {
        String page = read("customers/customers_list.jsp");
        String customerStyles = read("resources/css/pages/customers.css");
        String system = read("resources/css/ui-system.css");

        assertTrue(page.contains(
                "customer-table ui-table ui-data-table"));
        assertTrue(page.contains("customer-col-nodes col--numeric"));
        assertTrue(page.contains("customer-col-license col--numeric"));
        assertTrue(page.contains("customer-license-unit"));
        assertFalse(customerStyles.contains("td:nth-child"));
        assertFalse(customerStyles.contains("th:nth-child"));
        assertTrue(system.contains(
                ".ui-system .ui-data-table .col--date"));
        assertTrue(system.contains("text-align: right;"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
