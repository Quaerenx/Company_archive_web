package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommonUiReuseContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void flashMessagesUseOneRequestScopedEscapedTag() throws Exception {
        String tag = read("WEB-INF/tags/flashMessages.tag");
        assertTrue(tag.contains("<c:out value=\"${requestScope.message}\""));
        assertTrue(tag.contains("requestScope.messageType"));
        assertFalse(tag.contains("sessionScope.message"));
        assertFalse(tag.contains("sessionScope.error"));
        assertFalse(tag.contains("<c:remove"));
        assertTrue(tag.contains("ui-alert--${flashTone}"));

        for (String page : List.of(
                "customers/customers_list.jsp",
                "customers/customers_detail.jsp",
                "customers/customers_detail_edit.jsp",
                "maintenance/maintenance_cards.jsp",
                "maintenance/maintenance_history.jsp",
                "meeting/meeting_list.jsp",
                "meeting/meeting_view.jsp",
                "troubleshooting/troubleshooting_list.jsp",
                "troubleshooting/troubleshooting_view.jsp")) {
            String source = read(page);
            assertTrue(source.contains("<t:flashMessages />"), page);
            assertFalse(source.contains("sessionScope.message"), page);
            assertFalse(source.contains("sessionScope.error"), page);
        }
    }

    @Test
    void customerCreateAndEditShareOneFieldFragment() throws Exception {
        String fields = read("WEB-INF/includes/_customer_form_fields.jspf");
        String add = read("customers/customers_add.jsp");
        String edit = read("customers/customers_edit.jsp");

        for (String field : List.of(
                "customer_name",
                "first_introduction_year",
                "db_name",
                "vertica_version",
                "license_size",
                "manager_name",
                "customer_type",
                "note")) {
            assertTrue(fields.contains("name=\"" + field + "\""), field);
            assertFalse(add.contains("name=\"" + field + "\""), field);
            assertFalse(edit.contains("name=\"" + field + "\""), field);
        }
        assertTrue(fields.contains("customerFormMode eq 'edit'"));
        assertTrue(add.contains("customerFormMode\" value=\"add\""));
        assertTrue(edit.contains("customerFormMode\" value=\"edit\""));
        assertTrue(add.contains("_customer_form_fields.jspf"));
        assertTrue(edit.contains("_customer_form_fields.jspf"));
        assertFalse(fields.contains("max=\"2030\""));
        assertFalse(read("customers/customers_detail_edit.jsp")
                .contains("max=\"2030\""));
    }

    @Test
    void longCustomerAndTroubleshootingFormsUseTheSharedDirtyGuard()
            throws Exception {
        for (String page : List.of(
                "customers/customers_add.jsp",
                "customers/customers_edit.jsp",
                "troubleshooting/troubleshooting_add.jsp",
                "troubleshooting/troubleshooting_edit.jsp")) {
            assertTrue(read(page).contains(
                    "data-ui-dirty-guard=\"auto\""), page);
        }

        String behavior = read("resources/js/ui-system.js");
        assertTrue(behavior.contains("function createDirtyGuard(form)"));
        assertTrue(behavior.contains("window.addEventListener('beforeunload'"));
    }

    @Test
    void tableViewportBehaviorLivesInItsOwnSharedModule() throws Exception {
        String footer = read("includes/footer.jsp");
        String coreStyles = read("WEB-INF/includes/core_styles.jspf");
        String core = read("resources/js/ui-system.js");
        String tables = read("resources/js/ui-table.js");

        assertTrue(coreStyles.indexOf("resources/css/ui-system.css")
                < coreStyles.indexOf("resources/css/ui-table.css"));
        assertTrue(footer.indexOf("resources/js/ui-system.js")
                < footer.indexOf("resources/js/ui-table.js"));
        assertFalse(core.contains("function updateScrollableTableRegions()"));
        assertTrue(tables.contains("function updateScrollableTableRegions()"));
        assertTrue(tables.contains("function updateTableStickyOffset()"));
    }

    @Test
    void fullFormFootersExposeTheCanonicalActionClass() throws Exception {
        for (String page : List.of(
                "customers/customers_add.jsp",
                "customers/customers_edit.jsp",
                "customers/customers_detail_edit.jsp",
                "maintenance/maintenance_add.jsp",
                "maintenance/maintenance_edit.jsp",
                "meeting/meeting_write.jsp",
                "meeting/meeting_edit.jsp",
                "troubleshooting/troubleshooting_add.jsp",
                "troubleshooting/troubleshooting_edit.jsp",
                "mypage/change_password.jsp",
                "mypage/edit_profile.jsp",
                "mypage/monthly_customer_response.jsp")) {
            assertTrue(read(page).contains("ui-form-actions"), page);
        }

        String styles = read("resources/css/ui-system.css");
        assertTrue(styles.contains(".ui-system .ui-form-actions"));
        assertTrue(styles.contains(".ui-form-layout .ui-form-actions"));
        assertTrue(styles.contains(".ui-form-layout .button-group"));
    }

    @Test
    void canonicalComponentsDoNotCarryLegacyVisualAliases() throws Exception {
        for (String page : List.of(
                "customers/customers_add.jsp",
                "customers/customers_edit.jsp",
                "maintenance/maintenance_add.jsp",
                "maintenance/maintenance_edit.jsp",
                "meeting/meeting_write.jsp",
                "meeting/meeting_edit.jsp",
                "troubleshooting/troubleshooting_add.jsp",
                "troubleshooting/troubleshooting_edit.jsp")) {
            String source = read(page);
            assertFalse(source.contains("alert alert-danger"), page);
            assertFalse(source.contains("form-container ui-form-card"), page);
            assertTrue(source.contains("ui-form-card"), page);
        }

        for (String page : List.of(
                "customers/customers_list.jsp",
                "meeting/meeting_list.jsp",
                "troubleshooting/troubleshooting_list.jsp",
                "WEB-INF/views/filerepo/list.jsp",
                "mypage/monthly_customer_response.jsp")) {
            String source = read(page);
            assertFalse(source.contains("table-wrapper ui-table-wrap"), page);
            assertFalse(source.contains("table-responsive ui-table-wrap"), page);
        }
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
