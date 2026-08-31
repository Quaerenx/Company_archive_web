package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MyPageViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path MY_PAGE_SERVLET =
            Path.of("src/main/java/com/company/controller/MyPageServlet.java");
    private static final Path MY_PAGE_QUERY_SERVICE =
            Path.of("src/main/java/com/company/controller/MyPageQueryService.java");
    private static final Path VM_HOST_SERVLET =
            Path.of("src/main/java/com/company/controller/UserVmHostServlet.java");

    @Test
    void everyMyPageFragmentDeclaresUtf8Encoding() throws Exception {
        for (String fragment : new String[] {
                "profile_summary.jspf",
                "work_inbox.jspf",
                "recent_activity.jspf",
                "host_summary.jspf",
                "host_manager.jspf"
        }) {
            String source = read("WEB-INF/includes/mypage/" + fragment);
            assertTrue(source.startsWith(
                    "<%@ page pageEncoding=\"UTF-8\" %>"), fragment);
        }
    }

    @Test
    void overviewKeepsProfileInboxRecentWorkAndHostSummary() throws Exception {
        String page = read("mypage/mypage.jsp");
        String profile = read(
                "WEB-INF/includes/mypage/profile_summary.jspf");
        String activity = read(
                "WEB-INF/includes/mypage/recent_activity.jspf");
        String workInbox = read(
                "WEB-INF/includes/mypage/work_inbox.jspf");
        String hostSummary = read(
                "WEB-INF/includes/mypage/host_summary.jspf");

        assertTrue(page.contains("page-1050 page-mypage"));
        assertFalse(page.contains("page-customers"));
        assertTrue(page.contains("hostManagementMode"));
        assertTrue(page.contains("/resources/css/pages/mypage_hosts.css"));
        assertTrue(page.contains("/resources/js/pages/mypage_hosts.js"));
        assertTrue(page.contains("profile_summary.jspf"));
        assertTrue(page.contains("work_inbox.jspf"));
        assertTrue(page.contains("recent_activity.jspf"));
        assertTrue(page.contains("host_summary.jspf"));

        assertTrue(profile.contains("${userInfo.userName}"));
        assertTrue(profile.contains("${userInfo.userId}"));
        assertTrue(profile.contains("${userInfo.department}"));
        assertTrue(profile.contains("mypage?action=editProfile"));
        assertTrue(profile.contains("mypage?action=changePassword"));

        assertTrue(workInbox.contains("업무 인박스"));
        assertTrue(workInbox.contains("workInbox.items"));
        assertTrue(workInbox.contains("workInbox.dangerCount"));
        assertTrue(workInbox.contains("workInbox.warningCount"));
        assertTrue(workInbox.contains("workInbox.infoCount"));
        assertTrue(workInbox.contains("${item.path}"));
        assertTrue(workInbox.contains("${item.severityLabel}"));

        assertTrue(activity.contains("mypage?action=monthlyResponse"));
        assertTrue(activity.contains("recentMaintenanceRecords"));
        assertTrue(activity.contains("recentTroubleshootings"));
        assertTrue(activity.contains("maintenanceCount"));
        assertTrue(activity.contains("troubleshootingCount"));
        assertTrue(activity.contains(
                "<c:param name=\"view\" value=\"history\" />"));
        assertTrue(activity.contains(
                "<c:param name=\"customerName\" value=\"${record.customerName}\" />"));
        assertFalse(activity.contains("ui-pagination"));
        assertFalse(activity.contains("maintenancePage"));
        assertFalse(activity.contains("troubleshootingPage"));
        assertFalse(activity.contains("자주 사용하는 바로가기"));
        assertFalse(activity.contains("quick-link-item"));

        assertTrue(hostSummary.contains("/mypage?section=hosts"));
        assertTrue(hostSummary.contains("vmHostCount"));
        assertTrue(hostSummary.contains("vmHostLimit"));
    }

    @Test
    void workInboxBadgesCenterTheirLabels() throws Exception {
        String styles = read("resources/css/pages/mypage.css");

        assertTrue(styles.contains(
                ".page-mypage .work-inbox__link .ui-badge {"));
        assertTrue(styles.contains("justify-content: center;"));
        assertTrue(styles.contains("text-align: center;"));
    }

    @Test
    void hostManagerKeepsExistingOwnershipFormAndDialogContracts()
            throws Exception {
        String hosts = read("WEB-INF/includes/mypage/host_manager.jspf");
        String styles = read("resources/css/pages/mypage_hosts.css");
        String behavior = read("resources/js/pages/mypage_hosts.js");

        assertTrue(hosts.contains("id=\"openVmHostAddBtn\""));
        assertTrue(hosts.contains("id=\"vmHostModalBackdrop\""));
        assertTrue(hosts.contains("id=\"vmHostSaveForm\""));
        assertTrue(hosts.contains(
                "id=\"vmHostDeleteForm\" method=\"post\""));
        assertTrue(hosts.contains(
                "data-original-ip=\"<c:out value='${vmHostOriginalIp}'/>\""));
        assertFalse(hosts.contains(
                "data-original-ip=\"<c:out value='${vmHostForm.ip}'/>\""));
        assertTrue(hosts.contains("csrf_input.jspf"));
        assertTrue(hosts.contains("name=\"action\" value=\"save\""));
        assertTrue(hosts.contains("name=\"action\" value=\"delete\""));
        assertTrue(hosts.indexOf("name=\"returnTo\" value=\"mypage\"")
                != hosts.lastIndexOf("name=\"returnTo\" value=\"mypage\""));
        for (String field : new String[] {
                "ip", "purpose", "osInfo", "verticaVersion", "remoteHost", "note"
        }) {
            assertTrue(hosts.contains("name=\"" + field + "\""), field);
        }
        assertFalse(hosts.contains("<style>"));
        assertFalse(hosts.contains("style=\""));

        assertTrue(styles.contains(".page-mypage .mypage-host-manager"));
        assertTrue(styles.contains(".page-mypage .vm-modal-backdrop"));
        assertTrue(styles.contains("body.page-mypage.vm-modal-open"));
        assertTrue(styles.contains("width: min(720px, 100%)"));
        assertTrue(styles.contains("z-index: var(--z-dialog)"));

        assertTrue(behavior.contains("populateModal"));
        assertTrue(behavior.contains("querySelectorAll('.vm-edit-btn')"));
        assertTrue(behavior.contains("Frog2UI.createDialogController"));
        assertTrue(behavior.contains("modalDialog.open(trigger)"));
        assertTrue(behavior.contains("modalDialog.close()"));
        assertTrue(behavior.contains("event.key === 'Escape'"));
        assertTrue(behavior.contains("해당 호스트를 삭제하시겠습니까?"));
        assertFalse(behavior.contains(
                "archive.mypage.personal-hosts.collapsed"));
    }

    @Test
    void controllerLoadsFiveRecentItemsAndDefersTheHostList()
            throws Exception {
        String controller = Files.readString(MY_PAGE_SERVLET);
        String queryService = Files.readString(MY_PAGE_QUERY_SERVICE);

        assertTrue(controller.contains("RECENT_ACTIVITY_LIMIT = 5"));
        assertTrue(controller.contains("HOSTS_SECTION.equals(section)"));
        assertTrue(queryService.contains(
                "userVmHostDAO.getActiveHostsByOwner(userId)"));
        assertTrue(queryService.contains(
                "userVmHostDAO.getActiveHostCountByOwner(userId)"));
        assertTrue(controller.contains("recentMaintenanceRecords"));
        assertTrue(controller.contains("recentTroubleshootings"));
        assertTrue(controller.contains("workInbox"));
        assertTrue(controller.contains("queryService.loadOverview("));
        assertTrue(queryService.contains("userId, 1, recentActivityLimit"));
        assertFalse(controller.contains("Pagination.requestedPage("));
        assertFalse(controller.contains(
                "request.getParameter(\"maintenancePage\")"));
        assertFalse(controller.contains(
                "request.getParameter(\"troubleshootingPage\")"));
    }

    @Test
    void personalHostPostReturnsToTheHostManagementSection()
            throws Exception {
        String controller = Files.readString(VM_HOST_SERVLET);

        assertTrue(controller.contains("\"mypage\".equals(returnTo)"));
        assertTrue(controller.contains(
                "/mypage?section=hosts&vmHostResult="));
        assertTrue(controller.contains(
                "request.setAttribute(\"myPageSection\", \"hosts\")"));
        assertTrue(controller.contains("MyPageServlet.renderMainPage("));
        assertFalse(controller.contains("renderDashboard("));
        assertFalse(controller.contains("/dashboard?vmHostResult="));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
