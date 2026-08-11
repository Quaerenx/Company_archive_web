package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SelfHostedVendorAssetContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern REMOTE_ASSET = Pattern.compile(
            "(?i)(?:src|href)\\s*=\\s*[\"']https?://"
                    + "|@import\\s+(?:url\\()?\\s*[\"']?https?://");

    @Test
    void sharedHeaderUsesPackagedFontAwesomeWithLicense() throws Exception {
        String header = Files.readString(WEBAPP.resolve("includes/header.jsp"));

        assertTrue(header.contains(
                "/resources/vendor/fontawesome-free/5.15.4/css/all.min.css"));
        assertFalse(header.contains("cdnjs.cloudflare.com"));
        assertTrue(Files.isRegularFile(WEBAPP.resolve(
                "resources/vendor/fontawesome-free/5.15.4/LICENSE.txt")));
        assertTrue(Files.isRegularFile(WEBAPP.resolve(
                "resources/vendor/fontawesome-free/5.15.4/webfonts/fa-solid-900.woff2")));
        assertEquals(
                "99464ceb71bc9bbdcc72275faefe44f98eb5cbb6b5d8ee665b87b35376f1a96e",
                sha256(WEBAPP.resolve(
                        "resources/vendor/fontawesome-free/5.15.4/css/all.min.css")));
    }

    @Test
    void maintenanceHistoryUsesPackagedChartJsWithLicense() throws Exception {
        String page = Files.readString(
                WEBAPP.resolve("maintenance/maintenance_history.jsp"));

        assertTrue(page.contains(
                "/resources/vendor/chart.js/4.4.4/chart.umd.min.js"));
        assertFalse(page.contains("cdn.jsdelivr.net"));
        assertTrue(Files.isRegularFile(WEBAPP.resolve(
                "resources/vendor/chart.js/4.4.4/chart.umd.min.js")));
        assertTrue(Files.isRegularFile(WEBAPP.resolve(
                "resources/vendor/chart.js/4.4.4/LICENSE.md")));
        assertEquals(
                "fed6a739f8d0f0687174de6cd14745fc0fc7809144ab113d22908a26bf0d7fea",
                sha256(WEBAPP.resolve(
                        "resources/vendor/chart.js/4.4.4/chart.umd.min.js")));
        assertTrue(Files.isRegularFile(WEBAPP.resolve(
                "resources/vendor/THIRD_PARTY_NOTICES.md")));
    }

    @Test
    void runtimePagesDoNotLoadScriptsStylesOrFontsFromRemoteOrigins()
            throws Exception {
        Set<String> textExtensions = Set.of(
                ".jsp", ".jspf", ".tag", ".css", ".js");
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> textExtensions.stream()
                            .anyMatch(file.toString()::endsWith))
                    .toList()) {
                assertFalse(
                        REMOTE_ASSET.matcher(Files.readString(path)).find(),
                        () -> "Runtime remote asset reference: " + path);
            }
        }
    }

    private static String sha256(Path path) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path));
        return HexFormat.of().formatHex(digest);
    }
}
