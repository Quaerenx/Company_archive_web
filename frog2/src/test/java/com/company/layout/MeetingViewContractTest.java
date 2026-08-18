package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class MeetingViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void commentActionsKeepTheirRequestAndMessageContract() throws Exception {
        String page = read("meeting/meeting_view.jsp");
        assertTrue(page.contains("id=\"commentForm\""));
        assertTrue(page.contains("id=\"commentContent\""));
        assertTrue(page.contains("data-comment-id="));
        assertTrue(page.contains("commentPage.hasOlder"));
        assertTrue(page.contains("commentBefore"));
        assertTrue(page.contains(
                "class=\"comments-section ui-work-surface\" id=\"comments\""));
        assertTrue(page.contains("class=\"comment-btn edit ui-button"));
        assertTrue(page.contains("class=\"comment-btn delete ui-button"));
        assertTrue(page.contains("class=\"btn-save ui-button"));
        assertTrue(page.contains("class=\"btn-cancel-edit ui-button"));
        assertTrue(page.contains("button--primary"));
        assertTrue(page.contains("button--secondary"));
        assertTrue(page.contains("button--danger"));
        assertTrue(page.contains("name=\"meeting_id\"")
                || page.contains("data-meeting-id="));

        String behavior = behaviorSource(page);
        assertTrue(behavior.contains("/comment"));
        assertTrue(behavior.contains("Frog2Csrf.token()"));
        assertTrue(hasAction(behavior, "add"));
        assertTrue(hasAction(behavior, "update"));
        assertTrue(hasAction(behavior, "delete"));
        assertTrue(behavior.contains("댓글 내용을 입력해주세요."));
        assertTrue(behavior.contains("댓글 등록 중 오류가 발생했습니다."));
        assertTrue(behavior.contains("댓글 수정 중 오류가 발생했습니다."));
        assertTrue(behavior.contains("댓글 삭제 중 오류가 발생했습니다."));
        assertTrue(behavior.contains("/meeting?view=view&id="));
        assertTrue(behavior.contains("#comments"));
        assertTrue(behavior.contains("정말로 이 댓글을 삭제하시겠습니까?"));
    }

    @Test
    void detailUsesTheDtoMeetingTypeLabelInsteadOfTheStoredCode() throws Exception {
        String page = read("meeting/meeting_view.jsp");

        assertFalse(page.contains("meeting.meetingType.toLowerCase()"));
        assertTrue(page.contains("meeting.meetingTypeLabel"));
        assertFalse(page.contains("${meeting.meetingType}"));
        assertFalse(page.contains("type-${"));
    }

    @Test
    void detailHasOneBackNavigationAndAnExplicitCommentLabel() throws Exception {
        String page = read("meeting/meeting_view.jsp");

        assertTrue(page.contains("<nav class=\"back-navigation\""));
        assertTrue(page.contains("aria-label=\"회의록 상세 이동\""));
        assertTrue(page.contains("<label for=\"commentContent\" class=\"sr-only\""));
        assertTrue(page.contains("aria-describedby=\"commentHelp\""));
        assertTrue(page.contains("최근 <c:out value=\"${comments.size()}\" />개"));
        assertFalse(page.contains("회의록 목록으로 돌아가기"));
    }

    @Test
    void meetingContentUsesTheAvailableCardWidthWithoutCentering() throws Exception {
        String styles = read("resources/css/pages/meeting_view.css");

        assertTrue(styles.contains(".meeting-text {"));
        assertTrue(styles.contains("inline-size: 100%;"));
        assertTrue(styles.contains("max-inline-size: none;"));
        assertTrue(styles.contains("margin: 0;"));
        assertFalse(styles.contains("margin-inline: auto;"));
    }

    @Test
    void commentErrorsPreserveServerMessageBeforeFallingBack() throws Exception {
        String behavior = behaviorSource(read("meeting/meeting_view.jsp"));
        int jsonParse = behavior.indexOf("response.json()");
        int statusCheck = behavior.indexOf("response.ok");

        assertTrue(jsonParse >= 0, "comment responses must parse their JSON body");
        assertTrue(statusCheck >= 0, "comment responses must inspect the HTTP status");
        assertTrue(jsonParse < statusCheck,
                "the JSON error payload must be parsed before branching on response.ok");

        String nonSuccessBlock = blockFollowing(behavior, statusCheck);
        assertTrue(nonSuccessBlock.contains("throw")
                        || nonSuccessBlock.contains("Promise.reject"),
                "non-2xx handling must reject so the shared error path is used");
        assertTrue(nonSuccessBlock.contains(".message"),
                "non-2xx handling must propagate the server payload message");
        assertTrue(nonSuccessBlock.contains("fallbackMessage"),
                "non-2xx handling must retain the operation-specific fallback");

        Matcher catchHandler = Pattern.compile(
                        "\\.catch\\s*\\(\\s*function\\s*\\(\\s*([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\)")
                .matcher(behavior);
        assertTrue(catchHandler.find(), "the shared catch handler must receive the rejection");

        String errorName = catchHandler.group(1);
        String catchBlock = blockFollowing(behavior, catchHandler.end());
        assertTrue(Pattern.compile(Pattern.quote(errorName) + "\\s*\\??\\.\\s*message")
                        .matcher(catchBlock)
                        .find(),
                "the catch handler must display the propagated server message");
        assertTrue(catchBlock.contains("fallbackMessage"),
                "network and malformed-response failures must still use the fallback");
        assertTrue(catchBlock.contains("Frog2UI.notify"),
                "comment failures must use the shared visible notification");
        assertTrue(catchBlock.contains("persistent: true"),
                "comment failures must remain visible until dismissed");
    }

    private static boolean hasAction(String source, String action) {
        return source.contains("action=" + action)
                || source.contains("'" + action + "'");
    }

    private static String behaviorSource(String page) throws Exception {
        if (page.contains("/resources/js/pages/meeting_view.js")) {
            return read("resources/js/pages/meeting_view.js");
        }
        int start = page.indexOf("<script>");
        int end = page.indexOf("</script>", start);
        return page.substring(start, end);
    }

    private static String blockFollowing(String source, int offset) {
        int start = source.indexOf('{', offset);
        if (start < 0) {
            return "";
        }
        int depth = 0;
        for (int index = start; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(start + 1, index);
            }
        }
        return "";
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
