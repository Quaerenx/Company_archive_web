package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CssLayoutStructureTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Path CSS = WEBAPP.resolve("resources/css");

    @Test
    void sharedTokensHaveOneDefinitionSource() throws Exception {
        String tokens = Files.readString(CSS.resolve("tokens.css"));
        String main = Files.readString(CSS.resolve("main_style.css"));
        String base = Files.readString(CSS.resolve("base.css"));
        String utilities = Files.readString(CSS.resolve("utilities.css"));
        String login = Files.readString(CSS.resolve("login_style.css"));
        String customers = Files.readString(CSS.resolve("pages/customers.css"));

        assertTrue(tokens.startsWith(":root {"));
        assertTrue(main.startsWith("@import url(\"tokens.css\");\n@import url(\"base.css\");"));
        assertFalse(base.contains("@import"));
        assertFalse(base.contains(":root {"));
        assertFalse(login.contains("@import"));
        assertFalse(main.contains(":root {"));
        assertFalse(login.contains(":root {"));
        assertFalse(customers.contains(":root {"));
        for (String utility : List.of(
                ".d-flex {",
                ".justify-content-between {",
                ".align-items-center {",
                ".mb-3 {",
                ".mt-4 {",
                ".text-primary {")) {
            assertFalse(base.contains(utility), utility);
            assertTrue(utilities.contains(utility), utility);
        }
    }

    @Test
    void customerStylesAlwaysHaveTheSharedTokenProvider() throws Exception {
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".jsp")).toList()) {
                String source = Files.readString(path);
                if (source.contains("/resources/css/pages/customers.css")) {
                    assertTrue(
                            source.contains("/resources/css/main_style.css")
                                    || source.contains("include file=\"/includes/header.jsp\"")
                                    || source.contains(
                                            "include file=\"/WEB-INF/includes/core_styles.jspf\""),
                            () -> "customers.css has no token provider: " + path);
                }
            }
        }
    }

    @Test
    void commonIncludesExposePageAssetSlots() throws Exception {
        String header = Files.readString(WEBAPP.resolve("includes/header.jsp"));
        String footer = Files.readString(WEBAPP.resolve("includes/footer.jsp"));

        assertTrue(header.contains("not empty pageCss"));
        assertFalse(header.contains("pageCssBeforeVendor"));
        assertFalse(header.contains("pageCssAfterHeader"));
        assertTrue(header.contains("not empty pageCss"));
        assertTrue(footer.contains("not empty pageScript"));
    }

    @Test
    void migratedCustomerPagesHaveNoInlineBlocks() throws Exception {
        assertExternalized(
                "customers/customers_add.jsp",
                "resources/js/pages/customers_add.js",
                false);
        assertExternalized(
                "customers/customers_edit.jsp",
                "resources/js/pages/customers_edit.js",
                false);
        assertExternalized(
                "customers/customers_list.jsp",
                "resources/js/pages/customers_list.js",
                false);
        assertExternalized(
                "customers/customers_detail_edit.jsp",
                "resources/js/pages/customer_detail_edit.js",
                false);
        assertExternalized(
                "customers/customers_detail.jsp",
                "resources/js/pages/customer_detail.js",
                true);

        String list = Files.readString(WEBAPP.resolve("customers/customers_list.jsp"));
        assertTrue(list.contains("/resources/js/pages/customers_list.js"));
        assertTrue(list.contains("data-customer-list"));
        assertFalse(list.contains("onclick="));
        assertFalse(list.contains("javascript:void"));
        assertFalse(list.contains("style=\""));

        String detail = Files.readString(WEBAPP.resolve("customers/customers_detail.jsp"));
        assertTrue(detail.contains("/resources/css/pages/customer_detail.css"));
        assertTrue(detail.contains("data-context-path="));
        assertFalse(detail.contains("style=\""));
    }

    @Test
    void migratedMaintenancePagesHaveNoInlineBlocks() throws Exception {
        assertExternalized(
                "maintenance/maintenance_add.jsp",
                "resources/js/pages/maintenance_form.js",
                false);
        assertExternalized(
                "maintenance/maintenance_edit.jsp",
                "resources/js/pages/maintenance_form.js",
                false);
        assertExternalized(
                "maintenance/maintenance_cards.jsp",
                "resources/js/pages/maintenance_cards.js",
                true);
        assertExternalized(
                "maintenance/maintenance_history.jsp",
                "resources/js/pages/maintenance_history.js",
                true);

        String add = Files.readString(WEBAPP.resolve("maintenance/maintenance_add.jsp"));
        assertTrue(add.contains("data-context-path="));
        assertTrue(add.contains("/resources/js/pages/maintenance_form.js"));

        String edit = Files.readString(WEBAPP.resolve("maintenance/maintenance_edit.jsp"));
        assertTrue(edit.contains("id=\"maintenanceForm\""));
        assertTrue(edit.contains("id=\"deleteFormHeader\""));
        assertFalse(edit.contains("onsubmit="));
        assertFalse(edit.contains("style=\""));

        String editScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/maintenance_form.js"));
        assertTrue(editScript.contains("getElementById('maintenanceForm')"));
        assertTrue(editScript.contains("getElementById('deleteFormHeader')"));
        assertFalse(editScript.contains("querySelector('form')"));
        assertTrue(editScript.contains("정말 삭제하시겠습니까?"));

        String cardsCss = Files.readString(
                CSS.resolve("pages/maintenance_cards.css"));
        assertFalse(cardsCss.contains(".maintenance-management .alert"));
        assertTrue(cardsCss.contains(".maintenance-management .empty-state"));
        assertFalse(cardsCss.matches("(?s).*\\n\\s*\\.(alert|empty-state|customer-card)[\\s:{.].*"));

        String cardsScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/maintenance_cards.js"));
        assertFalse(cardsScript.contains(".style."));
        assertTrue(cardsScript.contains("classList.add('is-loading')"));

        String history = Files.readString(
                WEBAPP.resolve("maintenance/maintenance_history.jsp"));
        assertTrue(history.contains("data-usage-point"));
        assertFalse(history.contains("data-customer-name="));
        assertFalse(history.contains("style=\""));

        String historyCss = Files.readString(
                CSS.resolve("pages/maintenance_history.css"));
        assertFalse(historyCss.contains(".maintenance-history .alert"));
        assertTrue(historyCss.contains(".maintenance-history .history-item"));
        assertFalse(historyCss.lines().map(String::stripLeading)
                .anyMatch(line -> line.startsWith(".alert")
                        || line.startsWith(".history-item")
                        || line.startsWith(".empty-history")));

        String historyScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/maintenance_history.js"));
        assertTrue(historyScript.contains("readOptionalNumber"));
        assertTrue(historyScript.contains("item.animate"));
        assertFalse(historyScript.contains(".style."));
        assertFalse(historyScript.contains("deleteForms"));
    }

    @Test
    void migratedMeetingPagesHaveNoInlineBlocks() throws Exception {
        assertExternalized(
                "meeting/meeting_list.jsp",
                "resources/js/pages/meeting_list.js",
                true);
        assertExternalized(
                "meeting/meeting_write.jsp",
                "resources/js/pages/meeting_form.js",
                false);
        assertExternalized(
                "meeting/meeting_edit.jsp",
                "resources/js/pages/meeting_form.js",
                false);
        assertExternalized(
                "meeting/meeting_view.jsp",
                "resources/js/pages/meeting_view.js",
                false);

        String list = Files.readString(WEBAPP.resolve("meeting/meeting_list.jsp"));
        assertTrue(list.contains("page-meeting"));
        assertTrue(list.contains("meeting-list-table"));
        assertFalse(list.contains("troubleshooting-table"));
        assertFalse(list.contains("onclick="));

        String listCss = Files.readString(CSS.resolve("pages/meeting_list.css"));
        assertTrue(listCss.contains(".meeting-management .meeting-list-table"));
        assertTrue(listCss.contains(".meeting-management .title-link"));

        String listScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/meeting_list.js"));
        assertTrue(listScript.contains(".meeting-management tr[data-detail-url]"));

        String write = Files.readString(WEBAPP.resolve("meeting/meeting_write.jsp"));
        String edit = Files.readString(WEBAPP.resolve("meeting/meeting_edit.jsp"));
        assertTrue(write.contains("data-meeting-mode=\"write\""));
        assertTrue(edit.contains("data-meeting-mode=\"edit\""));
        assertFalse(write.contains("onclick="));
        assertFalse(edit.contains("onclick="));
        assertTrue(write.contains("data-meeting-action=\"preview\""));
        assertTrue(edit.contains("data-meeting-action=\"delete\""));

        String view = Files.readString(WEBAPP.resolve("meeting/meeting_view.jsp"));
        assertTrue(view.contains("data-context-path="));
        assertTrue(view.contains("data-meeting-id="));
        assertFalse(view.contains("id=\"deleteForm\""));

        String viewScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/meeting_view.js"));
        assertTrue(viewScript.contains("new URLSearchParams"));
        assertTrue(viewScript.contains("parameters.set('action', action)"));
        assertFalse(viewScript.contains("innerHTML"));
    }

    @Test
    void headerAssetsLoadFromHeadAndBehaviorIsExternal() throws Exception {
        String headerNav = Files.readString(
                WEBAPP.resolve("WEB-INF/includes/header_nav.jspf"));
        assertTrue(headerNav.contains("/resources/js/header_nav.js"));
        assertFalse(headerNav.contains("<script>"));
        assertFalse(headerNav.contains("<link rel=\"stylesheet\""));
        assertFalse(headerNav.contains("document.createElement('link')"));

        String headerScript = Files.readString(
                WEBAPP.resolve("resources/js/header_nav.js"));
        assertTrue(headerScript.contains("Frog2Csrf"));
        assertTrue(headerScript.contains("form.method = 'POST'"));
        assertFalse(headerScript.contains("$" + "{"));

        String header = Files.readString(WEBAPP.resolve("includes/header.jsp"));
        int headEnd = header.indexOf("</head>");
        int headerCss = header.indexOf("/resources/css/pages/header.css");
        assertTrue(headEnd > 0);
        assertTrue(headerCss > 0 && headerCss < headEnd);
        assertTrue(header.substring(0, headEnd).contains(
                "include file=\"/WEB-INF/includes/favicon.jspf\""));
        assertTrue(header.indexOf("/resources/css/pages/header.css")
                == header.lastIndexOf("/resources/css/pages/header.css"));
    }

    @Test
    void migratedDashboardHasNoInlineBlocks() throws Exception {
        assertExternalized(
                "dashboard.jsp",
                "resources/js/pages/dashboard.js",
                true);

        String page = Files.readString(WEBAPP.resolve("dashboard.jsp"));
        assertTrue(page.contains("dashboard-page"));
        assertTrue(page.contains("/resources/css/pages/dashboard.css"));
        assertFalse(page.contains("onclick="));
        assertFalse(page.contains("onchange="));
        assertFalse(page.contains("onsubmit="));
        assertFalse(page.contains("style=\""));
        assertFalse(page.contains("dashboard-quick-actions"));
        assertFalse(page.contains("vmHostDeleteForm"));

        String script = Files.readString(
                WEBAPP.resolve("resources/js/pages/dashboard.js"));
        assertFalse(script.contains("vmHost"));
        assertFalse(script.contains(".style."));
        assertFalse(script.contains("$" + "{"));

        String styles = Files.readString(CSS.resolve("pages/dashboard.css"));
        assertTrue(styles.contains(
                "/* Dashboard workspace and maintenance components */"));
        assertFalse(styles.contains(".dashboard-page .page-header"));
        assertFalse(styles.contains(".dashboard-page .maintenance-kpi-"));
        assertTrue(styles.contains(".dashboard-page .maintenance-month-board"));
        assertFalse(styles.contains(".dashboard-page .vm-modal-backdrop"));
        assertTrue(styles.contains("border: 1px solid var(--color-border);"));

        assertExternalized(
                "mypage/mypage.jsp",
                "resources/js/pages/mypage_hosts.js",
                true);
        String myPage = Files.readString(WEBAPP.resolve("mypage/mypage.jsp"));
        assertTrue(myPage.contains("id=\"vmHostDeleteForm\" method=\"post\""));
        assertTrue(myPage.contains("vmHostDeleteForm") && myPage.contains("hidden"));
        String myPageScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/mypage_hosts.js"));
        assertTrue(myPageScript.contains("deleteForm.hidden = !isEdit"));
        assertTrue(myPageScript.contains("deleteForm.addEventListener('submit'"));
        String myPageStyles = Files.readString(CSS.resolve("pages/mypage.css"));
        assertTrue(myPageStyles.contains(".page-mypage .vm-modal-backdrop"));
        assertTrue(myPageStyles.contains("body.page-mypage.vm-modal-open"));

        String headerStyles = Files.readString(CSS.resolve("pages/header.css"));
        assertTrue(headerStyles.contains(".page-1050 .main-header .header-box"));
        assertTrue(headerStyles.contains(
                "max-width: var(--page-content-max-width, 1018px);"));
        assertTrue(headerStyles.contains(
                "width: calc(100% - var(--page-content-total-gutter, 32px));"));

        String downloadStyles = Files.readString(CSS.resolve("pages/download.css"));
        String fileServerRule = downloadStyles.substring(
                downloadStyles.indexOf("body.page-file-repository"),
                downloadStyles.indexOf(".page-file-repository .main-content"));
        assertFalse(fileServerRule.contains("background"));
    }

    @Test
    void migratedTroubleshootingPagesHaveNoInlineBlocks() throws Exception {
        assertExternalized(
                "troubleshooting/troubleshooting_add.jsp",
                "resources/js/pages/troubleshooting_form.js",
                true);
        assertExternalized(
                "troubleshooting/troubleshooting_edit.jsp",
                "resources/js/pages/troubleshooting_form.js",
                true);
        assertExternalized(
                "troubleshooting/troubleshooting_list.jsp",
                "resources/js/pages/troubleshooting_list.js",
                true);
        assertExternalized(
                "troubleshooting/troubleshooting_view.jsp",
                "resources/js/pages/troubleshooting_view.js",
                true);

        for (String pagePath : new String[] {
                "troubleshooting/troubleshooting_add.jsp",
                "troubleshooting/troubleshooting_edit.jsp",
                "troubleshooting/troubleshooting_list.jsp",
                "troubleshooting/troubleshooting_view.jsp"
        }) {
            String page = Files.readString(WEBAPP.resolve(pagePath));
            assertFalse(page.contains("onclick="), pagePath);
            assertFalse(page.contains("onchange="), pagePath);
            assertFalse(page.contains("onsubmit="), pagePath);
            assertFalse(page.contains("style=\""), pagePath);
        }

        String add = Files.readString(
                WEBAPP.resolve("troubleshooting/troubleshooting_add.jsp"));
        String edit = Files.readString(
                WEBAPP.resolve("troubleshooting/troubleshooting_edit.jsp"));
        assertTrue(add.contains("data-troubleshooting-form-mode=\"add\""));
        assertTrue(edit.contains("data-troubleshooting-form-mode=\"edit\""));
        assertTrue(add.contains("/resources/css/pages/troubleshooting_form.css"));
        assertTrue(edit.contains("/resources/css/pages/troubleshooting_form.css"));

        String formCss = Files.readString(
                CSS.resolve("pages/troubleshooting_form.css"));
        assertFalse(formCss.contains(".troubleshooting-form-page .form-container"));
        assertTrue(add.contains("ui-form-card"));
        assertTrue(edit.contains("ui-form-card"));
        assertTrue(add.contains("ui-form ui-form-layout"));
        assertTrue(edit.contains("ui-form ui-form-layout"));
        assertFalse(formCss.contains(".add-page"));
        assertFalse(formCss.contains(".edit-page"));

        String listScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/troubleshooting_list.js"));
        assertFalse(listScript.contains(".style."));
        assertFalse(listScript.contains("animationDelay"));
        assertTrue(listScript.contains("tr[data-detail-url]"));

        String view = Files.readString(
                WEBAPP.resolve("troubleshooting/troubleshooting_view.jsp"));
        assertTrue(view.contains("id=\"deleteTroubleshootingForm\""));
        assertTrue(view.contains("csrf_input.jspf"));
        assertTrue(view.contains("form=\"deleteTroubleshootingForm\""));
        assertFalse(view.contains("data-troubleshooting-id="));

        String viewScript = Files.readString(
                WEBAPP.resolve("resources/js/pages/troubleshooting_view.js"));
        assertTrue(viewScript.contains("form.addEventListener('submit'"));
        assertFalse(viewScript.contains("createElement"));
        assertFalse(viewScript.contains("Frog2Csrf"));

        String listCss = Files.readString(
                CSS.resolve("pages/troubleshooting_list.css"));
        String viewCss = Files.readString(
                CSS.resolve("pages/troubleshooting_view.css"));
        assertTrue(listCss.contains(
                ".troubleshooting-management .troubleshooting-table"));
        assertTrue(viewCss.contains(".troubleshooting-detail .detail-grid"));
        assertFalse(listCss.lines().map(String::stripLeading)
                .anyMatch(line -> line.startsWith(".alert")
                        || line.startsWith(".troubleshooting-table")));
        assertFalse(viewCss.lines().map(String::stripLeading)
                .anyMatch(line -> line.startsWith(".alert")
                        || line.startsWith(".detail-grid")));
    }

    @Test
    void migratedMonthlyResponsePageHasNoInlineBlocks() throws Exception {
        assertExternalized(
                "mypage/monthly_customer_response.jsp",
                "resources/js/pages/monthly_customer_response.js",
                true);

        String page = Files.readString(
                WEBAPP.resolve("mypage/monthly_customer_response.jsp"));
        assertTrue(page.contains("monthly-response-page"));
        assertTrue(page.contains("/resources/css/pages/monthly_customer_response.css"));
        assertTrue(page.contains("data-monthly-action=\"add\""));
        assertTrue(page.contains("data-monthly-auto-submit"));
        assertFalse(page.contains("onclick="));
        assertFalse(page.contains("onchange="));
        assertFalse(page.contains("style=\""));

        String script = Files.readString(
                WEBAPP.resolve("resources/js/pages/monthly_customer_response.js"));
        assertTrue(script.contains("Frog2UI.createDialogController"));
        assertTrue(script.contains("responseDialog.open("));
        assertFalse(script.contains("modal.classList.add('show')"));
        assertTrue(script.contains("form.action = responseForm.action"));
        assertTrue(script.contains("Frog2Csrf.appendTo(form)"));
        assertFalse(script.contains("window.onclick"));
        assertFalse(script.contains("$" + "{"));

        String styles = Files.readString(
                CSS.resolve("pages/monthly_customer_response.css"));
        assertTrue(styles.contains(".monthly-response-page .filter-card"));
        assertTrue(styles.contains(".monthly-response-page .data-table"));
        assertTrue(styles.contains(".monthly-response-page #responseModal"));
        assertFalse(styles.lines().map(String::stripLeading)
                .anyMatch(line -> line.startsWith(".filter-card")
                        || line.startsWith(".data-table")
                        || line.startsWith("#responseModal")));
    }

    @Test
    void errorPagesUseNamespacedExternalStyles() throws Exception {
        for (String pagePath : new String[] {
                "error/403.jsp", "error/404.jsp", "error/409.jsp",
                "error/500.jsp", "error/503.jsp"
        }) {
            String page = Files.readString(WEBAPP.resolve(pagePath));
            assertTrue(page.contains("/resources/css/pages/error.css"), pagePath);
            assertFalse(page.contains("<style"), pagePath);
            assertFalse(page.contains("style=\""), pagePath);
        }

        String errorCss = Files.readString(CSS.resolve("pages/error.css"));
        assertTrue(errorCss.contains(".error-card"));
        assertTrue(errorCss.contains(".error-status"));
        assertFalse(errorCss.contains("\n.card"));
    }

    private static void assertExternalized(
            String pagePath, String scriptPath, boolean checkStyle) throws Exception {
        String page = Files.readString(WEBAPP.resolve(pagePath));
        String script = Files.readString(WEBAPP.resolve(scriptPath));

        assertFalse(page.contains("<script>"), pagePath);
        if (checkStyle) {
            assertFalse(page.contains("<style>"), pagePath);
        }
        assertFalse(script.contains("$" + "{"), scriptPath);
    }
}
