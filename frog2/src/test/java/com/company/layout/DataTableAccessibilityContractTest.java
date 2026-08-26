package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DataTableAccessibilityContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern COLUMN_HEADER = Pattern.compile("<th\\b([^>]*)>");

    @Test
    void everyOperationalDataTableHasACaptionAndScopedColumnHeaders()
            throws Exception {
        Map<String, String> expectedCaptions = new LinkedHashMap<>();
        expectedCaptions.put("customers/customers_list.jsp", "고객사 정보 목록");
        expectedCaptions.put("meeting/meeting_list.jsp", "회의록 목록");
        expectedCaptions.put(
                "troubleshooting/troubleshooting_list.jsp", "트러블슈팅 목록");
        expectedCaptions.put(
                "mypage/monthly_customer_response.jsp", "월별 고객 응대 기록");
        expectedCaptions.put(
                "WEB-INF/views/filerepo/list.jsp", "자료실 파일 및 폴더 목록");
        expectedCaptions.put(
                "WEB-INF/includes/mypage/host_manager.jspf",
                "개인 VM 호스트 목록");
        expectedCaptions.put("vm_hosts/list.jsp", "개인 호스트 목록");

        for (Map.Entry<String, String> entry : expectedCaptions.entrySet()) {
            String page = read(entry.getKey());
            assertTrue(page.contains(
                    "<caption class=\"sr-only\">" + entry.getValue() + "</caption>"),
                    entry.getKey());

            Matcher headers = COLUMN_HEADER.matcher(page);
            int headerCount = 0;
            while (headers.find()) {
                headerCount++;
                assertTrue(headers.group(1).contains("scope=\"col\""),
                        entry.getKey() + ": " + headers.group());
            }
            assertTrue(headerCount > 0, entry.getKey());
        }
    }

    @Test
    void customerSortHeadersExposeTheActualSortState() throws Exception {
        String page = read("customers/customers_list.jsp");

        assertEquals(8, occurrences(page, "aria-sort=\"${sortField eq"));
        assertEquals(8, occurrences(page, "scope=\"col\" aria-sort="));
        assertTrue(page.contains("? 'ascending' : 'descending') : 'none'"));
    }

    @Test
    void standaloneEmptyStatesReplaceEmptyTableRowsWhereMigrated() throws Exception {
        String customers = read("customers/customers_list.jsp");
        String troubleshooting = read("troubleshooting/troubleshooting_list.jsp");
        String hosts = read("WEB-INF/includes/mypage/host_manager.jspf");
        String legacyHosts = read("vm_hosts/list.jsp");

        assertTrue(customers.contains("customer-list-empty ui-empty-state"));
        assertTrue(troubleshooting.contains("troubleshooting-empty ui-empty-state"));
        assertTrue(hosts.contains("mypage-host-empty ui-empty-state"));
        assertTrue(legacyHosts.contains("vm-host-empty ui-empty-state"));
        assertFalse(legacyHosts.contains("colspan=\"8\""));
    }

    @Test
    void structuralRowsStillSpanEveryVisibleColumn() throws Exception {
        assertTrue(read("WEB-INF/views/filerepo/list.jsp").contains("colspan=\"4\""));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }

    private static int occurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
