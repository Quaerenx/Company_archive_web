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

    @Test
    void dashboardLinksAndVisualConstantsRemainStable() throws Exception {
        String page = read("mypage/mypage.jsp");
        assertTrue(page.contains("mypage?action=editProfile"));
        assertTrue(page.contains("mypage?action=changePassword"));
        assertTrue(page.contains("mypage?action=monthlyResponse"));
        assertTrue(page.contains("maintenanceCount"));
        assertTrue(page.contains("troubleshootingCount"));
        assertTrue(page.contains("myMaintenanceRecords"));
        assertTrue(page.contains("myTroubleshootings"));
        assertTrue(page.contains("<c:param name=\"view\" value=\"history\" />"));
        assertTrue(page.contains("<c:param name=\"customerName\" value=\"${record.customerName}\" />"));
        assertTrue(page.contains("/resources/css/pages/mypage.css"));
        assertFalse(page.contains("<style>"));
        assertFalse(page.contains("style=\""));

        String styles = page.contains("/resources/css/pages/mypage.css")
                ? read("resources/css/pages/mypage.css")
                : page;
        assertTrue(styles.contains("max-width: 1000px"));
        assertTrue(styles.contains("border-top: 3px solid #3D5A80"));
        assertTrue(styles.contains("max-height: 400px"));
    }

    @Test
    void controllerProvidesCalendarValuesPreviouslyCalculatedByTheJsp() throws Exception {
        String controller = Files.readString(MY_PAGE_SERVLET);
        assertTrue(controller.contains(
                "request.setAttribute(\"currentYear\", currentYear);"));
        assertTrue(controller.contains(
                "request.setAttribute(\"currentMonth\", currentMonth);"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
