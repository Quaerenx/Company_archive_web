package com.company.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorContractCoverageTest {
    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path WEB_XML = Path.of("src/main/webapp/WEB-INF/web.xml");

    @Test
    void productionCodeDoesNotWriteDirectlyToConsole() throws Exception {
        try (var paths = Files.walk(MAIN_JAVA)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("printStackTrace("), () -> "Direct stack trace output: " + path);
                assertFalse(source.contains("System.out"), () -> "Direct stdout output: " + path);
                assertFalse(source.contains("System.err"), () -> "Direct stderr output: " + path);
            }
        }
    }

    @Test
    void customerControllerDoesNotLogSensitiveRequestState() throws Exception {
        String source = Files.readString(MAIN_JAVA.resolve(
                "com/company/controller/CustomersServlet.java"));
        for (String forbidden : List.of(
                "Session:",
                "User in session:",
                "Customer Name:",
                "Subcluster YN:")) {
            assertFalse(source.contains(forbidden), () -> "Sensitive debug logging remains: " + forbidden);
        }
        assertFalse(source.contains("e.getMessage()"));
    }

    @Test
    void applicationErrorContractIsRegisteredInFilterOrder() throws Exception {
        String webXml = Files.readString(WEB_XML);
        int encoding = webXml.indexOf("<filter-name>CharacterEncodingFilter</filter-name>");
        int application = webXml.indexOf("<filter-name>ApplicationExceptionFilter</filter-name>");
        int headers = webXml.indexOf("<filter-name>SecurityHeadersFilter</filter-name>");

        assertTrue(encoding >= 0 && encoding < application && application < headers);
        assertTrue(webXml.contains("<error-code>409</error-code>"));
        assertTrue(webXml.contains("<error-code>503</error-code>"));
    }
}
