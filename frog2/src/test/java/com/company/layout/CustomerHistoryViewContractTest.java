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
        assertTrue(list.contains("var=\"addHistoryUrl\""));
        assertTrue(list.contains("<c:param name=\"view\" value=\"add\""));
        assertTrue(list.contains("<span class=\"sr-only\">고객사</span>"));
        assertTrue(list.contains("customer-history-search-button"));
        assertTrue(list.contains("ui-form--compact"));
        assertTrue(list.contains("customer-history-filter-actions"));
        assertTrue(list.contains("<button type=\"button\""));
        assertTrue(list.contains("data-ui-disclosure-row"));
        assertTrue(list.contains("data-ui-disclosure-toggle"));
        assertTrue(list.contains("aria-expanded=\"false\""));
        assertTrue(list.contains(
                "<c:out value='${history.title}' /> 이력 상세"));
        assertFalse(list.contains("data-customer-history-label"));
        assertFalse(list.contains("data-customer-history-row"));
        assertFalse(list.contains("data-customer-history-toggle"));
        assertTrue(list.contains("class=\"customer-history-detail-row\""));
        assertFalse(list.contains("customer-history-scroll-hint"));
        assertFalse(list.contains("fa-chevron-down"));
        String pageScript = read("resources/js/pages/customer_history.js");
        assertFalse(pageScript.contains("toggleHistoryDetail"));
        assertFalse(pageScript.contains("hasSelectedTextWithin"));
        assertTrue(pageScript.contains("data-customer-history-delete"));
        String sharedScript = read("resources/js/ui-system.js");
        assertTrue(sharedScript.contains("[data-ui-disclosure-row]"));
        assertTrue(sharedScript.contains("[data-ui-disclosure-toggle]"));
        assertTrue(sharedScript.contains("toggleDisclosure"));
        assertTrue(list.contains("data-ui-scroll-region"));
        assertTrue(list.contains(
                "data-ui-scroll-label=\"고객사 히스토리 표\""));
        assertTrue(list.contains("조건에 맞는 이력이 없습니다."));
        assertTrue(list.contains("customer-history-empty ui-empty-state"));
        assertTrue(list.contains("name=\"returnCustomerName\""));
        assertTrue(list.contains("name=\"returnCategory\""));
        assertTrue(list.contains("name=\"returnQ\""));
        assertTrue(list.contains("name=\"returnPage\""));
        assertFalse(list.contains("class=\"col--author\""));
        assertFalse(list.contains(">작성자</th>"));
        assertTrue(form.contains("name=\"category\""));
        assertTrue(form.contains("name=\"actionSummary\""));
        assertTrue(form.contains("href=\"<c:out value='${returnListUrl}' />\""));
        assertTrue(form.contains("name=\"returnCustomerName\""));
        assertTrue(form.contains("name=\"returnCategory\""));
        assertTrue(form.contains("name=\"returnQ\""));
        assertTrue(form.contains("name=\"returnPage\""));
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
        assertTrue(styles.contains("overflow-wrap: anywhere"));
        assertTrue(styles.contains("var(--control-height-md)"));
        assertTrue(styles.contains("overflow-x: clip"));
        assertTrue(styles.contains(
                "border-color: var(--color-border-strong);"));
        assertTrue(styles.contains(
                "body.page-customer-history .customer-history-table\n"
                        + "    .customer-history-summary-row > td"));
        assertTrue(styles.contains(".customer-history-summary-row > td"));
        assertTrue(styles.contains(
                ".customer-history-summary-row > td:first-child::before"));
        assertTrue(styles.contains(
                ".customer-history-summary-row:hover:not(.is-expanded) > td:first-child::before"));
        assertTrue(styles.contains(
                ".customer-history-summary-row:focus-within > td:first-child::before"));
        assertTrue(styles.contains("inset-inline-start: var(--space-4);"));
        assertTrue(styles.contains(".customer-history-detail-row > td::before"));
        assertTrue(styles.contains(".customer-history-summary-row::before"));
        assertFalse(styles.contains("box-shadow: inset"));
        assertTrue(styles.contains(
                "border-block-start: 1px solid var(--color-border);"));
        assertTrue(styles.contains(".customer-history-detail-row:not([hidden])"));
        assertFalse(styles.contains("min-inline-size: 788px"));
        assertFalse(styles.contains(".customer-history-detail-toggle-icon"));
        assertFalse(styles.matches("(?s).*(#[0-9a-fA-F]{3,8}|rgb\\().*"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(WEBAPP.resolve(relative));
    }
}
