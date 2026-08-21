package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DarkCanvasVisualRegressionContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final String HEADER_INCLUDE = "include file=\"/includes/header.jsp\"";

    @Test
    void everyAuthenticatedViewUsesOneSharedContentAndHeadingShell() throws Exception {
        List<Path> authenticatedPages;
        try (var paths = Files.walk(WEBAPP)) {
            authenticatedPages = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".jsp"))
                    .filter(path -> read(path).contains(HEADER_INCLUDE))
                    .toList();
        }

        assertEquals(27, authenticatedPages.size(), "authenticated view inventory changed");
        for (Path page : authenticatedPages) {
            String jsp = read(page);
            assertEquals(1, occurrences(jsp, "content-shell"), page.toString());
            assertEquals(1, occurrences(jsp, "<t:pageHeader"), page.toString());
        }
    }

    @Test
    void exposedContentKeepsAnOpaqueSurfaceOrInverseForeground() throws Exception {
        String components = read("resources/css/components.css");
        String header = read("resources/css/pages/header.css");
        String ui = read("resources/css/ui-system.css");
        String ambient = read("resources/css/ambient-background.css");

        assertTrue(components.contains(".page-header"));
        assertTrue(components.contains("background: var(--color-surface-elevated);"));
        assertTrue(components.contains(".modal-content"));
        assertTrue(header.contains(".main-nav .dropdown-menu"));
        assertTrue(header.contains("background-color: var(--color-surface-elevated);"));
        assertTrue(ui.contains(".ui-toast"));
        assertTrue(ui.contains("background: var(--color-surface-elevated);"));
        assertTrue(ambient.contains(".content-shell > .back-link"));
        assertTrue(ambient.contains("color: var(--color-link);"));
    }

    @Test
    void responsiveContractsCoverRequiredReviewWidths() throws Exception {
        String base = read("resources/css/base.css");
        String ui = read("resources/css/ui-system.css");
        String ambient = read("resources/css/ambient-background.css");
        String script = read("resources/js/ambient-background.js");

        // 360px is covered by the compact 480px contract.
        assertTrue(base.contains("@media (max-width: 480px)"));
        assertTrue(ui.contains("@media (max-width: 480px)"));
        // 768px uses the shared tablet/mobile contract.
        assertTrue(base.contains("@media (max-width: 768px)"));
        assertTrue(ui.contains("@media (max-width: 768px)"));
        // 1024px intentionally uses the static graphite canvas; 1440px enables particles.
        assertTrue(ambient.contains("@media (max-width: 1050px)"));
        assertTrue(script.contains("(min-width: 1051px)"));
        assertTrue(ambient.contains("@media (prefers-reduced-motion: reduce)"));
    }

    @Test
    void outerShellOwnsOneConsistentSectionRhythm() throws Exception {
        String tokens = read("resources/css/tokens.css");
        String ambient = read("resources/css/ambient-background.css");

        assertTrue(tokens.contains("--page-section-gap: var(--space-24);"));
        assertTrue(ambient.contains(".content-shell > * + *"));
        assertTrue(ambient.contains("margin-block-start: var(--page-section-gap);"));
        assertTrue(ambient.contains("--page-section-gap: var(--space-20);"));
    }

    @Test
    void focusAndOverlaysRemainVisibleAboveTheGraphiteCanvas() throws Exception {
        String tokens = read("resources/css/tokens.css");
        String ambient = read("resources/css/ambient-background.css");
        String components = read("resources/css/components.css");
        String header = read("resources/css/pages/header.css");
        String ui = read("resources/css/ui-system.css");
        String myPage = read("resources/css/pages/mypage_hosts.css");

        assertTrue(tokens.contains("--z-background: -1;"));
        assertTrue(tokens.contains("--z-dialog: 1000;"));
        assertTrue(tokens.contains("--z-command-palette: 1100;"));
        assertTrue(tokens.contains("--z-toast: 1200;"));
        assertTrue(tokens.contains("--z-skip-link: 1300;"));
        assertTrue(ambient.contains("isolation: isolate;"));
        assertTrue(ambient.contains("z-index: var(--z-background);"));
        assertTrue(ambient.contains(".back-link:focus-visible"));
        assertTrue(components.contains("z-index: var(--z-dialog);"));
        assertTrue(myPage.contains("z-index: var(--z-dialog);"));
        assertTrue(header.contains("z-index: var(--z-header);"));
        assertTrue(header.contains("z-index: var(--z-command-palette);"));
        assertTrue(ui.contains("z-index: var(--z-toast);"));
        assertTrue(ui.contains("z-index: var(--z-skip-link);"));
    }

    private static int occurrences(String source, String needle) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    private static String read(String relativePath) throws Exception {
        return read(WEBAPP.resolve(relativePath));
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
