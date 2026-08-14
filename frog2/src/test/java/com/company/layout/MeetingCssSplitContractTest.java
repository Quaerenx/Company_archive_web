package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            "/resources/css/pages/meeting_list.css",
            "/resources/css/pages/meeting_form.css");

    @Test
    void pageFilesKeepResponsibilitiesAndUseSharedUiTokens() throws Exception {
        String view = readCss("meeting_view.css");
        String list = readCss("meeting_list.css");
        String form = readCss("meeting_form.css");

        assertTrue(view.startsWith("/* meeting_view.jsp 전용 스타일 */"));
        assertTrue(list.contains(".meeting-management .meeting-list-table"));
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
        assertFalse(view.contains("!important"));
        assertFalse(list.contains("!important"));
        assertFalse(form.contains("!important"));
    }

    @Test
    void eachMeetingPageLoadsOnlyItsChunkInTheCanonicalPageStyleSlot() throws Exception {
        Map<String, String> expectedStyles = new LinkedHashMap<>();
        expectedStyles.put(
                "meeting/meeting_list.jsp",
                "/resources/css/pages/meeting_list.css");
        expectedStyles.put(
                "meeting/meeting_view.jsp",
                "/resources/css/pages/meeting_view.css");
        expectedStyles.put(
                "meeting/meeting_write.jsp",
                "/resources/css/pages/meeting_form.css");
        expectedStyles.put(
                "meeting/meeting_edit.jsp",
                "/resources/css/pages/meeting_form.css");

        for (Map.Entry<String, String> entry : expectedStyles.entrySet()) {
            String page = Files.readString(WEBAPP.resolve(entry.getKey()));
            String expectedDeclaration =
                    "<c:set var=\"pageCss\" value=\""
                            + entry.getValue()
                            + "\" scope=\"request\" />";

            assertTrue(page.contains(expectedDeclaration), entry.getKey());
            for (String chunk : PAGE_CHUNKS) {
                assertEquals(entry.getValue().contains(chunk), page.contains(chunk),
                        entry.getKey() + ": " + chunk);
            }
            assertFalse(page.contains("/resources/css/pages/meeting.css"),
                    entry.getKey());
            assertFalse(page.contains(
                    "/resources/css/pages/meeting_list_layout.css"), entry.getKey());
            assertFalse(entry.getValue().contains(
                    "/resources/css/pages/customers.css"), entry.getKey());
            assertTrue(page.contains("content-management"), entry.getKey());
        }
    }

    @Test
    void activeMeetingCssDoesNotKeepVerifiedLegacySelectors()
            throws Exception {
        String view = readCss("meeting_view.css");
        String list = readCss("meeting_list.css");

        for (String selector : List.of(
                ".meeting-header {",
                ".meeting-title {",
                ".meeting-actions {",
                ".meeting-actions .btn {",
                ".btn-save {",
                ".btn-cancel-edit {")) {
            assertFalse(view.contains(selector), selector);
        }
        assertFalse(view.lines().map(String::strip)
                .anyMatch(".btn-comment {"::equals));

        for (String selector : List.of(
                ":where(.meeting-view) .comments-section {",
                ":where(.meeting-view) .comment-item {",
                ":where(.meeting-view) .comment-header {",
                ":where(.meeting-view) .comment-content {",
                ":where(.meeting-view) .comment-actions {")) {
            assertTrue(view.contains(selector), selector);
        }
        assertFalse(list.contains("tr[data-detail-url]"));
        assertTrue(list.contains(".meeting-row-meta"));
        assertTrue(list.contains("@media (max-width: 768px)"));
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
