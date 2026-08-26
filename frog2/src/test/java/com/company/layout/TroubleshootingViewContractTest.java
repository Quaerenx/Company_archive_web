package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TroubleshootingViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final String FORM_FIELDS_INCLUDE =
            "<%@ include file=\"/WEB-INF/includes/_troubleshooting_form_fields.jspf\" %>";

    @Test
    void addAndEditFormsKeepTheirFieldsValidationAndVisualContract() throws Exception {
        String add = read("troubleshooting/troubleshooting_add.jsp");
        String edit = read("troubleshooting/troubleshooting_edit.jsp");
        String formFields = read(
                "WEB-INF/includes/_troubleshooting_form_fields.jspf");

        assertFormContract(add, formFields, "add");
        assertFormContract(edit, formFields, "update");
        assertTrue(edit.contains("name=\"id\""));
        assertTrue(formFields.contains("troubleshooting.creator"));
        assertFalse(formFields.contains("csrf_input.jspf"));
        assertFalse(formFields.contains("name=\"action\""));
        assertFalse(formFields.contains("name=\"id\""));
        assertFalse(formFields.contains("button-group"));
        assertTrue(add.contains(
                "data-troubleshooting-form-mode=\"add\""));
        assertTrue(edit.contains(
                "data-troubleshooting-form-mode=\"edit\""));

        String addBehavior = behavior(
                add, "resources/js/pages/troubleshooting_form.js");
        String editBehavior = behavior(
                edit, "resources/js/pages/troubleshooting_form.js");
        assertTrue(addBehavior.contains("occurrence_date"));
        assertTrue(addBehavior.contains("제목은 필수 입력 항목입니다."));
        assertTrue(addBehavior.contains("고객사는 필수 선택 항목입니다."));
        assertTrue(editBehavior.contains("제목은 필수 입력 항목입니다."));
        assertTrue(editBehavior.contains("고객사는 필수 선택 항목입니다."));
        assertTrue(editBehavior.contains("트러블 슈팅 정보를 수정하시겠습니까?"));
        assertFalse(addBehavior.contains("toISOString()"));
        assertTrue(addBehavior.contains("getFullYear()"));
        assertTrue(addBehavior.contains("getMonth() + 1"));
        assertTrue(addBehavior.contains("getDate()"));

        String addStyles = styles(
                add, "resources/css/pages/troubleshooting_form.css");
        String editStyles = styles(
                edit, "resources/css/pages/troubleshooting_form.css");
        String sharedStyles = read("resources/css/ui-system.css");
        assertFormVisualContract(addStyles, sharedStyles);
        assertFormVisualContract(editStyles, sharedStyles);
    }

    @Test
    void listKeepsSearchAndRowNavigationContract() throws Exception {
        String page = read("troubleshooting/troubleshooting_list.jsp");
        assertTrue(page.contains("<t:pageHeader>"));
        assertTrue(page.contains("method=\"get\""));
        assertTrue(page.contains("name=\"view\" value=\"list\""));
        assertTrue(page.contains("name=\"q\""));
        assertTrue(page.contains("name=\"scope\""));
        assertTrue(page.contains("본문 포함"));
        assertTrue(page.contains("minlength=\"2\""));
        assertTrue(page.contains("maxlength=\"100\""));
        assertTrue(page.contains("returnScope"));
        assertTrue(page.contains("pageSize"));
        assertTrue(page.contains("name=\"q\" value=\"${q}\""));
        assertTrue(page.contains("name=\"scope\" value=\"content\""));
        assertTrue(page.contains("name=\"page\" value=\"${currentPage - 1}\""));
        assertTrue(page.contains("name=\"page\" value=\"${currentPage + 1}\""));
        assertTrue(page.contains("name=\"returnQ\" value=\"${q}\""));
        assertTrue(page.contains("name=\"returnPage\" value=\"${currentPage}\""));
        assertTrue(page.contains("name=\"returnPageSize\" value=\"${pageSize}\""));
        assertTrue(page.contains("<t:tableFooter"));
        assertTrue(page.contains("troubleshootingPreviousPageUrl"));
        assertTrue(page.contains("troubleshootingNextPageUrl"));
        int rowsBranch = page.indexOf(
                "<c:when test=\"${not empty troubleshootingList}\">");
        int footer = page.indexOf("<t:tableFooter", rowsBranch);
        int emptyState = page.indexOf(
                "troubleshooting-empty ui-empty-state", rowsBranch);
        int rowsBranchEnd = page.lastIndexOf("</c:when>", emptyState);
        assertTrue(rowsBranch >= 0);
        assertTrue(footer > rowsBranch);
        assertTrue(rowsBranchEnd > footer);
        assertTrue(emptyState > rowsBranchEnd);
        assertFalse(page.contains("ui-pagination"));
        assertFalse(page.contains("aria-current=\"page\""));
        assertTrue(page.contains("data-detail-url="));
        assertTrue(page.contains("class=\"title-link\""));
        assertTrue(page.contains("troubleshootingList"));

        String behavior = read("resources/js/ui-system.js");
        assertTrue(page.contains("ui-data-row"));
        assertTrue(behavior.contains("dataset.detailUrl"));
        assertTrue(behavior.contains("interactiveTarget"));

        String styles = styles(
                page, "resources/css/pages/troubleshooting_list.css");
        String sharedStyles = read("resources/css/ui-system.css");
        assertTrue(sharedStyles.contains("max-width: var(--page-content-max-width)"));
        assertFalse(styles.contains("max-width: var(--page-content-max-width)"));
        assertFalse(styles.contains("min-height: 800px"));
        assertTrue(styles.contains("@media (max-width: 768px)"));
    }

    @Test
    void detailKeepsContentActionsAndDeleteConfirmationContract() throws Exception {
        String page = read("troubleshooting/troubleshooting_view.jsp");
        assertTrue(page.contains("<t:pageHeader>"));
        assertTrue(page.contains("deleteTroubleshootingButton"));
        assertTrue(page.contains(
                "<c:if test=\"${canManageTroubleshooting}\">"));
        assertTrue(page.contains("troubleshooting.overview"));
        assertTrue(page.contains("troubleshooting.causeAnalysis"));
        assertTrue(page.contains("troubleshooting.errorContent"));
        assertTrue(page.contains("troubleshooting.actionTaken"));
        assertTrue(page.contains("troubleshooting.scriptContent"));
        assertTrue(page.contains("troubleshooting.note"));
        assertEquals(1, occurrences(page, "${troubleshooting.title}"));
        assertTrue(page.contains("class=\"troubleshooting-meta-grid\""));
        assertTrue(page.contains("<dl class=\"troubleshooting-meta-grid\">"));
        assertTrue(page.contains("class=\"troubleshooting-report-text\""));
        assertTrue(page.contains("class=\"troubleshooting-code-block\""));
        assertTrue(page.contains("data-copy-target=\"troubleshooting-script-content\""));
        assertTrue(page.contains("tabindex=\"0\" aria-labelledby=\"troubleshooting-script-title\""));
        assertTrue(page.indexOf("troubleshooting-overview-title")
                < page.indexOf("troubleshooting-error-title"));
        assertTrue(page.indexOf("troubleshooting-error-title")
                < page.indexOf("troubleshooting-cause-title"));
        assertTrue(page.indexOf("troubleshooting-cause-title")
                < page.indexOf("troubleshooting-action-title"));
        assertTrue(page.indexOf("troubleshooting-action-title")
                < page.indexOf("troubleshooting-script-title"));
        assertTrue(page.indexOf("troubleshooting-script-title")
                < page.indexOf("troubleshooting-note-title"));
        assertFalse(page.contains("작성된 내용이 없습니다."));

        String behavior = behavior(
                page, "resources/js/pages/troubleshooting_view.js");
        assertTrue(behavior.contains("정말로 이 트러블 슈팅을 삭제하시겠습니까?"));
        assertTrue(behavior.contains("navigator.clipboard.writeText(text)"));
        assertTrue(behavior.contains("document.execCommand('copy')"));
        assertTrue(behavior.contains("스크립트를 복사했습니다."));
        assertTrue(page.contains("csrf_input.jspf")
                || behavior.contains("Frog2Csrf.appendTo(form)"));
        assertTrue(page.contains("value=\"delete\"")
                || behavior.contains("value = 'delete'"));

        String styles = styles(
                page, "resources/css/pages/troubleshooting_view.css");
        String sharedStyles = read("resources/css/ui-system.css");
        assertTrue(sharedStyles.contains("max-width: var(--page-content-max-width)"));
        assertFalse(styles.contains("max-width: var(--page-content-max-width)"));
        assertTrue(styles.contains(
                "grid-template-columns: repeat(3, minmax(0, 1fr))"));
        assertTrue(styles.contains("font-family: inherit"));
        assertTrue(styles.contains("font-family: var(--font-mono)"));
        assertTrue(styles.contains("white-space: pre-wrap"));
        assertTrue(styles.contains("white-space: pre"));
        assertFalse(styles.contains(".detail-item.full-width"));
        assertTrue(page.contains("button--ghost"));
        assertTrue(page.contains("button--ghost-danger"));
        assertFalse(styles.contains("min-width: 120px"));
        assertTrue(styles.contains("@media (max-width: 768px)"));
    }

    private static void assertFormContract(
            String page, String formFields, String action) {
        assertTrue(page.contains("method=\"post\""));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(page.contains("name=\"action\" value=\"" + action + "\""));
        assertEquals(1, occurrences(page, FORM_FIELDS_INCLUDE));
        for (String field : new String[] {
                "title", "customer_name", "occurrence_date", "customer_manager",
                "work_period", "work_personnel", "support_type", "case_open_yn",
                "overview", "cause_analysis", "error_content", "action_taken",
                "script_content", "note"
        }) {
            assertEquals(
                    1,
                    occurrences(formFields, "name=\"" + field + "\""),
                    field);
        }
    }

    private static void assertFormVisualContract(
            String styles, String sharedStyles) {
        assertTrue(sharedStyles.contains("max-width: var(--page-content-max-width)"));
        assertTrue(sharedStyles.contains("padding-block: var(--space-32)"));
        assertFalse(styles.contains("max-width: var(--page-content-max-width)"));
        assertTrue(sharedStyles.contains("padding: var(--space-24)"));
        assertTrue(sharedStyles.contains(
                "border-block-end: 1px solid var(--color-border)"));
        assertTrue(styles.contains(".troubleshooting-form-page .ui-form textarea"));
        assertTrue(sharedStyles.contains("@media (max-width: 768px)"));
    }

    private static String behavior(String page, String path) throws Exception {
        return page.contains("/" + path) ? read(path) : page;
    }

    private static String styles(String page, String path) throws Exception {
        return page.contains("/" + path) ? read(path) : page;
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }

    private static int occurrences(String source, String value) {
        return source.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
