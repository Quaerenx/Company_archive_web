package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class JspScriptletContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern JAVA_SCRIPTLET = Pattern.compile("<%(?!@|--)");

    @Test
    void jspViewsContainNoJavaScriptlets() throws Exception {
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths
                    .filter(Files::isRegularFile)
                    .filter(JspScriptletContractTest::isJspSource)
                    .toList()) {
                String source = Files.readString(path);
                assertFalse(
                        JAVA_SCRIPTLET.matcher(source).find(),
                        () -> "Java scriptlet found in " + path);
            }
        }
    }

    private static boolean isJspSource(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".jsp") || fileName.endsWith(".jspf");
    }
}
