package com.company.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SecurityRouteMatrixContractTest {
    private static final Path WEB_XML = Path.of("src/main/webapp/WEB-INF/web.xml");
    private static final Path MATRIX = Path.of(
            "docs/security/url-authorization-matrix-20260810.md");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "<url-pattern>([^<]+)</url-pattern>");

    @Test
    void everyConfiguredServletRouteIsRecordedInTheSecurityMatrix() throws Exception {
        String webXml = Files.readString(WEB_XML);
        String matrix = Files.readString(MATRIX);

        var matcher = URL_PATTERN.matcher(webXml);
        while (matcher.find()) {
            String route = matcher.group(1).trim();
            if ("/*".equals(route)) {
                continue;
            }
            assertTrue(
                    matrix.contains("`" + route + "`"),
                    () -> "Security matrix is missing configured route " + route);
        }
    }

    @Test
    void matrixRecordsRequiredAuthorizationDimensions() throws Exception {
        String matrix = Files.readString(MATRIX);

        for (String heading : new String[] {
                "Method", "Access", "Effect", "CSRF", "Ownership"
        }) {
            assertTrue(matrix.contains(heading),
                    () -> "Security matrix is missing column " + heading);
        }
        assertTrue(matrix.contains("Direct JSP"));
        assertTrue(matrix.contains("Legacy redirect"));
    }
}
