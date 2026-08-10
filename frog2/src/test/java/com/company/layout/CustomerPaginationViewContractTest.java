package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CustomerPaginationViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void searchFilterSortAndPageParametersRemainConnected() throws Exception {
        String page = Files.readString(
                WEBAPP.resolve("customers/customers_list.jsp"));
        String behavior = Files.readString(
                WEBAPP.resolve(
                        "resources/js/pages/customers_list.js"));

        assertTrue(page.contains("name=\"q\""));
        assertTrue(page.contains("name=\"pageSize\""));
        assertTrue(page.contains("ui-pagination"));
        assertTrue(page.contains("aria-current=\"page\""));
        assertTrue(page.contains(
                "<c:param name=\"filter\" value=\"${filter}\" />"));
        assertTrue(page.contains(
                "<c:param name=\"sortField\" value=\"${sortField}\" />"));
        assertTrue(page.contains(
                "<c:param name=\"q\" value=\"${q}\" />"));
        assertTrue(behavior.contains("currentQuery"));
        assertTrue(behavior.contains("currentPageSize"));
        assertFalse(behavior.contains("row.classList.add('hidden')"));
        assertFalse(page.contains("data-search-text="));
    }

    @Test
    void keepsMinimalToolbarSortControlsAndViewportWidthStable() throws Exception {
        String page = Files.readString(WEBAPP.resolve("customers/customers_list.jsp"));
        String styles = Files.readString(WEBAPP.resolve("resources/css/pages/customers.css"));
        String baseStyles = Files.readString(WEBAPP.resolve("resources/css/base.css"));

        assertTrue(page.contains("class=\"table-container customer-list-panel\""));
        assertTrue(page.contains("<t:pageHeader>"));
        assertTrue(page.contains("고객사 정보"));
        assertFalse(page.contains("<jsp:attribute name=\"subtitle\">"));
        assertFalse(page.contains("전체 고객사 <strong>"));
        assertFalse(styles.contains(".customer-management > .page-header"));
        assertTrue(page.contains("class=\"customer-list-toolbar\""));
        assertTrue(page.contains("class=\"filter-btn__count\""));
        assertTrue(page.contains("class=\"customer-secondary-action\""));
        assertTrue(page.indexOf("class=\"customer-secondary-action\"")
                > page.indexOf("</table>"));
        assertFalse(page.contains("class=\"filter-info\""));
        assertFalse(page.contains("class=\"search-stats\""));
        assertFalse(page.contains("정기점검만 보기"));
        assertFalse(page.contains("전체 보기"));
        assertTrue(styles.contains(
                "body.ui-system.page-customers .customer-management "
                        + "form.search-container.ui-form "
                        + ".search-input[type=\"text\"]"));
        assertTrue(styles.contains("padding: var(--space-8) var(--space-40);"));
        assertTrue(styles.contains("max-width: 360px;"));
        assertTrue(styles.contains("flex: none;"));
        assertTrue(styles.contains("width: 100%;"));
        assertTrue(styles.contains("pointer-events: none;"));
        assertTrue(page.contains("class=\"fas fa-search search-icon\" aria-hidden=\"true\""));
        assertTrue(styles.contains("min-width: 840px;"));
        assertTrue(styles.contains("flex: 0 0 auto;"));
        assertTrue(styles.contains("min-inline-size: var(--indicator-icon-size);"));
        assertTrue(baseStyles.contains("scrollbar-gutter: stable;"));
    }
}
