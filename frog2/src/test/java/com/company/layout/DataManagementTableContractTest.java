package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataManagementTableContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final List<String> LIST_PAGES = List.of(
            "meeting/meeting_list.jsp",
            "troubleshooting/troubleshooting_list.jsp",
            "WEB-INF/views/filerepo/list.jsp");

    @Test
    void dataManagementPagesShareOneTableFrame() throws Exception {
        for (String pagePath : LIST_PAGES) {
            String page = read(pagePath);
            assertTrue(page.contains("ui-table ui-data-table"), pagePath);
            assertTrue(page.contains("ui-table-wrap"), pagePath);
            assertTrue(page.contains("<t:tableFooter"), pagePath);

            int tableWrap = page.indexOf("ui-table-wrap");
            int tableFooter = page.indexOf("<t:tableFooter");
            assertTrue(tableWrap >= 0 && tableWrap < tableFooter, pagePath);
        }
    }

    @Test
    void toolbarsAndColumnWidthsUseSharedSemanticClasses() throws Exception {
        String meeting = read("meeting/meeting_list.jsp");
        String troubleshooting = read("troubleshooting/troubleshooting_list.jsp");
        String fileRepository = read("WEB-INF/views/filerepo/list.jsp");

        assertTrue(troubleshooting.contains("ts-search-bar ui-table-toolbar"));
        assertTrue(fileRepository.contains("file-toolbar ui-table-toolbar"));
        assertFalse(fileRepository.contains("ui-work-surface--padded"));
        assertTrue(fileRepository.indexOf("ui-table-toolbar")
                < fileRepository.indexOf("ui-table-wrap"));
        assertTrue(troubleshooting.indexOf("ui-table-toolbar")
                < troubleshooting.indexOf("ui-table-wrap"));

        for (String page : List.of(meeting, troubleshooting, fileRepository)) {
            assertTrue(page.contains("col--date"));
            assertTrue(page.contains("col--title"));
            assertFalse(page.contains(" width=\""));
        }
        assertTrue(meeting.contains("col--type"));
        assertTrue(meeting.contains("col--author"));
        assertTrue(troubleshooting.contains("col--customer"));
        assertTrue(fileRepository.contains("col--description"));
        assertTrue(fileRepository.contains("col--numeric"));
    }

    @Test
    void sharedStylesOwnTableDensityAlignmentAndColumnSizing() throws Exception {
        String shared = read("resources/css/ui-system.css");
        String meeting = read("resources/css/pages/meeting_list.css");
        String troubleshooting = read("resources/css/pages/troubleshooting_list.css");
        String fileRepository = read("resources/css/pages/download.css");

        assertTrue(shared.contains(".ui-system .ui-table-toolbar {"));
        assertTrue(shared.contains(".ui-system .ui-data-table {"));
        assertTrue(shared.contains(".ui-system .ui-data-table :is(th, td) {"));
        assertTrue(shared.contains(".ui-system .ui-data-table .col--date {"));
        assertTrue(shared.contains(".ui-system .ui-data-table .col--numeric {"));

        for (String pageCss : List.of(meeting, troubleshooting, fileRepository)) {
            assertFalse(pageCss.contains("border-collapse: collapse"));
        }
        assertFalse(meeting.contains(".meeting-list-table th {"));
        assertFalse(troubleshooting.contains(".troubleshooting-table th {"));
        assertFalse(fileRepository.contains(".file-table th {"));
        assertFalse(troubleshooting.contains("nth-child"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
