package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MeetingCssSplitContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path CSS = WEBAPP.resolve("resources/css/pages");
    private static final List<String> PAGE_CHUNKS = List.of(
            "/resources/css/pages/meeting_view.css",
            "/resources/css/pages/meeting_list_layout.css",
            "/resources/css/pages/meeting_form.css");

    @Test
    void splitFilesKeepPageResponsibilitiesAndUseSharedUiTokens() throws Exception {
        String shared = readCss("meeting.css");
        String view = readCss("meeting_view.css");
        String list = readCss("meeting_list_layout.css");
        String form = readCss("meeting_form.css");

        assertTrue(shared.contains("/* 회의 관리 페이지 전용 스타일 */"));
        assertTrue(view.startsWith("/* meeting_view.jsp 전용 스타일 */"));
        assertTrue(list.startsWith("/* meeting_list.jsp 전용 스타일 */"));
        assertTrue(form.startsWith("/* meeting_write.jsp 전용 스타일 */"));
        assertTrue(form.contains(".meeting-page-container .ui-form textarea"));
        String uiSystem = Files.readString(
                WEBAPP.resolve("resources/css/ui-system.css"));
        assertTrue(uiSystem.contains(".ui-form-card"));
        assertTrue(uiSystem.contains(".ui-form-layout .section-title"));
        assertTrue(uiSystem.contains(".ui-form-layout .form-row"));
        assertTrue(uiSystem.contains(".ui-form-layout .button-group"));
        assertFalse(form.contains("#007bff"));
        assertFalse(form.contains(".form-group input"));
        assertFalse(form.contains(".alert-danger"));
    }

    @Test
    void eachMeetingPageLoadsOnlyItsChunkInTheCanonicalPageStyleSlot() throws Exception {
        Map<String, String> expectedStyles = new LinkedHashMap<>();
        expectedStyles.put(
                "meeting/meeting_list.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_list_layout.css,"
                        + "/resources/css/pages/meeting_list.css");
        expectedStyles.put(
                "meeting/meeting_view.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_view.css");
        expectedStyles.put(
                "meeting/meeting_write.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_form.css");
        expectedStyles.put(
                "meeting/meeting_edit.jsp",
                "/resources/css/pages/meeting.css,"
                        + "/resources/css/pages/meeting_form.css");

        for (Map.Entry<String, String> entry : expectedStyles.entrySet()) {
            String page = Files.readString(WEBAPP.resolve(entry.getKey()));
            String expectedDeclaration =
                    "<c:set var=\"pageCss\" value=\""
                            + entry.getValue()
                            + "\" scope=\"request\" />";

            assertTrue(page.contains(expectedDeclaration), entry.getKey());
            assertEquals(1, occurrences(page, "/resources/css/pages/meeting.css"),
                    entry.getKey());
            String selectedChunk = null;
            for (String chunk : PAGE_CHUNKS) {
                assertEquals(entry.getValue().contains(chunk), page.contains(chunk),
                        entry.getKey() + ": " + chunk);
                if (entry.getValue().contains(chunk)) {
                    selectedChunk = chunk;
                }
            }
            assertNotNull(selectedChunk, entry.getKey());
            assertTrue(entry.getValue().indexOf("/resources/css/pages/meeting.css")
                            < entry.getValue().indexOf(selectedChunk),
                    entry.getKey());
            assertFalse(entry.getValue().contains(
                    "/resources/css/pages/customers.css"), entry.getKey());
            assertTrue(page.contains("content-management"), entry.getKey());
        }
    }

    private static String readCss(String fileName) throws Exception {
        Path file = CSS.resolve(fileName);
        assertTrue(Files.isRegularFile(file), file.toString());
        return Files.readString(file);
    }

    private static int occurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
