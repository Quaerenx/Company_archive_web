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

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
