package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MinimalPaletteContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern PALETTE_COLOR = Pattern.compile(
            "(?m)^\\s*--palette-([a-z-]+):\\s*(#[0-9A-Fa-f]{6});");
    private static final Pattern OPAQUE_COLOR = Pattern.compile("#[0-9A-Fa-f]{6}\\b");
    private static final Pattern ANY_HEX_COLOR = Pattern.compile("#[0-9A-Fa-f]{3,8}\\b");
    private static final Pattern ENCODED_HEX_COLOR = Pattern.compile("(?i)%23[0-9a-f]{3,8}\\b");
    private static final Pattern COLOR_FUNCTION = Pattern.compile(
            "(?i)\\b(?:rgb|hsl)a?\\s*\\(");

    @Test
    void globalLightThemeUsesExactlyTheApprovedEighteenOpaqueColors() throws Exception {
        String tokens = read("resources/css/tokens.css");
        Map<String, String> palette = palette(tokens);

        assertEquals(expectedLight(), palette);
        assertEquals(18, palette.size());

        Set<String> opaqueColors = new LinkedHashSet<>();
        Matcher matcher = OPAQUE_COLOR.matcher(tokens);
        while (matcher.find()) {
            opaqueColors.add(matcher.group().toUpperCase());
        }
        assertEquals(new LinkedHashSet<>(expectedLight().values()), opaqueColors);
        assertAccessible(palette);
        assertTrue(tokens.contains("--color-text-secondary: var(--palette-text-muted);"));
        assertTrue(tokens.contains("--color-icon-strong: var(--palette-text);"));
        assertTrue(tokens.contains("--color-icon: var(--palette-text-muted);"));
        assertTrue(tokens.contains("--color-icon-active: var(--palette-brand);"));
        assertTrue(tokens.contains("--color-chart-usage: var(--palette-brand);"));
        assertTrue(tokens.contains("--color-chart-used: var(--palette-success);"));
        assertTrue(tokens.contains(
                "--color-chart-capacity: var(--palette-border-strong);"));
        assertFalse(tokens.contains(
                "--color-chart-capacity: var(--palette-warning);"));
    }

    @Test
    void applicationIsLightOnlyEvenWhenTheOperatingSystemPrefersDark() throws Exception {
        String tokens = read("resources/css/tokens.css");
        String base = read("resources/css/base.css");

        assertTrue(tokens.contains("color-scheme: light;"));
        assertTrue(base.contains("color-scheme: light;"));
        assertFalse(tokens.contains("prefers-color-scheme"));
        assertFalse(tokens.contains("color-scheme: dark"));

        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".css")
                            || file.toString().endsWith(".js")
                            || file.toString().endsWith(".jsp")
                            || file.toString().endsWith(".jspf")
                            || file.toString().endsWith(".tag"))
                    .toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("prefers-color-scheme"), path.toString());
                assertFalse(source.contains("minimal-tone-pilot"), path.toString());
                assertFalse(source.contains("--pilot-"), path.toString());
            }
        }
    }

    @Test
    void runtimeStylesAndScriptsKeepColorLiteralsInTokensOnly() throws Exception {
        try (var paths = Files.walk(WEBAPP.resolve("resources"))) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".css")
                            || file.toString().endsWith(".js"))
                    .filter(file -> !file.startsWith(
                            WEBAPP.resolve("resources/vendor")))
                    .filter(file -> !file.equals(WEBAPP.resolve("resources/css/tokens.css")))
                    .toList()) {
                String source = Files.readString(path);
                assertFalse(ANY_HEX_COLOR.matcher(source).find(), path.toString());
                assertFalse(ENCODED_HEX_COLOR.matcher(source).find(), path.toString());
                assertFalse(COLOR_FUNCTION.matcher(source).find(), path.toString());
            }
        }
    }

    @Test
    void listAndSummaryPagesKeepOneOrFewerVisiblePrimaryActions() throws Exception {
        assertEquals(0, occurrences(read("customers/customers_list.jsp"), "button--primary"));
        assertEquals(1, occurrences(read("troubleshooting/troubleshooting_list.jsp"), "button--primary"));
        assertEquals(1, occurrences(read("meeting/meeting_list.jsp"), "button--primary"));
        assertEquals(1, occurrences(read("maintenance/maintenance_history.jsp"), "button--primary"));
        assertEquals(1, occurrences(read("mypage/mypage.jsp"), "button--primary"));
        assertEquals(0, occurrences(read("customers/customers_detail.jsp"), "button--primary"));

        String monthly = read("mypage/monthly_customer_response.jsp");
        String monthlyPage = monthly.substring(0, monthly.indexOf("<!-- 추가/수정 모달 -->"));
        assertEquals(1, occurrences(monthlyPage, "button--primary"));

        String meetingView = read("meeting/meeting_view.jsp");
        String meetingHeader = meetingView.substring(0, meetingView.indexOf("<!-- 댓글 섹션 -->"));
        assertEquals(0, occurrences(meetingHeader, "button--primary"));
    }

    @Test
    void normalDashboardSurfacesAreFlatWhileOverlaysKeepElevation() throws Exception {
        String page = read("dashboard.jsp");
        String styles = read("resources/css/pages/dashboard.css");
        String myPageStyles = read("resources/css/pages/mypage.css");

        assertTrue(page.contains("pageBodyClass\" value=\"page-1050 dashboard-page\""));
        assertTrue(styles.contains("/* Dashboard refinements under the shared global Light palette. */"));
        assertFalse(page.contains("maintenance-kpi-section"));
        assertFalse(styles.contains(".maintenance-kpi-"));
        assertTrue(styles.contains(".dashboard-page .maintenance-month-board {"));
        assertTrue(styles.contains("background: var(--color-surface);"));
        assertTrue(styles.contains("border: 1px solid var(--color-border);"));
        assertTrue(styles.contains("box-shadow: none;"));
        assertTrue(styles.contains("outline: 2px solid var(--color-primary);"));
        assertTrue(styles.contains("border-color: var(--color-border-strong);"));
        assertTrue(myPageStyles.contains(".page-mypage .vm-modal"));
        assertTrue(myPageStyles.contains("box-shadow: var(--shadow-lg);"));
        assertFalse(styles.contains("--pilot-"));
        assertFalse(styles.contains("minimal-tone-pilot"));
        assertFalse(Pattern.compile("#[0-9A-Fa-f]{3,8}\\b").matcher(styles).find());
        assertFalse(Pattern.compile("(?i)\\b(?:rgb|hsl)a?\\s*\\(").matcher(styles).find());
        assertFalse(styles.contains("!important"));
    }

    @Test
    void structuralIconsUseNeutralForegroundsWhileActionsKeepTheAccent() throws Exception {
        String components = read("resources/css/components.css");
        String base = read("resources/css/base.css");
        String customerDetail = read("resources/css/pages/customer_detail.css");
        String dashboard = read("resources/css/pages/dashboard.css");
        String maintenanceCards = read("resources/css/pages/maintenance_cards.css");
        String maintenanceHistory = read("resources/css/pages/maintenance_history.css");

        assertTrue(components.contains(".page-header .ph-title h1 i"));
        assertTrue(components.contains("color: currentColor;"));
        assertTrue(components.contains(".card-header i"));
        assertTrue(base.contains("color: var(--color-icon);"));
        assertTrue(customerDetail.contains(".detail-section-title i"));
        assertFalse(dashboard.contains(".maintenance-month-title > i"));
        assertTrue(dashboard.contains(".maintenance-assignee-customer::before"));
        assertTrue(maintenanceCards.contains(".maintenance-management .inspector-header i { color: currentColor;"));
        assertTrue(maintenanceCards.contains(".maintenance-management .customer-name i { color: currentColor;"));
        assertTrue(maintenanceHistory.contains(".maintenance-history .detail-item i"));
        assertTrue(maintenanceHistory.contains("color: currentColor;"));
    }

    private static Map<String, String> palette(String source) {
        Map<String, String> colors = new LinkedHashMap<>();
        Matcher matcher = PALETTE_COLOR.matcher(source);
        while (matcher.find()) {
            colors.put(matcher.group(1), matcher.group(2).toUpperCase());
        }
        return colors;
    }

    private static Map<String, String> expectedLight() {
        return expected(
                "#EFF2F5", "#D9DEE4", "#F8F9FA", "#EEF1F4", "#D5DAE0", "#87919B",
                "#20252B", "#47535F", "#646F7A", "#E7EDF2", "#455F7A",
                "#344A60", "#EEF6F1", "#347A58", "#FFF6E5", "#B54708",
                "#FFF1F1", "#B64B4B");
    }

    private static Map<String, String> expected(String... values) {
        String[] roles = {
                "canvas", "ambient", "surface", "surface-muted", "border", "border-strong",
                "text-strong", "text", "text-muted", "brand-subtle", "brand",
                "brand-hover", "success-subtle", "success", "warning-subtle",
                "warning", "danger-subtle", "danger"
        };
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < roles.length; index++) {
            result.put(roles[index], values[index]);
        }
        return result;
    }

    private static void assertAccessible(Map<String, String> colors) {
        assertContrast(colors, "text", "surface", 4.5);
        assertContrast(colors, "text-muted", "canvas", 4.5);
        assertContrast(colors, "brand", "brand-subtle", 4.5);
        assertContrast(colors, "success", "success-subtle", 4.5);
        assertContrast(colors, "warning", "warning-subtle", 4.5);
        assertContrast(colors, "danger", "danger-subtle", 4.5);
        assertContrast(colors, "border-strong", "surface", 3.0);
    }

    private static void assertContrast(
            Map<String, String> colors,
            String foreground,
            String background,
            double minimum) {
        double foregroundLuminance = luminance(colors.get(foreground));
        double backgroundLuminance = luminance(colors.get(background));
        double ratio = (Math.max(foregroundLuminance, backgroundLuminance) + 0.05)
                / (Math.min(foregroundLuminance, backgroundLuminance) + 0.05);
        assertTrue(ratio >= minimum,
                () -> foreground + "/" + background + " contrast was " + ratio + ":1");
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

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
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
}
