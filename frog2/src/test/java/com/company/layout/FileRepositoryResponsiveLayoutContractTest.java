package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FileRepositoryResponsiveLayoutContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void repositoryRowsExposeEveryDesktopColumnAsMobileLabelValuePairs()
            throws Exception {
        String page = read("WEB-INF/views/filerepo/list.jsp");

        for (String label : List.of("이름", "설명", "수정일", "크기")) {
            assertTrue(page.contains("data-label=\"" + label + "\""), label);
        }
        assertTrue(page.contains("<caption class=\"sr-only\">"));
        assertTrue(page.contains(
                "<span class=\"icon\" aria-hidden=\"true\"><c:out value=\"${entry.icon}\" />"));
        assertTrue(page.contains("var=\"directoryUrl\""));
        assertTrue(page.contains("var=\"downloadUrl\""));
        assertTrue(page.contains("href=\"<c:out value=\"${directoryUrl}\" />\""));
        assertTrue(page.contains("href=\"<c:out value=\"${downloadUrl}\" />\""));
    }

    @Test
    void repositoryTableBecomesNonScrollingCardsOnlyAtMobileBreakpoint()
            throws Exception {
        String css = read("resources/css/pages/download.css");
        int mobileStart = css.indexOf("@media (max-width: 768px)");

        assertTrue(mobileStart >= 0);
        String desktop = css.substring(0, mobileStart);
        String mobile = css.substring(mobileStart);

        assertFalse(desktop.contains("content: attr(data-label);"));
        assertFalse(desktop.contains(
                ".page-file-repository .file-table tbody tr {"));
        assertTrue(mobile.contains(".page-file-repository .ui-table-wrap {"));
        assertTrue(mobile.contains("overflow-x: visible;"));
        assertTrue(mobile.contains(".page-file-repository .file-table thead {"));
        assertTrue(mobile.contains("clip-path: inset(50%);"));
        assertTrue(mobile.contains(".page-file-repository .file-table tbody {"));
        assertTrue(mobile.contains(".page-file-repository .file-table tbody tr {"));
        assertTrue(mobile.contains(
                ".page-file-repository .file-table.ui-data-table td {"));
        assertTrue(mobile.contains("content: attr(data-label);"));
        assertTrue(mobile.contains("min-width: 0;"));
        assertFalse(mobile.contains("min-width: 680px;"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
