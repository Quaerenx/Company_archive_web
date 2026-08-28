package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DesignPrinciplesContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern TRANSITION_ALL = Pattern.compile(
            "(?i)transition\\s*:\\s*all\\b");

    @Test
    void productShellUsesOneArchiveName() throws Exception {
        String header = read("includes/header.jsp");
        String navigation = read("WEB-INF/includes/header_nav.jspf");
        String footer = read("WEB-INF/includes/footer_content.jspf");
        String login = read("login.jsp");
        String webXml = read("WEB-INF/web.xml");

        assertTrue(header.contains(" | Archive"));
        assertTrue(navigation.contains(
                "/resources/images/archive-logo.svg?v=${frog2AssetVersion}"));
        assertTrue(footer.contains("Archive · 고객 운영 업무공간"));
        assertTrue(login.contains("var=\"productName\" value=\"Archive\""));
        assertTrue(webXml.contains("<display-name>Archive</display-name>"));

        for (String surface : List.of(header, navigation, footer, login)) {
            assertFalse(surface.contains("ARCHIVE"));
            assertFalse(surface.contains("게시판 시스템"));
            assertFalse(surface.contains("Company Inc."));
        }
        for (String errorPage : List.of(
                "error/400.jsp",
                "error/403.jsp",
                "error/404.jsp",
                "error/405.jsp",
                "error/409.jsp",
                "error/500.jsp",
                "error/503.jsp")) {
            assertTrue(read(errorPage).contains(" | Archive</title>"), errorPage);
        }
    }

    @Test
    void operationalDomainsUseTheSharedPageHeader() throws Exception {
        for (String pagePath : List.of(
                "troubleshooting/troubleshooting_list.jsp",
                "troubleshooting/troubleshooting_add.jsp",
                "troubleshooting/troubleshooting_edit.jsp",
                "troubleshooting/troubleshooting_view.jsp",
                "WEB-INF/views/filerepo/list.jsp",
                "WEB-INF/views/filerepo/upload.jsp")) {
            String page = read(pagePath);
            assertTrue(page.contains("tagdir=\"/WEB-INF/tags\""), pagePath);
            assertEquals(1, occurrences(page, "<t:pageHeader>"), pagePath);
        }
    }

    @Test
    void sharedVisualLayersAvoidBroadResetsAndTransitionAll() throws Exception {
        String upload = read("resources/css/pages/upload.css");
        assertFalse(upload.contains(".page-file-upload * {"));
        assertFalse(upload.matches("(?m)^\\s*\\*\\s*\\{"));

        for (String stylesheet : List.of(
                "resources/css/components.css",
                "resources/css/ui-system.css",
                "resources/css/pages/header.css",
                "resources/css/pages/upload.css")) {
            assertFalse(TRANSITION_ALL.matcher(read(stylesheet)).find(), stylesheet);
        }
    }

    @Test
    void sharedUtilitiesAndCardsKeepAccessibilityAndInteractionOptIn() throws Exception {
        String utilities = read("resources/css/utilities.css");
        String components = read("resources/css/components.css");

        assertTrue(utilities.contains(".sr-only {"));
        assertTrue(components.contains(
                ".card:is(a, button, [role=\"link\"], .is-interactive):hover"));
        assertFalse(components.matches("(?s).*\\.card:hover\\s*\\{.*"));
    }

    @Test
    void contextualDialogsUseOneFocusAndEscapeController() throws Exception {
        for (String pagePath : List.of(
                "meeting/meeting_write.jsp",
                "meeting/meeting_edit.jsp")) {
            String page = read(pagePath);
            assertTrue(page.contains("id=\"previewModal\""), pagePath);
            assertTrue(page.contains("role=\"dialog\""), pagePath);
            assertTrue(page.contains("aria-modal=\"true\""), pagePath);
            assertTrue(page.contains("aria-labelledby=\"previewModalTitle\""), pagePath);
            assertTrue(page.contains("aria-hidden=\"true\""), pagePath);
            assertTrue(page.contains("tabindex=\"-1\""), pagePath);
        }

        String monthly = read("mypage/monthly_customer_response.jsp");
        assertTrue(monthly.contains("id=\"responseModal\""));
        assertTrue(monthly.contains("role=\"dialog\""));
        assertTrue(monthly.contains("aria-modal=\"true\""));
        assertTrue(monthly.contains("aria-labelledby=\"modalTitle\""));
        assertTrue(monthly.contains("aria-hidden=\"true\""));
        assertTrue(monthly.contains("tabindex=\"-1\""));

        String systemScript = read("resources/js/ui-system.js");
        assertTrue(systemScript.contains("function createDialogController"));
        assertTrue(systemScript.contains("createDialogController: createDialogController"));
        assertTrue(read("resources/js/pages/meeting_form.js")
                .contains("Frog2UI.createDialogController"));
        assertTrue(read("resources/js/pages/monthly_customer_response.js")
                .contains("Frog2UI.createDialogController"));
    }

    @Test
    void customerEnvironmentTabsPreserveContextForKeyboardUsers() throws Exception {
        String page = read("customers/customers_detail.jsp");
        String script = read("resources/js/pages/customer_detail.js");

        assertTrue(page.contains("role=\"tablist\""));
        assertTrue(page.contains("role=\"tab\""));
        assertTrue(page.contains("aria-controls="));
        assertTrue(page.contains("aria-selected="));
        assertTrue(page.contains("role=\"tabpanel\""));
        assertTrue(page.contains("aria-labelledby="));
        assertTrue(script.contains("aria-selected"));
        assertTrue(script.contains("ArrowLeft"));
        assertTrue(script.contains("ArrowRight"));
        assertTrue(script.contains("Home"));
        assertTrue(script.contains("End"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }

    private static int occurrences(String source, String target) {
        return source.split(Pattern.quote(target), -1).length - 1;
    }
}
