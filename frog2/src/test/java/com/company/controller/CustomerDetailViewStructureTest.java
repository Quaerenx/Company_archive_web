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
        assertTrue(fragment.contains("<c:out value=\"" + "$" + "{not empty detail.systemName"));
        assertTrue(fragment.contains("<fmt:formatDate value=\"" + "$" + "{detail.eosDate}\""));
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
