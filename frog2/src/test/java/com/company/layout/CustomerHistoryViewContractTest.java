package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerHistoryViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void standaloneHistoryPageContainsOnlyCuratedWorkCategories() throws Exception {
        String list = read("customer-history/customer_history_list.jsp");
        String form = read("customer-history/customer_history_form.jsp");

        assertTrue(list.contains("주요 장애, 업그레이드, 증설"));
        assertTrue(list.contains("<t:tableFooter"));
        assertTrue(list.contains("scope=\"col\""));
        assertTrue(list.contains("/customer-history?view=add"));
        assertTrue(list.contains("<span class=\"sr-only\">고객사</span>"));
        assertTrue(list.contains("customer-history-search-button"));
        assertFalse(list.contains("class=\"col--author\""));
        assertFalse(list.contains(">작성자</th>"));
        assertTrue(form.contains("name=\"category\""));
        assertTrue(form.contains("name=\"actionSummary\""));
        assertTrue(form.contains("csrf_input.jspf"));
        assertFalse(list.contains("/maintenance"));
        assertFalse(list.contains("/troubleshooting"));
    }

    @Test
    void historyIsASeparateRouteAndNavigationEntry() throws Exception {
        String webXml = read("WEB-INF/web.xml");
        String navigation = read("WEB-INF/includes/header_nav.jspf");
        String detail = read("customers/customers_detail.jsp");

        assertTrue(webXml.contains("<url-pattern>/customer-history</url-pattern>"));
        assertTrue(navigation.contains("navCustomerHistoryCurrent"));
        assertTrue(navigation.contains("고객사 히스토리"));
        assertFalse(detail.contains("customerHistoryUrl"));
    }

    @Test
    void historyStylesUseExistingDesignTokens() throws Exception {
        String styles = read("resources/css/pages/customer_history.css");

        assertTrue(styles.contains("var(--color-"));
        assertTrue(styles.contains("var(--space-"));
        assertTrue(styles.contains("body.page-customer-history .customer-history-filter-form"));
        assertTrue(styles.contains("table-layout: fixed"));
        assertTrue(styles.contains(":is(th, td).col--date"));
        assertTrue(styles.contains(":is(th, td).col--customer"));
        assertTrue(styles.contains(":is(th, td).col--type"));
        assertFalse(styles.matches("(?s).*(#[0-9a-fA-F]{3,8}|rgb\\().*"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(WEBAPP.resolve(relative));
    }
}
