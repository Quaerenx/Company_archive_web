package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PageShellContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final String HEADER_INCLUDE = "include file=\"/includes/header.jsp\"";
    private static final String FOOTER_INCLUDE = "include file=\"/includes/footer.jsp\"";
    private static final Pattern STYLESHEET_LINK = Pattern.compile(
            "<link\\b[^>]*\\brel\\s*=\\s*['\"]stylesheet['\"]",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> AUTHENTICATED_PAGES = List.of(
            "dashboard.jsp",
            "customers/customers_add.jsp",
            "customers/customers_detail.jsp",
            "customers/customers_detail_edit.jsp",
            "customers/customers_edit.jsp",
            "customers/customers_list.jsp",
            "maintenance/maintenance_add.jsp",
            "maintenance/maintenance_cards.jsp",
            "maintenance/maintenance_edit.jsp",
            "maintenance/maintenance_history.jsp",
            "meeting/meeting_edit.jsp",
            "meeting/meeting_list.jsp",
            "meeting/meeting_view.jsp",
            "meeting/meeting_write.jsp",
            "mypage/change_password.jsp",
            "mypage/edit_profile.jsp",
            "mypage/monthly_customer_response.jsp",
            "mypage/mypage.jsp",
            "troubleshooting/troubleshooting_add.jsp",
            "troubleshooting/troubleshooting_edit.jsp",
            "troubleshooting/troubleshooting_list.jsp",
            "troubleshooting/troubleshooting_view.jsp",
            "vm_hosts/list.jsp",
            "WEB-INF/views/filerepo/list.jsp",
            "WEB-INF/views/filerepo/upload.jsp");

    @Test
    void authenticatedPagesUseExactlyOneSharedDocumentShell() throws Exception {
        for (String pagePath : AUTHENTICATED_PAGES) {
            String page = read(pagePath);

            assertEquals(1, occurrences(page, HEADER_INCLUDE), pagePath);
            assertEquals(1, occurrences(page, FOOTER_INCLUDE), pagePath);
            assertFalse(page.contains("<!DOCTYPE"), pagePath);
            assertFalse(page.contains("<html"), pagePath);
            assertFalse(page.contains("<head"), pagePath);
            assertFalse(page.contains("<body"), pagePath);
            assertFalse(page.contains("</body>"), pagePath);
            assertFalse(page.contains("</html>"), pagePath);
            assertFalse(page.contains("include file=\"/WEB-INF/includes/core_styles.jspf\""),
                    pagePath);
            assertFalse(page.contains("include file=\"/WEB-INF/includes/header_nav.jspf\""),
                    pagePath);
            assertFalse(page.contains("include file=\"/WEB-INF/includes/footer_content.jspf\""),
                    pagePath);
            assertFalse(STYLESHEET_LINK.matcher(page).find(), pagePath);
        }
    }

    @Test
    void sharedShellOwnsDocumentNavigationAndAssetSlots() throws Exception {
        String header = read("includes/header.jsp");
        String footer = read("includes/footer.jsp");

        assertEquals(1, occurrences(header, "<!DOCTYPE html>"));
        assertEquals(1, occurrences(header, "<html lang=\"ko\">"));
        assertEquals(1, occurrences(header, "<head>"));
        assertEquals(1, occurrences(header, "<body"));
        assertTrue(header.contains("include file=\"/WEB-INF/includes/core_styles.jspf\""));
        assertTrue(header.contains("include file=\"/WEB-INF/includes/header_nav.jspf\""));
        assertTrue(header.contains("not empty pageCss"));
        assertFalse(header.contains("pageCssBeforeVendor"));
        assertFalse(header.contains("pageCssAfterHeader"));
        assertTrue(header.contains("pageDocumentTitle"));

        assertEquals(1, occurrences(footer, "</body>"));
        assertEquals(1, occurrences(footer, "</html>"));
        assertTrue(footer.contains("include file=\"/WEB-INF/includes/footer_content.jspf\""));
        assertTrue(footer.contains("not empty vendorScript"));
        assertTrue(footer.contains("not empty pageScript"));
    }

    @Test
    void sharedShellVersionsLocalStylesAndScripts() throws Exception {
        String coreStyles = read("WEB-INF/includes/core_styles.jspf");
        String header = read("includes/header.jsp");
        String navigation = read("WEB-INF/includes/header_nav.jspf");
        String footer = read("includes/footer.jsp");
        String webXml = read("WEB-INF/web.xml");

        assertTrue(header.contains("var=\"frog2AssetVersion\""));
        assertTrue(header.contains("${initParam.frog2AssetVersion}"));
        assertTrue(webXml.contains("<param-name>frog2AssetVersion</param-name>"));
        assertEquals(1, occurrences(webXml, "20260810-pagination-security-1"));
        assertEquals(6, occurrences(coreStyles, "?v=${frog2AssetVersion}"));
        assertEquals(2, occurrences(header, "?v=${frog2AssetVersion}"));
        assertTrue(navigation.contains("header_nav.js?v=${frog2AssetVersion}"));
        assertTrue(footer.contains("ui-system.js?v=${frog2AssetVersion}"));
        assertTrue(footer.contains("ambient-background.js?v=${frog2AssetVersion}"));
        assertTrue(footer.contains("${script}?v=${frog2AssetVersion}"));

        String login = read("login.jsp");
        assertEquals(6, occurrences(login, "?v=${initParam.frog2AssetVersion}"));
        assertTrue(login.contains(
                "/resources/js/ui-system.js?v=${initParam.frog2AssetVersion}"));
        assertTrue(login.contains(
                "/resources/js/ambient-background.js?v=${initParam.frog2AssetVersion}"));
        assertFalse(login.matches("(?s).*\\?v=202\\d+.*"));
        for (String errorPage : List.of(
                "error/400.jsp",
                "error/403.jsp",
                "error/404.jsp",
                "error/405.jsp",
                "error/409.jsp",
                "error/500.jsp",
                "error/503.jsp")) {
            assertTrue(read(errorPage).contains("?v=${initParam.frog2AssetVersion}"),
                    errorPage);
        }
    }

    @Test
    void fileRepositoryPageStylesKeepGenericRulesInsidePageScopes() throws Exception {
        String upload = read("resources/css/pages/upload.css");
        String download = read("resources/css/pages/download.css");

        assertFalse(upload.contains(".page-file-upload * {"));
        assertTrue(upload.contains(".page-file-upload .upload-page {"));
        assertTrue(upload.contains(".page-file-upload .upload-container {"));
        assertFalse(upload.matches("(?m)^\\*\\s*\\{"));
        assertFalse(upload.matches("(?m)^body\\s*\\{"));
        assertFalse(upload.matches("(?m)^\\.container\\s*\\{"));

        assertTrue(download.contains("body.page-file-repository {"));
        assertTrue(download.contains(".page-file-repository .main-content {"));
        assertTrue(download.contains(".page-file-repository .breadcrumb {"));
        assertFalse(download.matches("(?m)^\\.main-content\\s*\\{"));
        assertFalse(download.matches("(?m)^\\.breadcrumb\\s*\\{"));
    }

    @Test
    void authenticatedPagesUseTheSharedContentShell() throws Exception {
        for (String pagePath : AUTHENTICATED_PAGES) {
            String page = read(pagePath);
            assertEquals(1, occurrences(page, "content-shell"), pagePath);
        }
    }

    @Test
    void removedDashboardBoxStylesAreNotReferenced() throws Exception {
        assertFalse(Files.exists(
                WEBAPP.resolve("resources/css/pages/dashboard_box.css")));
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".jsp")
                            || file.toString().endsWith(".jspf")
                            || file.toString().endsWith(".tag"))
                    .toList()) {
                assertFalse(Files.readString(path).contains("dashboard_box.css"),
                        path.toString());
            }
        }
    }

    @Test
    void retiredTypographyDashboardPreviewStaysRemoved() throws Exception {
        assertFalse(Files.exists(Path.of(
                "src/main/java/com/company/controller/TypographyDashboardPreviewServlet.java")));
        assertFalse(Files.exists(
                WEBAPP.resolve("WEB-INF/views/design/typography_dashboard.jsp")));
        assertFalse(Files.exists(WEBAPP.resolve(
                "resources/css/pages/typography_dashboard_assignees.css")));

        String webXml = read("WEB-INF/web.xml");
        String ambientCss = read("resources/css/ambient-background.css");
        String ambientScript = read("resources/js/ambient-background.js");
        assertFalse(webXml.contains("TypographyDashboardPreviewServlet"));
        assertFalse(webXml.contains("dashboard-typography-preview"));
        assertFalse(ambientCss.contains("typography-dashboard-page"));
        assertFalse(ambientScript.contains("typography-dashboard-page"));
    }

    @Test
    void migratedPagesPreserveBodyClassesAndAssetOrderDeclarations() throws Exception {
        Map<String, PageAssets> expected = new LinkedHashMap<>();
        expected.put("dashboard.jsp", new PageAssets(
                "page-1050 dashboard-page",
                "/resources/css/pages/dashboard.css",
                "/resources/js/pages/dashboard.js"));
        expected.put("customers/customers_list.jsp", new PageAssets(
                "page-1050 page-customers",
                "/resources/css/pages/customers.css",
                "/resources/js/pages/customers_list.js"));
        expected.put("WEB-INF/views/filerepo/list.jsp", new PageAssets(
                "page-1050 page-file-repository file-server-container",
                "/resources/css/pages/download.css",
                null));
        expected.put("WEB-INF/views/filerepo/upload.jsp", new PageAssets(
                "page-1050 page-file-upload",
                "/resources/css/pages/upload.css",
                "/resources/js/pages/file_repository_upload.js"));
        expected.put("mypage/mypage.jsp", new PageAssets(
                "page-1050 page-customers page-mypage",
                "/resources/css/pages/mypage.css",
                "/resources/js/pages/mypage_hosts.js"));
        expected.put("mypage/edit_profile.jsp", new PageAssets(
                "page-1050 page-customers page-mypage",
                "/resources/css/pages/profile_edit.css",
                "/resources/js/pages/profile_edit.js"));
        expected.put("mypage/change_password.jsp", new PageAssets(
                "page-1050 page-customers page-mypage",
                "/resources/css/pages/password_change.css",
                "/resources/js/pages/password_change.js"));
        expected.put("mypage/monthly_customer_response.jsp", new PageAssets(
                "page-1050 page-customers monthly-response-page",
                "/resources/css/pages/monthly_customer_response.css",
                "/resources/js/pages/monthly_customer_response.js"));
        expected.put("vm_hosts/list.jsp", new PageAssets(
                "page-1050 page-customers page-vm-hosts",
                "/resources/css/pages/vm_hosts.css",
                "/resources/js/pages/vm_hosts.js"));

        for (Map.Entry<String, PageAssets> entry : expected.entrySet()) {
            String page = read(entry.getKey());
            PageAssets assets = entry.getValue();

            assertEquals("${pageTitle}", declaredValue(page, "pageDocumentTitle"),
                    entry.getKey());
            assertEquals(assets.bodyClass(), declaredValue(page, "pageBodyClass"),
                    entry.getKey());
            assertEquals(assets.pageCss(), declaredValue(page, "pageCss"), entry.getKey());
            assertFalse(hasDeclaration(page, "pageCssBeforeVendor"), entry.getKey());
            assertFalse(hasDeclaration(page, "pageCssAfterHeader"), entry.getKey());
            assertOptionalDeclaration(page, "pageScript", assets.pageScript(), entry.getKey());
        }
    }

    @Test
    void publicAndErrorPagesKeepIndependentShells() throws Exception {
        for (String pagePath : List.of(
                "login.jsp",
                "error/400.jsp",
                "error/403.jsp",
                "error/404.jsp",
                "error/405.jsp",
                "error/409.jsp",
                "error/500.jsp",
                "error/503.jsp")) {
            String page = read(pagePath);
            assertTrue(page.contains("<!DOCTYPE html>"), pagePath);
            assertFalse(page.contains(HEADER_INCLUDE), pagePath);
            assertFalse(page.contains(FOOTER_INCLUDE), pagePath);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }

    private static String declaredValue(String source, String variable) {
        Pattern declaration = Pattern.compile(
                "<c:set\\b(?=[^>]*\\bvar=\"" + Pattern.quote(variable) + "\")"
                        + "(?=[^>]*\\bvalue=\"([^\"]+)\")[^>]*>");
        Matcher matcher = declaration.matcher(source);
        assertTrue(matcher.find(), variable + " declaration is missing");
        return matcher.group(1);
    }

    private static void assertOptionalDeclaration(
            String source, String variable, String expected, String pagePath) {
        if (expected == null) {
            assertFalse(hasDeclaration(source, variable), pagePath + ": " + variable);
            return;
        }
        assertEquals(expected, declaredValue(source, variable), pagePath);
    }

    private static boolean hasDeclaration(String source, String variable) {
        return Pattern.compile(
                "<c:set\\b(?=[^>]*\\bvar=\"" + Pattern.quote(variable) + "\")[^>]*>")
                .matcher(source)
                .find();
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

    private record PageAssets(
            String bodyClass,
            String pageCss,
            String pageScript) {
    }
}
