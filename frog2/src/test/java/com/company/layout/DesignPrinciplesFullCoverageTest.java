package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DesignPrinciplesFullCoverageTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path CSS_ROOT = WEBAPP.resolve("resources/css");
    private static final Pattern CSS_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");
    private static final Pattern COLOR_FUNCTION = Pattern.compile(
            "(?i)\\b(?:rgb|hsl)a?\\s*\\(");
    private static final Pattern LITERAL_FONT_SIZE = Pattern.compile(
            "(?i)font-size\\s*:\\s*[0-9.]+(?:px|rem|em)\\b");
    private static final Pattern TRANSITION_ALL = Pattern.compile(
            "(?i)transition(?:-property)?\\s*:\\s*all\\b");
    private static final Pattern VARIABLE_DEFINITION = Pattern.compile(
            "(?m)(--[a-zA-Z0-9-]+)\\s*:");
    private static final Pattern VARIABLE_USE = Pattern.compile(
            "var\\(\\s*(--[a-zA-Z0-9-]+)");
    private static final Pattern BREAKPOINT = Pattern.compile(
            "@media\\s*\\([^)]*(?:min|max)-width\\s*:\\s*(\\d+)px",
            Pattern.CASE_INSENSITIVE);

    @Test
    void tokensCoverTypographyColorSpacingLayoutAndMotion() throws Exception {
        String tokens = readCss("tokens.css");
        for (String token : List.of(
                "--font-base:",
                "--font-mono:",
                "--font-size-2xs:",
                "--font-size-md: 1rem",
                "--font-size-lg: 1.25rem",
                "--font-size-xl: 1.563rem",
                "--font-size-2xl: 1.953rem",
                "--font-size-3xl: 2.441rem",
                "--line-height-base: 1.6",
                "--line-height-relaxed: 1.75",
                "--measure-compact: 52ch",
                "--measure-readable: 65ch",
                "--measure-wide: 75ch",
                "--space-4: 4px",
                "--space-8: 8px",
                "--space-16: 16px",
                "--space-24: 24px",
                "--page-content-max-width: 1018px",
                "--control-height-md: 44px",
                "--motion-instant: 80ms",
                "--motion-slow: 320ms")) {
            assertTrue(tokens.contains(token), token);
        }
        assertTrue(tokens.contains("color-scheme: light"));
        assertFalse(tokens.contains("prefers-color-scheme"));
        assertFalse(tokens.contains("color-scheme: dark"));
        assertTrue(tokens.contains("@media (prefers-reduced-motion: reduce)"));
    }

    @Test
    void runtimeStylesUseTheTokenContractWithoutPageLevelColorOrTypeDrift()
            throws Exception {
        for (Path stylesheet : cssFiles()) {
            String css = withoutComments(Files.readString(stylesheet));
            String relative = CSS_ROOT.relativize(stylesheet).toString();
            assertFalse(TRANSITION_ALL.matcher(css).find(), relative);
            assertFalse(LITERAL_FONT_SIZE.matcher(css).find(), relative);
            if (!relative.equals("tokens.css")) {
                assertFalse(HEX_COLOR.matcher(css).find(), relative);
                assertFalse(COLOR_FUNCTION.matcher(css).find(), relative);
            }
            if (relative.startsWith("pages/")) {
                assertFalse(css.contains("!important"), relative);
                Matcher breakpoints = BREAKPOINT.matcher(css);
                while (breakpoints.find()) {
                    int width = Integer.parseInt(breakpoints.group(1));
                    assertTrue(Set.of(480, 768, 1024).contains(width),
                            relative + " uses unsupported breakpoint " + width);
                }
            }
        }
    }

    @Test
    void everyCssVariableReferenceHasADefinition() throws Exception {
        Set<String> definitions = new HashSet<>();
        Set<String> uses = new HashSet<>();
        for (Path stylesheet : cssFiles()) {
            String css = withoutComments(Files.readString(stylesheet));
            collect(VARIABLE_DEFINITION, css, definitions);
            collect(VARIABLE_USE, css, uses);
        }
        uses.removeAll(definitions);
        assertTrue(uses.isEmpty(), "Undefined CSS variables: " + uses);
    }

    @Test
    void lightSemanticPairsMeetWcagAa() throws Exception {
        String tokens = readCss("tokens.css");
        assertThemeContrast(tokens, "light");
    }

    @Test
    void navigationAndFeedbackSupportKeyboardContextAndRecovery() throws Exception {
        String shellHeader = read("includes/header.jsp");
        String shellFooter = read("includes/footer.jsp");
        String navigation = read("WEB-INF/includes/header_nav.jspf");
        String navigationScript = read("resources/js/header_nav.js");
        String uiScript = read("resources/js/ui-system.js");
        String maintenanceScript = read("resources/js/pages/maintenance_form.js");

        assertTrue(shellHeader.contains("href=\"#main-content\""));
        assertTrue(shellHeader.contains("<main id=\"main-content\""));
        assertTrue(shellFooter.contains("</main>"));
        assertTrue(navigation.contains("aria-keyshortcuts=\"Control+K Meta+K\""));
        assertTrue(navigation.contains("role=\"combobox\""));
        assertTrue(navigation.contains("role=\"listbox\""));
        for (String key : List.of("ArrowDown", "ArrowUp", "Home", "End", "Enter", "Escape")) {
            assertTrue(navigationScript.contains("'" + key + "'"), key);
        }
        assertTrue(navigationScript.contains("target.isContentEditable"));
        assertTrue(uiScript.contains("function createDialogController"));
        assertTrue(uiScript.contains("function setButtonLoading"));
        assertTrue(uiScript.contains("function showFieldError"));
        assertTrue(uiScript.contains("aria-busy"));
        assertTrue(maintenanceScript.contains("retryMaintenanceOptions"));
        assertTrue(maintenanceScript.contains("showOptionsStatus"));

        int nativeConfirmCalls = 0;
        try (var paths = Files.walk(WEBAPP.resolve("resources/js"))) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".js"))
                    .toList()) {
                nativeConfirmCalls += occurrences(Files.readString(path), "window.confirm(");
            }
        }
        assertEquals(1, nativeConfirmCalls,
                "Native confirmation must remain behind the shared helper");

        Pattern emptyFragment = Pattern.compile(
                "href\\s*=\\s*['\"]#['\"]", Pattern.CASE_INSENSITIVE);
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(DesignPrinciplesFullCoverageTest::isMarkup)
                    .toList()) {
                assertFalse(emptyFragment.matcher(Files.readString(path)).find(),
                        path.toString());
            }
        }
    }

    @Test
    void progressiveEnhancementHasSafeFallbacksAndReducedMotion() throws Exception {
        String uiStyles = readCss("ui-system.css");
        String dashboardStyles = readCss("pages/dashboard.css");
        String historyScript = read("resources/js/pages/maintenance_history.js");
        String meetingView = read("meeting/meeting_view.jsp");

        assertTrue(uiStyles.contains(":has("));
        assertTrue(uiStyles.contains("@starting-style"));
        assertTrue(dashboardStyles.contains("container-type: inline-size"));
        assertTrue(dashboardStyles.contains("@container dashboard-maintenance"));
        assertTrue(historyScript.contains("prefers-reduced-motion: reduce"));
        assertTrue(historyScript.contains("typeof item.animate === 'function'"));
        assertTrue(meetingView.contains("hidden"));
        assertTrue(meetingView.contains("aria-expanded"));
    }

    private static void assertThemeContrast(String theme, String label) {
        for (String foreground : List.of(
                "--color-text",
                "--color-text-secondary",
                "--color-text-muted",
                "--color-link",
                "--color-success",
                "--color-danger",
                "--color-warning",
                "--color-info")) {
            assertContrast(
                    color(theme, foreground),
                    color(theme, "--color-background"),
                    4.5,
                    label + " " + foreground);
        }
        for (String tone : List.of("primary", "success", "danger", "warning", "info")) {
            assertContrast(
                    color(theme, "--color-on-" + tone),
                    color(theme, "--color-" + tone),
                    4.5,
                    label + " on-" + tone);
        }
        for (String tone : List.of("success", "danger", "warning", "info")) {
            assertContrast(
                    color(theme, "--color-" + tone + "-text"),
                    color(theme, "--color-" + tone + "-bg"),
                    4.5,
                    label + " " + tone + " message");
        }
    }

    private static String color(String source, String name) {
        Pattern declaration = Pattern.compile(
                Pattern.quote(name) + "\\s*:\\s*([^;]+)\\s*;");
        String current = name;
        for (int depth = 0; depth < 20; depth++) {
            Matcher matcher = declaration.matcher(source);
            assertTrue(matcher.find(), "Missing color token " + current);
            String value = matcher.group(1).trim();
            if (value.matches("#[0-9a-fA-F]{6}")) {
                return value;
            }
            Matcher alias = Pattern.compile("var\\(\\s*(--[a-zA-Z0-9-]+)\\s*\\)")
                    .matcher(value);
            assertTrue(alias.matches(), "Unsupported color token value " + current + ": " + value);
            current = alias.group(1);
            declaration = Pattern.compile(
                    Pattern.quote(current) + "\\s*:\\s*([^;]+)\\s*;");
        }
        throw new AssertionError("Color token alias cycle for " + name);
    }

    private static void assertContrast(
            String foreground, String background, double minimum, String label) {
        double foregroundLuminance = luminance(foreground);
        double backgroundLuminance = luminance(background);
        double ratio = (Math.max(foregroundLuminance, backgroundLuminance) + 0.05)
                / (Math.min(foregroundLuminance, backgroundLuminance) + 0.05);
        assertTrue(ratio >= minimum,
                () -> label + " contrast was " + ratio + ":1");
    }

    private static double luminance(String color) {
        int red = Integer.parseInt(color.substring(1, 3), 16);
        int green = Integer.parseInt(color.substring(3, 5), 16);
        int blue = Integer.parseInt(color.substring(5, 7), 16);
        return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue);
    }

    private static double linear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045
                ? value / 12.92
                : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static void collect(Pattern pattern, String source, Set<String> target) {
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            target.add(matcher.group(1));
        }
    }

    private static List<Path> cssFiles() throws IOException {
        try (var paths = Files.walk(CSS_ROOT)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".css"))
                    .sorted()
                    .toList();
        }
    }

    private static boolean isMarkup(Path path) {
        String name = path.toString();
        return name.endsWith(".jsp") || name.endsWith(".jspf") || name.endsWith(".tag");
    }

    private static String withoutComments(String css) {
        return CSS_COMMENT.matcher(css).replaceAll("");
    }

    private static String readCss(String path) throws IOException {
        return Files.readString(CSS_ROOT.resolve(path));
    }

    private static String read(String path) throws IOException {
        return Files.readString(WEBAPP.resolve(path));
    }

    private static int occurrences(String source, String target) {
        return source.split(Pattern.quote(target), -1).length - 1;
    }
}
