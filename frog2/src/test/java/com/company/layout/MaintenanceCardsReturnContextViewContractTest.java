package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaintenanceCardsReturnContextViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void cardsPassTheSelectedMonthAndStatusIntoHistoryAndAddNavigation()
            throws Exception {
        String cards = page("maintenance/maintenance_cards.jsp");
        String card = page("WEB-INF/tags/maintenanceCustomerCard.tag");
        assertEquals(2, cards.split("registrationFilter=", -1).length - 1);
        assertTrue(card.contains("name=\"returnCardsMonth\" value=\"${monthLabel}\""));
        assertTrue(card.contains("name=\"returnCardsStatus\" value=\"${registrationFilter}\""));
        String addUrl = urlBlock(cards, "maintenanceAddUrl");
        assertTrue(addUrl.contains("name=\"returnCardsMonth\" value=\"${maintenanceMonthParam}\""));
        assertTrue(addUrl.contains("name=\"returnCardsStatus\" value=\"${registrationStatus}\""));
    }

    @Test
    void historyRetainsListContextThroughSearchResetPaginationAndFormLinks()
            throws Exception {
        String history = page("maintenance/maintenance_history.jsp");
        for (String variable : List.of("addHistoryUrl", "maintenanceHistoryResetUrl",
                "maintenanceEditUrl", "maintenanceHistoryPreviousUrl", "maintenanceHistoryNextUrl")) {
            assertReturnParameters(urlBlock(history, variable));
        }
        String listUrl = urlBlock(history, "maintenanceCardsReturnUrl");
        assertTrue(listUrl.contains("name=\"view\" value=\"cards\""));
        assertTrue(listUrl.contains("name=\"maintenanceMonth\" value=\"${returnCardsMonth}\""));
        assertTrue(listUrl.contains("name=\"registrationStatus\" value=\"${returnCardsStatus}\""));
        assertTrue(history.contains("href=\"<c:out value='${maintenanceCardsReturnUrl}' />\""));
        assertReturnInputs(history.substring(history.indexOf("<form class=\"history-filter-form"),
                history.indexOf("</form>", history.indexOf("<form class=\"history-filter-form"))));
        assertFalse(history.contains("${param.returnCards"));
    }

    @Test
    void addEditAndDeleteFormsUseValidatedReturnHintsForEveryExit()
            throws Exception {
        String add = page("maintenance/maintenance_add.jsp");
        String edit = page("maintenance/maintenance_edit.jsp");
        assertReturnParameters(urlBlock(add, "customerHistoryUrl"));
        for (String variable : List.of("customerHistoryUrl", "headerHistoryUrl", "cancelUrl")) {
            assertReturnParameters(urlBlock(edit, variable));
        }
        assertReturnInputs(formBlock(add, "maintenanceForm"));
        assertReturnInputs(formBlock(edit, "maintenanceForm"));
        assertReturnInputs(formBlock(edit, "deleteFormHeader"));
        assertFalse(add.contains("${param.returnCards"));
        assertFalse(edit.contains("${param.returnCards"));
    }

    @Test
    void addFlowRetainsTheFilteredHistoryPageOnCancellationAndSubmission()
            throws Exception {
        String history = page("maintenance/maintenance_history.jsp");
        String add = page("maintenance/maintenance_add.jsp");
        String addUrl = urlBlock(history, "addHistoryUrl");
        String cancelUrl = urlBlock(add, "customerHistoryUrl");
        String addForm = formBlock(add, "maintenanceForm");
        for (String field : List.of("Page", "Year", "Version", "Query")) {
            String viewValue = "Page".equals(field) ? "currentPage" : "history" + field;
            assertTrue(addUrl.contains("name=\"returnHistory" + field
                    + "\" value=\"${" + viewValue + "}\""), field);
            assertTrue(cancelUrl.contains("name=\"history" + field
                    + "\" value=\"${param.returnHistory" + field + "}\""), field);
            assertTrue(addForm.contains("name=\"returnHistory" + field
                    + "\" value=\"<c:out value='${param.returnHistory" + field + "}' />\""), field);
        }
        assertTrue(add.contains("href=\"<c:out value='${customerHistoryUrl}' />\""));
    }

    private static String page(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }

    private static String urlBlock(String source, String variable) {
        int marker = source.indexOf("var=\"" + variable + "\"");
        assertTrue(marker >= 0, variable);
        int start = source.lastIndexOf("<c:url", marker);
        int end = source.indexOf("</c:url>", marker);
        String block = source.substring(start, end);
        assertTrue(block.contains("value=\"/maintenance\""), variable);
        return block;
    }

    private static String formBlock(String source, String id) {
        int start = source.indexOf("<form id=\"" + id + "\"");
        assertTrue(start >= 0, id);
        return source.substring(start, source.indexOf("</form>", start));
    }

    private static void assertReturnParameters(String source) {
        assertTrue(source.contains("${not empty returnCardsMonth}"));
        assertTrue(source.contains("name=\"returnCardsMonth\" value=\"${returnCardsMonth}\""));
        assertTrue(source.contains("name=\"returnCardsStatus\" value=\"${returnCardsStatus}\""));
    }

    private static void assertReturnInputs(String source) {
        assertTrue(source.contains("${not empty returnCardsMonth}"));
        assertTrue(source.contains("name=\"returnCardsMonth\" value=\"<c:out value='${returnCardsMonth}' />\""));
        assertTrue(source.contains("name=\"returnCardsStatus\" value=\"<c:out value='${returnCardsStatus}' />\""));
    }
}
