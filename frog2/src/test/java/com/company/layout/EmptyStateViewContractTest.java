package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EmptyStateViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void customerAndTroubleshootingListsHideTablesAndFootersWhenEmpty()
            throws Exception {
        String customers = read("customers/customers_list.jsp");
        assertConditionalTable(
                customers,
                "<c:when test=\"${not empty customerList}\">",
                "customer-list-empty ui-empty-state");
        assertFalse(customers.contains("<td colspan=\"8\" class=\"empty-state\">"));
        assertEmptyBranchHasNoAction(
                customers, "customer-list-empty ui-empty-state");

        String troubleshooting = read(
                "troubleshooting/troubleshooting_list.jsp");
        assertConditionalTable(
                troubleshooting,
                "<c:when test=\"${not empty troubleshootingList}\">",
                "troubleshooting-empty ui-empty-state");
        assertFalse(troubleshooting.contains(
                "<td colspan=\"4\" class=\"empty-state\">"));
        assertEmptyBranchHasNoAction(
                troubleshooting, "troubleshooting-empty ui-empty-state");
    }

    @Test
    void maintenanceAndHostFootersOnlyRenderWithRows() throws Exception {
        String maintenance = read("maintenance/maintenance_history.jsp");
        assertConditionalTable(
                maintenance,
                "<c:when test=\"${not empty historyRows}\">",
                "empty-history ui-empty-state");
        assertFalse(maintenance.contains("첫 번째 점검 이력 추가하기"));
        assertTrue(maintenance.contains("검색 조건 초기화"));
        assertTrue(maintenance.contains(
                "<i class=\"fas fa-clipboard\" aria-hidden=\"true\"></i>"));
        assertTrue(maintenance.contains("<strong>검색 결과가 없습니다</strong>"));
        assertTrue(maintenance.contains("<strong>정기점검 이력이 없습니다</strong>"));

        String hosts = read("WEB-INF/includes/mypage/host_manager.jspf");
        assertConditionalTable(
                hosts,
                "<c:when test=\"${not empty vmHosts}\">",
                "mypage-host-empty ui-empty-state");
        assertFalse(hosts.contains("<td colspan=\"7\">등록된 VM 호스트가 없습니다.</td>"));
        assertEmptyBranchHasNoAction(
                hosts, "mypage-host-empty ui-empty-state");

        String legacyHosts = read("vm_hosts/list.jsp");
        assertConditionalTable(
                legacyHosts,
                "<c:when test=\"${not empty vmHosts}\">",
                "vm-host-empty ui-empty-state");
        assertFalse(legacyHosts.contains(
                "<tr><td colspan=\"8\">등록된 VM 호스트가 없습니다.</td></tr>"));
        assertEmptyBranchHasNoAction(
                legacyHosts, "vm-host-empty ui-empty-state");
    }

    @Test
    void meetingKeepsOneHeaderActionAndAStandaloneEmptyState()
            throws Exception {
        String meeting = read("meeting/meeting_list.jsp");
        assertConditionalTable(
                meeting,
                "<c:when test=\"${not empty meetingList}\">",
                "meeting-list-empty ui-empty-state");
        assertFalse(meeting.contains("회의록 작성하기"));
        assertTrue(meeting.contains("새 회의록 작성"));
        assertTrue(meeting.contains(
                "<strong>등록된 회의록이 없습니다</strong>"));
        assertTrue(meeting.contains(
                "<span>첫 번째 회의록을 작성해보세요.</span>"));
        assertEmptyBranchHasNoAction(
                meeting, "meeting-list-empty ui-empty-state");
    }

    @Test
    void remainingDomainEmptyStatesUseTheSharedComponent() throws Exception {
        String cards = read("maintenance/maintenance_cards.jsp");
        assertTrue(cards.contains("maintenance-cards-empty ui-empty-state"));
        assertTrue(cards.contains("<strong>등록된 고객사 정보가 없습니다.</strong>"));
        assertFalse(cards.contains("<div class=\"empty-state\">"));

        String monthly = read("mypage/monthly_customer_response.jsp");
        assertTrue(monthly.contains("monthly-response-empty ui-empty-state"));
        assertTrue(monthly.contains("<strong>고객 응대 기록이 없습니다.</strong>"));
        assertTrue(monthly.contains("data-monthly-action=\"add\""));

        String recent = read("WEB-INF/includes/mypage/recent_activity.jspf");
        assertTrue(recent.contains("mypage-recent-empty ui-empty-state"));
        assertFalse(recent.contains("class=\"mypage-empty-state\""));
    }

    private static void assertConditionalTable(
            String source, String nonEmptyMarker, String emptyMarker) {
        int branchStart = source.indexOf(nonEmptyMarker);
        int tableStart = source.indexOf("<table", branchStart);
        int footer = source.indexOf("<t:tableFooter", branchStart);
        int emptyState = source.indexOf(emptyMarker, branchStart);
        int branchEnd = findMatchingTagEnd(
                source, branchStart, "<c:when", "</c:when>");

        assertTrue(branchStart >= 0, nonEmptyMarker);
        assertTrue(tableStart > branchStart, nonEmptyMarker);
        assertTrue(footer > tableStart, nonEmptyMarker);
        assertTrue(branchEnd > footer, nonEmptyMarker);
        assertTrue(emptyState > branchEnd, emptyMarker);
    }

    private static void assertEmptyBranchHasNoAction(
            String source, String emptyMarker) {
        int emptyState = source.indexOf(emptyMarker);
        int emptyBranchStart = source.lastIndexOf("<c:otherwise>", emptyState);
        int emptyBranchEnd = findMatchingTagEnd(
                source,
                emptyBranchStart,
                "<c:otherwise>",
                "</c:otherwise>");

        assertTrue(emptyState >= 0, emptyMarker);
        assertTrue(emptyBranchStart >= 0, emptyMarker);
        assertTrue(emptyBranchEnd > emptyState, emptyMarker);
        assertFalse(source.substring(emptyState, emptyBranchEnd)
                .contains("ui-button"), emptyMarker);
    }

    private static int findMatchingTagEnd(
            String source, int openingStart, String openingTag, String closingTag) {
        if (openingStart < 0) {
            return -1;
        }

        int depth = 0;
        int cursor = openingStart;
        while (cursor < source.length()) {
            int nextOpening = source.indexOf(openingTag, cursor);
            int nextClosing = source.indexOf(closingTag, cursor);
            if (nextClosing < 0) {
                return -1;
            }
            if (nextOpening >= 0 && nextOpening < nextClosing) {
                depth++;
                cursor = nextOpening + openingTag.length();
                continue;
            }

            depth--;
            if (depth == 0) {
                return nextClosing + closingTag.length();
            }
            cursor = nextClosing + closingTag.length();
        }
        return -1;
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
