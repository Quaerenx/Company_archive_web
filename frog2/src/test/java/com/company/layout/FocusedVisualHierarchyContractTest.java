package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FocusedVisualHierarchyContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void pageTitlesLeadWithoutDecorativeIcons() throws Exception {
        String components = read("resources/css/components.css");

        assertTrue(components.contains(".page-header .ph-title h1 {"));
        assertTrue(components.contains("font-size: var(--font-size-2xl);"));
        assertTrue(components.contains("letter-spacing: -0.02em;"));
        assertTrue(components.contains(".page-header .ph-title h1 i {"));
        assertTrue(components.contains("display: none;"));
    }

    @Test
    void navigationUsesAccentForCurrentStateInsteadOfHover() throws Exception {
        String header = read("resources/css/pages/header.css");

        assertTrue(header.contains("keep only functional indicators visible"));
        assertTrue(header.contains(".main-nav .nav-link > i:not(.dropdown-chevron)"));
        assertTrue(header.contains(".main-nav .nav-link:hover,"));
        assertTrue(header.contains("color: var(--color-navigation-text-strong);"));
        assertTrue(header.contains(".main-nav .nav-link.active,"));
        assertTrue(header.contains("background: var(--color-navigation-current);"));
    }

    @Test
    void dashboardEmphasizesOnePrimaryWorkAreaWithMeasuredRhythm() throws Exception {
        String dashboard = read("resources/css/pages/dashboard.css");

        assertFalse(dashboard.contains("maintenance-kpi"));
        assertTrue(dashboard.contains(".dashboard-page .maintenance-month-title h2 {"));
        assertTrue(dashboard.contains("font-size: var(--font-size-2xl);"));
        assertTrue(dashboard.contains(".dashboard-page .maintenance-month-label {"));
        assertTrue(dashboard.contains(".dashboard-page .maintenance-assignee-grid {"));
        assertTrue(dashboard.contains(".dashboard-page .maintenance-assignee-name {"));
        assertTrue(dashboard.contains(".dashboard-page .maintenance-assignee-customer::before {"));
        assertFalse(dashboard.contains(".dashboard-page .dashboard-action-group"));
        assertFalse(dashboard.contains(".dashboard-page .vm-board"));
        assertTrue(dashboard.contains("background-color var(--motion-fast) var(--motion-ease)"));
    }

    @Test
    void containerWidthAndMotionTokensRemainStable() throws Exception {
        String tokens = read("resources/css/tokens.css");

        assertTrue(tokens.contains("--page-content-max-width: 1018px;"));
        assertTrue(tokens.contains("--page-content-gutter: var(--space-16);"));
        assertTrue(tokens.contains("--motion-fast: 120ms;"));
        assertTrue(tokens.contains("--motion-base: 180ms;"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
