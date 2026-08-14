package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisualRegressionToolContractTest {
    private static final Path TOOLS = Path.of("src/tools");

    @Test
    void routeManifestCoversNineReleaseScreensWithoutPersistingDetailIdentifiers()
            throws Exception {
        List<String> lines = Files.readAllLines(
                TOOLS.resolve("visual-regression-routes.tsv"));
        List<String> names = lines.stream()
                .filter(line -> !line.isBlank())
                .map(line -> line.split("\\t", -1)[0])
                .toList();

        assertEquals(List.of(
                "login",
                "dashboard",
                "customers",
                "customer-detail",
                "maintenance-history",
                "meeting",
                "troubleshooting",
                "file-repository",
                "mypage"), names);
        assertTrue(lines.stream().anyMatch(
                line -> line.equals("customer-detail\t@customer-detail\tauthenticated")));
        assertTrue(lines.stream().anyMatch(
                line -> line.equals("maintenance-history\t@maintenance-history\tauthenticated")));
        assertTrue(lines.stream().anyMatch(
                line -> line.equals("login\tlogin\tpublic")));
        assertFalse(String.join("\n", lines).contains("customerName="));
    }

    @Test
    void captureRunnerUsesFiveRequiredWidthsAndRouteOnlyOutputNames()
            throws Exception {
        String capture = Files.readString(
                TOOLS.resolve("capture-visual-regression.mjs"));

        for (String viewport : List.of(
                "[360, 900]",
                "[390, 900]",
                "[768, 1024]",
                "[1024, 900]",
                "[1440, 1000]")) {
            assertTrue(capture.contains(viewport), viewport);
        }
        assertTrue(capture.contains("resolveRouteUrl(route)"));
        assertTrue(capture.contains("route.access"));
        assertTrue(capture.contains("@customer-detail"));
        assertTrue(capture.contains("@maintenance-history"));
        assertTrue(capture.contains("`${route.name}-${width}x${height}.png`"));
        assertFalse(capture.contains("customerName}-${width}"));
    }

    @Test
    void shellSeparatesPublicAndAuthenticatedProfilesAndRecordsMetrics()
            throws Exception {
        String shell = Files.readString(TOOLS.resolve("visual-regression.sh"));
        String capture = Files.readString(
                TOOLS.resolve("capture-visual-regression.mjs"));

        assertTrue(shell.contains("PUBLIC_PROFILE="));
        assertTrue(shell.contains(" public"));
        assertTrue(shell.contains(" authenticated"));
        assertTrue(capture.contains("scrollWidth"));
        assertTrue(capture.contains("window.scrollTo(0, 0)"));
        assertTrue(capture.contains("scrollX: metrics.scrollX"));
        assertTrue(capture.contains("scrollY: metrics.scrollY"));
        assertTrue(capture.contains("metrics.scrollX !== 0 || metrics.scrollY !== 0"));
        assertTrue(capture.contains("consoleErrorCount"));
        assertTrue(capture.contains("capture-metrics.json"));
        assertFalse(capture.contains("e2ePassword" + "}"));
    }
}
