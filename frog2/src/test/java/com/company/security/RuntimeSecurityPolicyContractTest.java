package com.company.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class RuntimeSecurityPolicyContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern INLINE_SCRIPT = Pattern.compile(
            "<script\\b(?![^>]*\\bsrc\\s*=)", Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_HANDLER = Pattern.compile(
            "\\son[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);

    @Test
    void sessionCookiePolicyIsHttpOnlyAndStrictSameSite() throws Exception {
        String webXml = Files.readString(WEBAPP.resolve("WEB-INF/web.xml"));
        String context = Files.readString(WEBAPP.resolve("META-INF/context.xml"));

        assertTrue(webXml.contains("<http-only>true</http-only>"));
        assertTrue(context.contains("sameSiteCookies=\"strict\""));
    }

    @Test
    void applicationViewsDoNotRequireUnsafeInlineCsp() throws Exception {
        try (Stream<Path> files = Files.walk(WEBAPP)) {
            for (Path file : files.filter(Files::isRegularFile)
                    .filter(RuntimeSecurityPolicyContractTest::isTemplate)
                    .toList()) {
                String source = Files.readString(file);
                assertFalse(INLINE_SCRIPT.matcher(source).find(), file.toString());
                assertFalse(source.toLowerCase().contains("<style"), file.toString());
                assertFalse(source.toLowerCase().contains(" style="), file.toString());
                assertFalse(INLINE_HANDLER.matcher(source).find(), file.toString());
            }
        }

        String poolMonitor = Files.readString(Path.of(
                "src/main/java/com/company/controller/PoolMonitorServlet.java"));
        assertFalse(poolMonitor.contains("<style"));
        assertFalse(poolMonitor.contains("onclick="));
        assertFalse(poolMonitor.contains(" style="));
    }

    private static boolean isTemplate(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jsp")
                || name.endsWith(".jspf")
                || name.endsWith(".tag")
                || name.endsWith(".html");
    }
}
