package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class TableFooterViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final List<String> TABLE_FOOTER_PAGES = List.of(
            "customers/customers_list.jsp",
            "meeting/meeting_list.jsp",
            "troubleshooting/troubleshooting_list.jsp",
            "WEB-INF/views/filerepo/list.jsp",
            "mypage/monthly_customer_response.jsp",
            "WEB-INF/includes/mypage/host_manager.jspf",
            "vm_hosts/list.jsp",
            "maintenance/maintenance_history.jsp");

    @Test
    void everyDataTableUsesTheSharedCenteredCountFreeFooter() throws Exception {
        String tag = read("WEB-INF/tags/tableFooter.tag");

        assertTrue(tag.contains("class=\"ui-table-footer\""));
        assertFalse(tag.contains("class=\"ui-table-footer__count\""));
        assertFalse(tag.contains("attribute name=\"totalCount\""));
        assertTrue(tag.contains("class=\"ui-table-pagination\""));
        assertTrue(tag.contains("class=\"ui-table-pagination__position\""));
        assertTrue(tag.contains("totalPages ge 1 ? totalPages : 1"));
        assertTrue(tag.contains("aria-label=\"${paginationLabel}\""));
        assertTrue(tag.contains("aria-label=\"${previousLabel}\""));
        assertTrue(tag.contains("aria-label=\"${nextLabel}\""));
        assertTrue(tag.contains("value=\"${itemLabel} 현재 "
                + "${tableFooterCurrentPage}페이지, 전체 "
                + "${tableFooterTotalPages}페이지\""));
        assertTrue(tag.contains(
                "<span class=\"sr-only\"><c:out value=\"${positionLabel}\" /></span>"));
        assertFalse(tag.contains("<span class=\"sr-only\">현재 페이지</span>"));

        for (String pagePath : TABLE_FOOTER_PAGES) {
            String page = read(pagePath);
            assertTrue(page.contains("<t:tableFooter"), pagePath);
            assertFalse(page.contains("totalCount="), pagePath);
        }

        assertFalse(read("meeting/meeting_list.jsp").contains("pagination-container"));
        assertFalse(read("troubleshooting/troubleshooting_list.jsp").contains("ui-pagination"));
        assertFalse(read("WEB-INF/views/filerepo/list.jsp").contains("file-pagination"));
        assertFalse(read("customers/customers_list.jsp").contains("customer-table-footer"));
    }

    @Test
    void sharedFooterUsesNeutralFortyFourPixelTouchTargets() throws Exception {
        String styles = read("resources/css/ui-system.css");

        assertTrue(styles.contains(".ui-system .ui-table-footer {"));
        assertTrue(styles.contains("justify-content: center;"));
        assertFalse(styles.contains(".ui-system .ui-table-footer__count"));
        assertTrue(styles.contains("min-block-size: 48px;"));
        assertTrue(styles.contains(".ui-system .ui-table-pagination__control {"));
        assertTrue(styles.contains("block-size: var(--control-height-md);"));
        assertTrue(styles.contains("inline-size: var(--control-height-md);"));
        assertFalse(styles.contains("block-size: 32px;"));
        assertFalse(styles.contains("inline-size: 32px;"));
        assertTrue(styles.contains("background: transparent;"));
        assertTrue(styles.contains(".ui-system .ui-table-pagination__control.is-disabled {"));
        assertTrue(styles.contains("pointer-events: none;"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
