package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class UiDesignSystemContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern ALERT_TAG = Pattern.compile(
            "<div\\b[^>]*\\bclass=\"([^\"]*\\balert\\b[^\"]*)\"[^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Test
    void semanticTokensAndCanonicalComponentsAreAvailable() throws Exception {
        String tokens = read("resources/css/tokens.css");
        for (String token : List.of(
                "--color-primary:",
                "--color-secondary:",
                "--color-danger:",
                "--color-success:",
                "--color-warning-accent:",
                "--color-warning-text:",
                "--color-success-strong:",
                "--color-danger-strong:",
                "--color-warning-strong:",
                "--color-info-strong:",
                "--color-text:",
                "--color-text-muted:",
                "--color-border:",
                "--color-surface-edge:",
                "--color-border-strong:",
                "--color-surface:",
                "--color-surface-muted:",
                "--color-surface-selected:",
                "--color-focus:",
                "--control-height-sm:",
                "--control-height-md:",
                "--font-size-sm:",
                "--font-size-md:",
                "--radius-md:",
                "--shadow-md:")) {
            assertTrue(tokens.contains(token), token);
        }
        assertTrue(tokens.contains("'Noto Sans KR'"));
        assertTrue(tokens.contains("'Apple SD Gothic Neo'"));
        assertTrue(tokens.contains("'Malgun Gothic'"));
        assertTrue(tokens.contains(
                "--color-surface-hover: var(--palette-ambient);"));
        assertTrue(tokens.contains(
                "--color-surface-selected: var(--palette-brand-subtle);"));
        assertTrue(tokens.contains(
                "--color-surface-edge: var(--palette-surface-edge);"));
        assertFalse(tokens.contains("--color-surface-edge: color-mix("));

        String styles = read("resources/css/ui-system.css");
        for (String selector : List.of(
                ".ui-button",
                ".button--primary",
                ".button--secondary",
                ".button--danger",
                ".button--ghost",
                ".button--ghost-danger",
                ".button--sm",
                ".button--md",
                ".ui-touch-target",
                "form.ui-form",
                ".ui-form-card",
                ".ui-form-layout .section-title",
                ".ui-form-layout .form-row",
                ".ui-form-layout .button-group",
                ".ui-detail .detail-section",
                ".ui-detail .detail-grid",
                ".ui-detail .detail-item",
                ".ui-detail .detail-label",
                ".ui-detail .detail-value",
                ".ui-field-error",
                ".ui-alert",
                ".ui-status",
                ".ui-toast-region",
                ".ui-badge",
                ".ui-badge--neutral",
                ".ui-badge--success",
                ".ui-badge--warning",
                ".ui-badge--danger",
                ".ui-work-surface",
                ".ui-work-surface--padded",
                ".ui-section-header",
                ".ui-section-header--compact",
                ".ui-section-actions",
                ".ui-section-title",
                ".ui-section-body",
                ".ui-subgroup",
                ".ui-data-row",
                ".ui-table",
                ".content-shell")) {
            assertTrue(styles.contains(selector), selector);
        }
        assertTrue(styles.contains(
                "body.ui-system [hidden] {\n    display: none;\n}"));
        assertTrue(tokens.contains("--page-content-max-width: 1018px"));
        assertTrue(tokens.contains("--page-content-gutter: var(--space-16)"));
        assertTrue(tokens.contains(
                "--page-content-total-gutter: calc(var(--page-content-gutter) * 2)"));
        assertTrue(tokens.contains("--work-surface-padding: var(--space-24)"));
        assertTrue(tokens.contains("--section-content-gap: var(--space-24)"));
        assertTrue(tokens.contains("--item-content-gap: var(--space-12)"));
        assertTrue(styles.contains("background: var(--background)"));
        assertTrue(styles.contains("width: calc(100% - var(--page-content-total-gutter))"));
        assertTrue(styles.contains("padding-block: var(--space-32)"));
        assertTrue(styles.contains(
                "--page-content-total-gutter: calc(var(--page-content-gutter) * 2)"));
        assertTrue(styles.contains("padding-inline: 0"));
        assertTrue(styles.contains(":focus-visible"));
        assertTrue(styles.contains("min-block-size: 44px"));
        assertTrue(styles.contains("prefers-reduced-motion: reduce"));
        assertTrue(styles.contains(".ui-button.is-loading::before"));
        assertTrue(styles.contains("padding: var(--space-24)"));
        assertTrue(styles.contains(
                "border-block-end: 1px solid var(--color-border)"));
        assertTrue(styles.contains("animation: none"));
        assertFalse(styles.contains("!important"));
        assertFalse(styles.contains("transition: all"));
        assertTrue(styles.matches(
                "(?s).*\\.ui-system \\.ui-button\\s*\\{[^}]*font-weight:\\s*500;.*"));
        assertTrue(styles.matches(
                "(?s).*form\\.ui-form :is\\(\\.form-group > label, \\.ui-label\\)"
                        + "\\s*\\{[^}]*font-weight:\\s*500;.*"));
        assertFalse(read("resources/css/pages/maintenance.css")
                .contains("font-weight: 650"));
        assertFalse(read("resources/css/utilities.css")
                .contains("font-weight: 300"));
        assertFalse(read("resources/css/utilities.css")
                .contains("font-weight: lighter"));

        for (String pageStyle : List.of(
                "resources/css/components.css",
                "resources/css/pages/dashboard.css",
                "resources/css/pages/download.css",
                "resources/css/pages/maintenance.css",
                "resources/css/pages/maintenance_cards.css",
                "resources/css/pages/maintenance_history.css",
                "resources/css/pages/monthly_customer_response.css",
                "resources/css/pages/mypage.css",
                "resources/css/pages/mypage_hosts.css",
                "resources/css/pages/troubleshooting_form.css",
                "resources/css/pages/troubleshooting_list.css",
                "resources/css/pages/troubleshooting_view.css",
                "resources/css/pages/upload.css")) {
            assertFalse(read(pageStyle).contains(
                    "max-width: var(--page-content-max-width)"), pageStyle);
        }
    }

    @Test
    void workspaceHierarchyAndRowNavigationAreSharedAcrossDomains()
            throws Exception {
        String styles = read("resources/css/ui-system.css");
        String script = read("resources/js/ui-system.js");

        assertTrue(styles.contains(":where(.ui-work-surface, .table-container)"));
        assertTrue(styles.contains(
                ".ui-system .table-container:not(.ui-work-surface)"));
        assertFalse(styles.contains(".ui-system .table-container {"));
        assertTrue(styles.contains(".ui-section-header--flush"));
        assertTrue(styles.contains(".ui-section-body--flush"));
        assertTrue(styles.contains("@media (min-width: 769px)"));
        assertTrue(styles.contains(".ui-system .is-table-sticky-ready"));
        assertTrue(styles.contains("@supports (overflow: clip)"));
        assertTrue(styles.contains(
                ".ui-table-wrap[data-ui-scrollable=\"false\"]"));
        assertTrue(styles.contains(
                "top: var(--table-sticky-offset, 0px);"));
        assertTrue(styles.contains("z-index: var(--z-table-sticky);"));
        assertTrue(styles.contains(
                "box-shadow: var(--shadow-table-sticky);"));
        assertTrue(read("resources/css/tokens.css").contains(
                "--shadow-table-sticky:"));
        assertTrue(styles.contains(
                ".ui-data-row[data-detail-url]:hover {\n"
                        + "    background: var(--color-primary-subtle);"));
        assertFalse(styles.contains(
                ".ui-table tbody tr:hover {"));
        assertFalse(styles.matches(
                "(?s).*\\.ui-data-table thead th\\s*\\{[^}]*position:\\s*sticky;.*"));
        assertTrue(script.contains(".ui-data-row[data-detail-url]"));
        assertTrue(script.contains("--table-sticky-offset"));
        assertTrue(script.contains("is-table-sticky-ready"));
        assertTrue(script.contains(
                ".ui-work-surface, [data-ui-table-surface]"));
        assertTrue(script.contains("region.dataset.uiScrollable"));
        assertTrue(script.contains("!scrollable"));
        assertTrue(script.contains("window.location.assign(row.dataset.detailUrl)"));
        assertTrue(script.contains("event.defaultPrevented"));
        assertTrue(script.contains("window.getSelection()"));
        assertTrue(script.contains("selection.isCollapsed"));
        assertTrue(script.contains("selection.containsNode(element, true)"));
        assertTrue(script.contains("element.contains(selection.anchorNode)"));
        assertTrue(script.contains("isSelectingText"));
        assertTrue(script.contains("row.getAttribute('aria-disabled') === 'true'"));

        for (String page : List.of(
                "customers/customers_list.jsp",
                "meeting/meeting_list.jsp",
                "troubleshooting/troubleshooting_list.jsp")) {
            String source = read(page);
            assertTrue(source.contains("ui-data-row"), page);
            assertTrue(source.contains("data-detail-url"), page);
        }

        assertTrue(read("customers/customers_list.jsp").contains(
                "customer-list-panel ui-work-surface"));
        for (String page : List.of(
                "meeting/meeting_list.jsp",
                "troubleshooting/troubleshooting_list.jsp")) {
            assertTrue(read(page).contains("class=\"ui-work-surface\""), page);
        }

        for (String page : List.of(
                "dashboard.jsp",
                "customers/customers_detail.jsp",
                "maintenance/maintenance_history.jsp",
                "meeting/meeting_view.jsp",
                "troubleshooting/troubleshooting_view.jsp",
                "WEB-INF/views/filerepo/list.jsp",
                "mypage/mypage.jsp")) {
            assertTrue(read(page).contains("ui-work-surface"), page);
        }

        for (String page : List.of(
                "mypage/monthly_customer_response.jsp",
                "vm_hosts/list.jsp",
                "WEB-INF/includes/mypage/host_manager.jspf")) {
            assertTrue(read(page).contains("data-ui-table-surface"), page);
        }
    }

    @Test
    void legacyButtonVisualClassesAreRetiredFromViews() throws Exception {
        List<String> legacyClasses = List.of(
                "button",
                "btn",
                "btn-primary",
                "btn-secondary",
                "btn-success",
                "btn-danger",
                "btn-warning",
                "btn-cancel",
                "btn-sm",
                "btn-min",
                "add-button");
        Pattern classAttribute = Pattern.compile("class=\"([^\"]*)\"");

        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(Files::isRegularFile)
                    .filter(UiDesignSystemContractTest::isViewTemplate)
                    .toList()) {
                Matcher matcher = classAttribute.matcher(Files.readString(path));
                while (matcher.find()) {
                    List<String> classes = List.of(matcher.group(1).trim().split("\\s+"));
                    for (String legacyClass : legacyClasses) {
                        assertFalse(classes.contains(legacyClass),
                                () -> path + " still uses " + legacyClass);
                    }
                }
            }
        }

        String base = read("resources/css/base.css");
        String components = read("resources/css/components.css");
        assertFalse(base.contains(".button {"));
        for (String selector : List.of(
                ".btn {", ".btn-primary", ".btn-secondary", ".btn-success",
                ".btn-danger", ".btn-warning", ".btn-cancel", ".btn-min",
                ".add-button")) {
            assertFalse(components.contains(selector), selector);
        }
    }

    @Test
    void repeatedSectionHeadersUseOneTagContract() throws Exception {
        String tag = read("WEB-INF/tags/sectionHeader.tag");
        assertTrue(tag.contains("ui-section-header"));
        assertTrue(tag.contains("ui-section-header--flush"));
        assertTrue(tag.contains("ui-section-header--compact"));
        assertTrue(tag.contains("ui-section-actions"));
        assertTrue(tag.contains("<jsp:invoke fragment=\"title\" />"));
        assertTrue(tag.contains("<jsp:invoke fragment=\"actions\" />"));

        String maintenanceHistory = read("maintenance/maintenance_history.jsp");
        String maintenanceCards = read("maintenance/maintenance_cards.jsp");
        String meetingView = read("meeting/meeting_view.jsp");
        assertTrue(maintenanceHistory.contains(
                "<t:sectionHeader className=\"history-header\">"));
        assertTrue(maintenanceCards.contains(
                "<t:sectionHeader className=\"inspector-header\" compact=\"true\">"));
        assertTrue(meetingView.contains(
                "<t:sectionHeader className=\"content-header\">"));
        assertTrue(meetingView.contains(
                "<t:sectionHeader className=\"comments-header\">"));
        assertFalse(maintenanceHistory.contains(
                "class=\"history-header ui-section-header\""));
        assertFalse(meetingView.contains(
                "class=\"content-header ui-section-header\""));
    }

    @Test
    void sharedUtilitiesAndHeaderUseSemanticTokens() throws Exception {
        String utilities = read("resources/css/utilities.css");
        String header = read("resources/css/pages/header.css");
        String components = read("resources/css/components.css");
        String base = read("resources/css/base.css");

        for (String token : List.of(
                "--color-success-strong",
                "--color-danger-strong",
                "--color-warning-strong",
                "--color-info-strong")) {
            assertTrue(utilities.contains("var(" + token + ")"), token);
        }
        assertFalse(utilities.matches(
                "(?s).*\\.(?:text|bg|border)-(?:success|danger|warning|info)"
                        + "[^{]*\\{[^}]*#[0-9a-fA-F]{3,8}.*"));
        assertTrue(header.contains("var(--color-navigation-surface)"));
        assertTrue(header.contains("var(--color-navigation-border)"));
        assertTrue(header.contains("var(--shadow-navigation)"));
        assertTrue(header.contains("box-shadow: none"));
        assertFalse(header.contains("box-shadow: var(--shadow-header)"));
        assertTrue(components.contains("var(--color-primary-ring)"));
        assertFalse(base.contains("@media (max-width: 992px)"));
        assertFalse(base.contains("@media (max-width: 991.98px)"));
        assertTrue(base.contains("@media (max-width: 1024px)"));
    }

    @Test
    void domainFormsUseTheSharedLayoutWithoutPageLevelCopies() throws Exception {
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
            assertTrue(source.contains("ui-form-card"), page);
            assertTrue(source.contains("ui-form ui-form-layout"), page);
        }

        for (String pageStyle : List.of(
                "resources/css/pages/customers.css",
                "resources/css/pages/maintenance.css",
                "resources/css/pages/meeting_form.css",
                "resources/css/pages/troubleshooting_form.css")) {
            String styles = read(pageStyle);
            assertFalse(styles.matches(
                            "(?s).*\\.form-row\\s*\\{[^}]*display\\s*:\\s*flex.*"),
                    pageStyle);
        }
    }

    @Test
    void remainingOperationalSurfacesUseCanonicalControls() throws Exception {
        String myPage = read("mypage/mypage.jsp");
        String myPageProfile = read(
                "WEB-INF/includes/mypage/profile_summary.jspf");
        String myPageHosts = read(
                "WEB-INF/includes/mypage/host_manager.jspf");
        assertTrue(myPageHosts.contains(
                "ui-button button--primary button--md"));
        assertTrue(myPageProfile.contains(
                "ui-button button--secondary button--sm"));
        assertFalse(myPageProfile.contains("btn btn-secondary"));

        String password = read("mypage/change_password.jsp");
        String profile = read("mypage/edit_profile.jsp");
        for (String page : List.of(password, profile)) {
            assertTrue(page.contains("class=\"ui-form\""));
            assertTrue(page.contains("data-ui-submit-lock=\"auto\""));
            assertTrue(page.contains("ui-button button--primary button--md"));
            assertTrue(page.contains("ui-button button--secondary button--md"));
        }

        String monthly = read("mypage/monthly_customer_response.jsp");
        assertTrue(monthly.contains("class=\"filter-form ui-form\""));
        assertTrue(monthly.contains(
                "class=\"monthly-response-table ui-table ui-data-table\""));
        assertTrue(monthly.contains("class=\"ui-table-wrap\""));
        assertTrue(monthly.contains("id=\"responseForm\""));
        assertTrue(monthly.contains(
                "${responseDateValue} ${responseEntry.customerName} 응대 기록 수정"));
        assertTrue(monthly.contains(
                "${responseDateValue} ${responseEntry.customerName} 응대 기록 삭제"));

        String vmHosts = read("vm_hosts/list.jsp");
        assertTrue(vmHosts.contains("class=\"vm-form ui-form\""));
        assertTrue(vmHosts.contains("class=\"vm-table-wrap ui-table-wrap\""));
        assertTrue(vmHosts.contains("class=\"vm-table ui-table\""));
        assertTrue(vmHosts.contains("class=\"js-vm-host-delete ui-form\""));

        assertTrue(myPageHosts.contains(
                "class=\"vm-table-wrap ui-table-wrap\""));
        assertTrue(myPageHosts.contains("class=\"vm-table ui-table\""));
    }

    @Test
    void customerAndTroubleshootingDetailsUseSharedFieldLayout() throws Exception {
        String customerView = read("customers/_detail_sections.jspf");
        String customerEdit = read("customers/customers_detail_edit.jsp");
        String troubleshooting = read("troubleshooting/troubleshooting_view.jsp");
        assertTrue(customerView.contains("class=\"environment-detail ui-detail\""));
        assertTrue(customerEdit.contains(
                "class=\"detail-container ui-detail ui-work-surface\""));
        assertTrue(troubleshooting.contains(
                "class=\"detail-container ui-detail troubleshooting-report ui-work-surface\""));

        String troubleshootingStyles = read(
                "resources/css/pages/troubleshooting_view.css");
        assertFalse(troubleshootingStyles.contains(
                ".troubleshooting-detail .detail-label"));
        assertFalse(troubleshootingStyles.contains(
                ".troubleshooting-detail .detail-section-title"));
    }

    @Test
    void sharedStylesLoadBeforePageStylesAndSharedScriptsBeforePageScripts()
            throws Exception {
        String coreStyles = read("WEB-INF/includes/core_styles.jspf");
        String header = read("includes/header.jsp");
        int components = coreStyles.indexOf("/resources/css/components.css");
        int uiStyle = coreStyles.indexOf("/resources/css/ui-system.css");
        int utilities = coreStyles.indexOf("/resources/css/utilities.css");
        int pageStyleSlot = header.indexOf("not empty pageCss");
        int headEnd = header.indexOf("</head>");
        assertTrue(components >= 0);
        assertTrue(uiStyle > components);
        assertTrue(utilities > uiStyle);
        assertTrue(pageStyleSlot >= 0);
        assertTrue(headEnd > pageStyleSlot);
        assertFalse(header.contains("/resources/css/ui-system.css"));
        assertTrue(header.contains("<body class=\"ui-system "));

        String footer = read("includes/footer.jsp");
        int uiStatus = footer.indexOf("id=\"ui-status-region\"");
        int uiToastPolite = footer.indexOf("id=\"ui-toast-region-polite\"");
        int uiToastAssertive = footer.indexOf("id=\"ui-toast-region-assertive\"");
        int uiScript = footer.indexOf("/resources/js/ui-system.js");
        int pageScriptSlot = footer.indexOf("not empty pageScript");
        assertTrue(uiStatus >= 0);
        assertTrue(uiToastPolite > uiStatus);
        assertTrue(uiToastAssertive > uiToastPolite);
        assertTrue(uiScript > uiToastAssertive);
        assertTrue(pageScriptSlot > uiScript);
        assertTrue(footer.contains("aria-live=\"polite\""));
        assertTrue(footer.contains("aria-live=\"assertive\""));
        assertTrue(footer.contains("aria-atomic=\"false\""));
        assertTrue(footer.contains("aria-relevant=\"additions\""));
    }

    @Test
    void submitLockAndAccessibleFeedbackHaveExplicitContracts() throws Exception {
        String script = read("resources/js/ui-system.js");
        assertTrue(script.contains("event.defaultPrevented"));
        assertTrue(script.contains("form.checkValidity()"));
        assertTrue(script.contains("data-ui-submit-lock=\"auto\""));
        assertTrue(script.contains("event.submitter"));
        assertTrue(script.contains("MAX_TOASTS_PER_REGION"));
        assertTrue(script.contains("originalButtonState"));
        assertTrue(script.contains("aria-busy"));
        assertTrue(script.contains("aria-invalid"));
        assertTrue(script.contains("aria-describedby"));
        assertTrue(script.contains("role', 'alert'"));
        assertTrue(script.contains("window.Frog2UI"));
        assertTrue(script.contains("pageshow"));

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
                "troubleshooting/troubleshooting_view.jsp")) {
            String source = read(page);
            assertTrue(source.matches(
                            "(?s).*class=\"[^\"]*\\bui-form\\b[^\"]*\".*"),
                    page);
            assertTrue(source.contains("data-ui-submit-lock=\"auto\""), page);
        }

        String upload = read("WEB-INF/views/filerepo/upload.jsp");
        assertTrue(upload.contains("class=\"ui-form\""));
        assertTrue(upload.contains("data-ui-submit-lock=\"manual\""));
        assertTrue(upload.contains("role=\"status\""));
        assertTrue(upload.contains("aria-live=\"polite\""));

        String comments = read("meeting/meeting_view.jsp");
        assertTrue(comments.contains("id=\"commentForm\" class=\"ui-form\""));
        assertFalse(comments.contains("id=\"commentForm\" data-ui-submit-lock"));
    }

    @Test
    void migratedServerMessagesUseAccessibleAlertVariants() throws Exception {
        int alertCount = 0;
        for (String directory : List.of(
                "customers",
                "maintenance",
                "meeting",
                "troubleshooting",
                "WEB-INF/views/filerepo",
                "WEB-INF/tags")) {
            try (var paths = Files.walk(WEBAPP.resolve(directory))) {
                for (Path path : paths.filter(Files::isRegularFile)
                        .filter(file -> {
                            String name = file.getFileName().toString();
                            return name.endsWith(".jsp") || name.endsWith(".tag");
                        })
                        .toList()) {
                    String source = Files.readString(path);
                    Matcher matcher = ALERT_TAG.matcher(source);
                    while (matcher.find()) {
                        alertCount++;
                        String classes = matcher.group(1);
                        String tag = matcher.group();
                        assertTrue(classes.contains("ui-alert"), path.toString());
                        if (classes.contains("ui-alert--danger")) {
                            assertTrue(tag.contains("role=\"alert\""), path.toString());
                            assertTrue(tag.contains("aria-atomic=\"true\""), path.toString());
                        }
                        if (classes.contains("ui-alert--success")
                                || classes.contains("ui-alert--warning")) {
                            assertTrue(tag.contains("role=\"status\""), path.toString());
                            assertTrue(tag.contains("aria-live=\"polite\""), path.toString());
                        }
                    }
                }
            }
        }
        assertTrue(alertCount >= 12, "unexpected alert coverage: " + alertCount);
    }

    @Test
    void pageStylesDoNotRedefineCanonicalAlertVisuals() throws Exception {
        String canonical = read("resources/css/ui-system.css");
        for (String tone : List.of("success", "danger", "warning", "info", "neutral")) {
            assertTrue(canonical.contains(".ui-alert--" + tone), tone);
        }

        for (String pageStyle : List.of(
                "resources/css/pages/customers.css",
                "resources/css/pages/maintenance_cards.css",
                "resources/css/pages/maintenance_history.css",
                "resources/css/pages/meeting_view.css",
                "resources/css/pages/monthly_customer_response.css",
                "resources/css/pages/troubleshooting_list.css",
                "resources/css/pages/troubleshooting_view.css",
                "resources/css/pages/upload.css")) {
            String styles = read(pageStyle);
            assertFalse(styles.matches(
                            "(?s).*\\.(?:alert|ui-alert)(?:-|\\b)[^{]*\\{.*"),
                    pageStyle);
        }
    }

    @Test
    void everyRequestedDomainUsesTheCanonicalSystem() throws Exception {
        assertCanonicalUsage("customers/customers_add.jsp");
        assertCanonicalUsage("maintenance/maintenance_add.jsp");
        assertCanonicalUsage("meeting/meeting_write.jsp");
        assertCanonicalUsage("troubleshooting/troubleshooting_add.jsp");
        assertCanonicalUsage("WEB-INF/views/filerepo/upload.jsp");

        String uploadScript = read("resources/js/pages/file_repository_upload.js");
        assertTrue(uploadScript.contains("Frog2UI.setButtonLoading"));
        assertTrue(uploadScript.contains("Frog2UI.setStatus"));
        assertTrue(uploadScript.contains("new FormData(form)"));
        assertTrue(uploadScript.contains("X-CSRF-Token"));

        for (String page : List.of(
                "customers/customers_detail.jsp",
                "customers/customers_list.jsp",
                "meeting/meeting_write.jsp",
                "meeting/meeting_edit.jsp")) {
            assertTrue(read(page).contains("ui-touch-target"), page);
        }
    }

    private static void assertCanonicalUsage(String path) throws Exception {
        String source = read(path);
        assertTrue(source.contains("ui-form"), path);
        assertTrue(source.contains("ui-button"), path);
        assertTrue(source.contains("button--primary"), path);
        assertTrue(source.contains("button--secondary"), path);
    }

    private static boolean isViewTemplate(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith(".jsp")
                || fileName.endsWith(".jspf")
                || fileName.endsWith(".tag");
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
