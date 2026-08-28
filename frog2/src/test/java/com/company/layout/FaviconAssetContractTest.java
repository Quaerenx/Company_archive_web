package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FaviconAssetContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void faviconSvgIsSelfContainedAndContainsNoActiveContent() throws Exception {
        String svg = Files.readString(WEBAPP.resolve("favicon.svg"));
        String lower = svg.toLowerCase();

        assertTrue(svg.contains("width=\"32\" height=\"32\""));
        assertTrue(svg.contains("viewBox=\"0 0 1024 1024\""));
        assertTrue(svg.contains("fill=\"#1B364B\""));
        assertTrue(svg.contains("<path"));
        assertTrue(svg.contains("fill=\"#F9FBFB\""));
        assertEquals(0, occurrences(svg, "<image "));
        assertEquals(0, occurrences(svg, "data:image/"));
        assertEquals(0, occurrences(svg, "<text"));
        assertFalse(lower.contains("<script"));
        assertFalse(lower.contains("<foreignobject"));
        assertFalse(lower.contains("javascript:"));
        assertFalse(lower.contains("xlink:href"));
        assertFalse(lower.contains("onload="));
        assertFalse(lower.contains("onclick="));
        assertFalse(lower.contains("@import"));
    }

    @Test
    void allDocumentShellsUseTheSharedFaviconMarkup() throws Exception {
        String include = Files.readString(WEBAPP.resolve("WEB-INF/includes/favicon.jspf"));
        assertTrue(include.contains("/favicon.svg"));
        assertTrue(include.contains("type=\"image/svg+xml\""));
        assertTrue(include.contains("sizes=\"any\""));
        assertTrue(include.contains("?v=${initParam.frog2AssetVersion}"));
        assertEquals(1, occurrences(include, "rel=\"icon\""));
        assertFalse(include.contains("/favicon.png"));
        assertFalse(include.contains("rel=\"apple-touch-icon\""));
        assertFalse(include.contains("rel=\"shortcut icon\""));

        for (String path : List.of(
                "includes/header.jsp",
                "login.jsp",
                "error/400.jsp",
                "error/403.jsp",
                "error/404.jsp",
                "error/405.jsp",
                "error/409.jsp",
                "error/500.jsp",
                "error/503.jsp")) {
            String jsp = Files.readString(WEBAPP.resolve(path));
            assertEquals(1, occurrences(
                    jsp,
                    "include file=\"/WEB-INF/includes/favicon.jspf\""), path);
            assertFalse(jsp.contains("rel=\"icon\" href=\""), path);
        }
    }

    private static int occurrences(String value, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
