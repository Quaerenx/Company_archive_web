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
    private static final Path VM_HOST_SERVLET =
            Path.of("src/main/java/com/company/controller/UserVmHostServlet.java");

    @Test
    void linksPersonalHostsAndVisualConstantsRemainStable() throws Exception {
        String page = read("mypage/mypage.jsp");
        assertTrue(page.contains("mypage?action=editProfile"));
        assertTrue(page.contains("mypage?action=changePassword"));
        assertTrue(page.contains("mypage?action=monthlyResponse"));
        assertTrue(page.contains("maintenanceCount"));
        assertTrue(page.contains("troubleshootingCount"));
        assertTrue(page.contains("myMaintenanceRecords"));
        assertTrue(page.contains("myTroubleshootings"));
        assertTrue(page.contains("maintenancePage"));
        assertTrue(page.contains("troubleshootingPage"));
        assertTrue(page.contains("ui-pagination"));
        assertFalse(page.contains("end=\"9\""));
        assertTrue(page.contains("<c:param name=\"view\" value=\"history\" />"));
        assertTrue(page.contains("<c:param name=\"customerName\" value=\"${record.customerName}\" />"));
        assertTrue(page.contains("/resources/css/pages/mypage.css"));
        assertTrue(page.contains("/resources/js/pages/mypage_hosts.js"));
        assertTrue(page.contains("id=\"vmHostBoardBody\""));
        assertTrue(page.contains("id=\"toggleVmHostBoardBtn\""));
        assertTrue(page.contains("aria-controls=\"vmHostBoardBody\""));
        assertTrue(page.contains("id=\"openVmHostAddBtn\""));
        assertTrue(page.contains("id=\"vmHostModalBackdrop\""));
        assertTrue(page.contains("id=\"vmHostSaveForm\""));
        assertTrue(page.contains("id=\"vmHostDeleteForm\" method=\"post\""));
        assertTrue(page.contains("data-original-ip=\"<c:out value='${vmHostOriginalIp}'/>\""));
        assertFalse(page.contains("data-original-ip=\"<c:out value='${vmHostForm.ip}'/>\""));
        assertTrue(page.contains("<c:out value=\"${host.ip}\" />"));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(page.contains("name=\"action\" value=\"save\""));
        assertTrue(page.contains("name=\"action\" value=\"delete\""));
        assertTrue(page.indexOf("name=\"returnTo\" value=\"mypage\"")
                != page.lastIndexOf("name=\"returnTo\" value=\"mypage\""));
        assertFalse(page.contains("name=\"returnTo\" value=\"dashboard\""));
        for (String field : new String[] {
                "ip", "purpose", "osInfo", "verticaVersion", "remoteHost", "note"
        }) {
            assertTrue(page.contains("name=\"" + field + "\""), field);
        }
        assertFalse(page.contains("<style>"));
        assertFalse(page.contains("style=\""));

        String styles = page.contains("/resources/css/pages/mypage.css")
                ? read("resources/css/pages/mypage.css")
                : page;
        String sharedStyles = read("resources/css/ui-system.css");
        assertTrue(sharedStyles.contains("max-width: var(--page-content-max-width)"));
        assertTrue(sharedStyles.contains("padding-block: var(--space-32)"));
        assertFalse(styles.contains("max-width: var(--page-content-max-width)"));
        assertTrue(styles.contains("border: 1px solid var(--color-border)"));
        assertTrue(styles.contains("box-shadow: none"));
        assertFalse(styles.contains("translateY("));
        assertTrue(styles.contains("max-height: 400px"));
        assertTrue(styles.contains(".page-mypage .mypage-host-card"));
        assertTrue(styles.contains(".page-mypage .vm-modal-backdrop"));
        assertTrue(styles.contains("body.page-mypage.vm-modal-open"));
        assertTrue(styles.contains("width: min(720px, 100%)"));
        assertTrue(styles.contains("z-index: var(--z-dialog)"));

        String behavior = read("resources/js/pages/mypage_hosts.js");
        assertTrue(behavior.contains("archive.mypage.personal-hosts.collapsed"));
        assertTrue(behavior.contains("populateModal"));
        assertTrue(behavior.contains("querySelectorAll('.vm-edit-btn')"));
        assertTrue(behavior.contains("Frog2UI.createDialogController"));
        assertTrue(behavior.contains("modalDialog.open(trigger)"));
        assertTrue(behavior.contains("modalDialog.close()"));
        assertTrue(behavior.contains("event.key === 'Escape'"));
        assertTrue(behavior.contains("해당 호스트를 삭제하시겠습니까?"));

        String sharedBehavior = read("resources/js/ui-system.js");
        assertTrue(sharedBehavior.contains("event.key !== 'Tab'"));
        assertTrue(sharedBehavior.contains("document.activeElement"));
        assertTrue(sharedBehavior.contains("focusableElements(dialog)"));
        assertTrue(sharedBehavior.contains("focusTarget.focus()"));
    }

    @Test
    void controllerProvidesCalendarValuesPreviouslyCalculatedByTheJsp() throws Exception {
        String controller = Files.readString(MY_PAGE_SERVLET);
        assertTrue(controller.contains(
                "request.setAttribute(\"currentYear\", currentYear);"));
        assertTrue(controller.contains(
                "request.setAttribute(\"currentMonth\", currentMonth);"));
        assertTrue(controller.contains(
                "getMaintenanceRecordsByOwner("));
        assertTrue(controller.contains(
                "getTroubleshootingPageByOwner("));
        assertTrue(controller.contains(
                "Pagination.requestedPage("));
        assertTrue(controller.contains(
                "userVmHostDAO.getActiveHostsByOwner(user.getUserId())"));
        assertTrue(controller.contains(
                "request.setAttribute(\"vmHosts\", vmHosts);"));
    }

    @Test
    void personalHostPostReturnsToMyPageWithoutDashboardCoupling() throws Exception {
        String controller = Files.readString(VM_HOST_SERVLET);
        assertTrue(controller.contains("\"mypage\".equals(returnTo)"));
        assertTrue(controller.contains("/mypage?vmHostResult="));
        assertTrue(controller.contains("MyPageServlet.renderMainPage("));
        assertFalse(controller.contains("renderDashboard("));
        assertFalse(controller.contains("/dashboard?vmHostResult="));
        assertFalse(controller.contains("DashboardMenuProvider"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
