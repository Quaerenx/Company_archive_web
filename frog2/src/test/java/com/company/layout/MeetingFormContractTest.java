package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MeetingFormContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final String FORM_FIELDS_INCLUDE =
            "<%@ include file=\"/WEB-INF/includes/_meeting_form_fields.jspf\" %>";

    @Test
    void writeAndEditFormsExposeTheSameCoreContract() throws Exception {
        String writePage = read("meeting/meeting_write.jsp");
        String editPage = read("meeting/meeting_edit.jsp");
        String formFields = read("WEB-INF/includes/_meeting_form_fields.jspf");

        for (String page : new String[] {writePage, editPage}) {
            assertTrue(page.contains("id=\"meetingForm\""));
            assertTrue(page.contains("id=\"previewModal\""));
            assertEquals(1, occurrences(page, FORM_FIELDS_INCLUDE));
        }
        for (String fieldId : new String[] {
                "title", "meeting_type", "meeting_datetime", "content"
        }) {
            assertTrue(formFields.contains("id=\"" + fieldId + "\""), fieldId);
            assertTrue(formFields.contains("name=\"" + fieldId + "\""), fieldId);
        }
        assertFalse(formFields.contains("csrf_input.jspf"));
        assertFalse(formFields.contains("name=\"action\""));
        assertFalse(formFields.contains("name=\"meeting_id\""));
        assertFalse(formFields.contains("id=\"deleteForm\""));
        assertFalse(formFields.contains("id=\"previewModal\""));

        assertTrue(writePage.contains("name=\"action\" value=\"write\""));
        assertTrue(writePage.contains("data-meeting-mode=\"write\""));
        assertTrue(editPage.contains("name=\"action\" value=\"update\""));
        assertTrue(editPage.contains("name=\"meeting_id\""));
        assertTrue(editPage.contains("data-meeting-mode=\"edit\""));

        String writeBehavior = behaviorSource(writePage);
        String editBehavior = behaviorSource(editPage);
        for (String behavior : new String[] {writeBehavior, editBehavior}) {
            assertTrue(behavior.contains("회의 제목을 입력해주세요."));
            assertTrue(behavior.contains("회의 유형을 선택해주세요."));
            assertTrue(behavior.contains("회의 일시를 선택해주세요."));
            assertTrue(behavior.contains("회의 내용을 입력해주세요."));
            assertTrue(behavior.contains("'daily': '일일 회의'"));
            assertTrue(behavior.contains("'emergency': '긴급 회의'"));
            assertTrue(behavior.contains("beforeunload"));
            assertTrue(behavior.contains("insertTemplate"));
            assertTrue(behavior.contains("updateTitleSuggestion"));
            assertTrue(behavior.contains("updateContentCount"));
            assertFalse(behavior.contains("회의록을 등록하시겠습니까?"));
            assertFalse(behavior.contains("회의록을 수정하시겠습니까?"));
        }

        assertTrue(writeBehavior.contains("getFullYear"));
        assertTrue(editBehavior.contains("정말로 이 회의록을 삭제하시겠습니까?"));
        assertTrue(editBehavior.contains("originalData"));
        assertTrue(editPage.contains("id=\"deleteForm\""));
        assertTrue(formFields.contains("data-meeting-action=\"insert-template\""));
        assertTrue(formFields.contains("id=\"contentCount\""));
        assertFalse(formFields.contains("공간 확보용"));
    }

    private static String behaviorSource(String page) throws Exception {
        if (page.contains("/resources/js/pages/meeting_form.js")) {
            return read("resources/js/pages/meeting_form.js");
        }

        int start = page.indexOf("<script>");
        int end = page.indexOf("</script>", start);
        return page.substring(start, end);
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }

    private static int occurrences(String source, String value) {
        return source.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
