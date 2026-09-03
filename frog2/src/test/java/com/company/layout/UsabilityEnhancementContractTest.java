package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class UsabilityEnhancementContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void selectedLongFormsOptIntoScopedDraftRecovery() throws Exception {
        String footer = read("includes/footer.jsp");
        String script = read("resources/js/form-drafts.js");
        assertTrue(footer.contains("/resources/js/form-drafts.js"));
        assertTrue(script.contains("24 * 60 * 60 * 1000"));
        assertTrue(script.contains("type !== 'password'"));
        assertTrue(script.contains("type !== 'file'"));
        for (String path : new String[] {
                "customers/customers_detail_edit.jsp",
                "troubleshooting/troubleshooting_add.jsp",
                "troubleshooting/troubleshooting_edit.jsp",
                "meeting/meeting_write.jsp",
                "meeting/meeting_edit.jsp",
                "customer-history/customer_history_form.jsp"
        }) {
            assertTrue(read(path).contains("data-ui-draft=\"auto\""), path);
        }
    }

    @Test
    void customerHubAndInboxExposeSourceActions() throws Exception {
        String customer = read("customers/customers_detail.jsp");
        String inbox = read("mypage/work_inbox_list.jsp");
        assertTrue(customer.contains("최근 업무"));
        assertTrue(customer.contains("점검 등록"));
        assertTrue(customer.contains("히스토리 등록"));
        assertTrue(customer.contains("트러블슈팅 작성"));
        assertTrue(inbox.contains("data-work-inbox-filter"));
        assertTrue(inbox.contains("data-defer-form"));
        assertTrue(inbox.contains("원본 정보가 보완되면"));
    }

    @Test
    void meetingListKeepsAllSearchDimensionsAndReturnState() throws Exception {
        String list = read("meeting/meeting_list.jsp");
        String view = read("meeting/meeting_view.jsp");
        for (String name : new String[] {
                "q", "type", "author", "startDate", "endDate"
        }) {
            assertTrue(list.contains("name=\"" + name + "\""), name);
        }
        assertTrue(view.contains("param.returnQ"));
        assertTrue(view.contains("param.returnStartDate"));
        assertTrue(view.contains("param.returnEndDate"));
    }

    @Test
    void dailyWorkflowEnhancementsRemainConnectedEndToEnd() throws Exception {
        String footer = read("includes/footer.jsp");
        String customerDetail = read("customers/customers_detail.jsp");
        String detailField = read("WEB-INF/tags/detailField.tag");
        String importView = read("WEB-INF/views/filerepo/import.jsp");
        String quickNav = read("WEB-INF/includes/header_nav.jspf");

        assertTrue(footer.contains("/resources/js/ui-customer-combobox.js"));
        assertTrue(footer.contains("/resources/js/list-return.js"));
        for (String path : new String[] {
                "WEB-INF/includes/maintenance_form_fields.jspf",
                "customer-history/customer_history_form.jsp",
                "customer-history/customer_history_list.jsp",
                "WEB-INF/includes/_troubleshooting_form_fields.jspf"
        }) {
            assertTrue(read(path).contains("data-ui-customer-combobox"), path);
        }
        for (String path : new String[] {
                "customers/customers_list.jsp",
                "customer-history/customer_history_list.jsp",
                "maintenance/maintenance_history.jsp",
                "meeting/meeting_list.jsp",
                "troubleshooting/troubleshooting_list.jsp"
        }) {
            assertTrue(read(path).contains("data-ui-return-row"), path);
        }

        assertTrue(customerDetail.contains("data-customer-edit-url"));
        assertTrue(customerDetail.contains("data-customer-favorite"));
        assertTrue(detailField.contains("data-detail-field-missing"));
        assertTrue(read("resources/js/pages/customer_detail_edit.js")
                .contains("params.get('focus')"));

        assertTrue(importView.contains("반입 전 확인"));
        assertTrue(importView.contains("name=\"selectedPath\""));
        assertTrue(importView.contains("실패 항목만 다시 시도"));
        assertTrue(read("resources/js/pages/file_repository_import.js")
                .contains("file.retryable"));

        assertTrue(read("customers/customers_list.jsp")
                .contains("view\" value=\"export"));
        assertTrue(read("customer-history/customer_history_list.jsp")
                .contains("view\" value=\"export"));
        assertTrue(read("maintenance/maintenance_history.jsp")
                .contains("view\" value=\"export"));
        assertTrue(read("maintenance/maintenance_history.jsp")
                .contains("returnHistoryPage"));
        assertTrue(read("maintenance/maintenance_history.jsp")
                .contains("data-ui-return-source-key"));
        assertTrue(read("maintenance/maintenance_edit.jsp")
                .contains("name=\"returnHistoryPage\""));

        assertTrue(quickNav.contains("data-favorite-customers"));
        assertTrue(quickNav.contains("data-recent-customers"));
        assertTrue(read("resources/js/header_nav.js")
                .contains("recent.favorites"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
