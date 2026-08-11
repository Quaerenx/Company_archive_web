package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerDetailViewStructureTest {
    @Test
    void allEnvironmentsUseTheSameEncodedDetailFragment() throws Exception {
        Path customers = Path.of("src/main/webapp/customers");
        String page = Files.readString(customers.resolve("customers_detail.jsp"));
        String fragment = Files.readString(customers.resolve("_detail_sections.jspf"));

        assertEquals(3, occurrences(page, "<%@ include file=\"/customers/_detail_sections.jspf\" %>"));
        assertFalse(page.contains("customerDetail.systemName"));
        assertTrue(fragment.contains(
                "<t:detailField label=\"시스템명\" value=\""
                        + "$" + "{detail.systemName}\" />"));
        assertTrue(fragment.contains(
                "<fmt:formatDate var=\"detailEosDate\" value=\""
                        + "$" + "{detail.eosDate}\""));
        assertEquals(49, occurrences(fragment, "<t:detailField "));
        assertEquals(4, occurrences(
                fragment,
                "<details class=\"detail-section detail-section--collapsible\">"));
        assertTrue(fragment.contains("핵심 정보"));
        assertTrue(fragment.contains("기본·담당자 정보"));
        assertTrue(fragment.contains("인프라·네트워크"));
    }

    @Test
    void activeCustomerOwnsActionsAndExplicitEnvironmentOwnsInitialTab() throws Exception {
        Path webapp = Path.of("src/main/webapp");
        String page = Files.readString(webapp.resolve("customers/customers_detail.jsp"));
        String script = Files.readString(
                webapp.resolve("resources/js/pages/customer_detail.js"));

        assertTrue(page.contains("<c:if test=\"" + "$" + "{not empty customer}\">"));
        assertTrue(script.contains(
                "var envParam = (params.get('env') || '').trim().toLowerCase()"));
        assertTrue(script.contains("var hasRequestedEnvironment = envParam === 'prod'"));
        assertTrue(script.contains("if (!hasRequestedEnvironment && prodEmpty)"));
    }

    @Test
    void readOnlyDetailUsesAResponsiveTableLikeHierarchy() throws Exception {
        Path webapp = Path.of("src/main/webapp");
        String page = Files.readString(
                webapp.resolve("customers/customers_detail.jsp"));
        String styles = Files.readString(
                webapp.resolve("resources/css/pages/customer_detail.css"));
        String fieldTag = Files.readString(
                webapp.resolve("WEB-INF/tags/detailField.tag"));

        assertTrue(page.contains("customer-detail--view"));
        assertTrue(styles.contains(
                "grid-template-columns: repeat(2, minmax(0, 1fr));"));
        assertTrue(styles.contains(
                "grid-template-columns: minmax(104px, 120px) minmax(0, 1fr);"));
        assertTrue(styles.contains("details[open] > .detail-section-title"));
        assertTrue(styles.contains("@media (max-width: 768px)"));
        assertTrue(fieldTag.contains("detail-value--empty\">미등록</span>"));
        assertTrue(fieldTag.contains("<c:out value=\"${value}\" />"));
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
