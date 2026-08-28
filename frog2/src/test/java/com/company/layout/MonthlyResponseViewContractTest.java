package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MonthlyResponseViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void responseFormsBehaviorAndVisualConstantsRemainStable() throws Exception {
        String page = read("mypage/monthly_customer_response.jsp");
        assertTrue(page.contains("id=\"filterForm\""));
        assertTrue(page.contains("name=\"action\" value=\"monthlyResponse\""));
        assertTrue(page.contains("id=\"year\" name=\"year\""));
        assertTrue(page.contains("id=\"month\" name=\"month\""));
        assertTrue(page.contains("id=\"responseForm\""));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(page.contains("name=\"formAction\" id=\"formAction\" value=\"addResponse\""));
        assertTrue(page.contains("name=\"responseId\" id=\"responseId\""));
        assertTrue(page.contains("data-response-id="));
        assertTrue(page.contains(
                "class=\"monthly-response-table ui-table ui-data-table\""));
        assertTrue(page.contains("class=\"col--author\" data-label=\"담당자\""));
        assertTrue(page.contains("responseEntry.userName"));
        for (String label : new String[] {
                "날짜", "고객사", "담당자", "사유", "조치 내용", "비고", "작업"
        }) {
            assertTrue(page.contains("data-label=\"" + label + "\""), label);
        }
        assertTrue(page.contains("class=\"response-date\""));
        assertTrue(page.contains("class=\"response-customer-name\""));
        assertTrue(page.contains("class=\"response-reason\""));
        assertTrue(page.contains("class=\"response-action-content\" hidden"));
        assertTrue(page.contains("class=\"response-note\" hidden"));
        assertTrue(page.contains(
                "${responseDateValue} ${responseEntry.customerName} 응대 기록 수정"));
        assertTrue(page.contains(
                "${responseDateValue} ${responseEntry.customerName} 응대 기록 삭제"));

        String behavior = page.contains("/resources/js/pages/monthly_customer_response.js")
                ? read("resources/js/pages/monthly_customer_response.js")
                : page;
        assertTrue(behavior.contains("addResponse"));
        assertTrue(behavior.contains("updateResponse"));
        assertTrue(behavior.contains("deleteResponse"));
        assertTrue(behavior.contains("Frog2Csrf.appendTo(form)"));
        assertTrue(behavior.contains("정말로 이 응대 기록을 삭제하시겠습니까?"));
        assertTrue(behavior.contains("getDefaultResponseDate"));
        assertTrue(behavior.contains("filterForm"));

        String styles = page.contains("/resources/css/pages/monthly_customer_response.css")
                ? read("resources/css/pages/monthly_customer_response.css")
                : page;
        String sharedStyles = read("resources/css/ui-system.css");
        assertTrue(sharedStyles.contains("max-width: var(--page-content-max-width)"));
        assertTrue(sharedStyles.contains("padding-block: var(--space-32)"));
        assertFalse(styles.contains("max-width: var(--page-content-max-width)"));
        assertTrue(styles.contains("background-color: var(--color-surface-muted)"));
        assertTrue(styles.contains(
                "border: 1px solid var(--color-surface-edge)"));
        assertTrue(styles.contains("box-shadow: var(--shadow-sm)"));
        assertFalse(styles.contains("translateY("));
        assertTrue(styles.contains("@media (max-width: 768px)"));
        assertTrue(styles.contains("overflow-x: visible"));
        assertTrue(styles.contains("content: attr(data-label)"));
        assertFalse(styles.contains("nth-child"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
