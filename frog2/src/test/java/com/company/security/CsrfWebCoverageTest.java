package com.company.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CsrfWebCoverageTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern POST_FORM = Pattern.compile(
            "<form\\b[^>]*\\bmethod\\s*=\\s*[\"']post[\"'][^>]*>(.*?)</form>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Test
    void everyStaticPostFormContainsCsrfInput() throws Exception {
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".jsp")).toList()) {
                String source = Files.readString(path);
                Matcher forms = POST_FORM.matcher(source);
                while (forms.find()) {
                    String formBody = forms.group(1);
                    assertTrue(
                            formBody.contains("csrf_input.jspf") || formBody.contains("name=\"_csrf\""),
                            () -> "POST form lacks CSRF input: " + path);
                }
            }
        }
    }

    @Test
    void dynamicPostsUseSharedCsrfHelper() throws Exception {
        for (String relative : List.of(
                "resources/js/pages/customer_detail.js",
                "resources/js/pages/monthly_customer_response.js")) {
            String source = Files.readString(WEBAPP.resolve(relative));
            assertTrue(source.contains("Frog2Csrf.appendTo(form)"), relative);
        }

        String meeting = Files.readString(
                WEBAPP.resolve("resources/js/pages/meeting_view.js"));
        assertTrue(meeting.contains(
                "parameters.set('_csrf', window.Frog2Csrf.token())"));
    }

    @Test
    void filterAndPostOnlyLogoutAreConfigured() throws Exception {
        String webXml = Files.readString(WEBAPP.resolve("WEB-INF/web.xml"));
        assertTrue(webXml.contains("com.company.security.CsrfFilter"));
        assertTrue(webXml.contains("<error-code>403</error-code>"));

        String logout = Files.readString(Path.of(
                "src/main/java/com/company/controller/LogoutServlet.java"));
        int getStart = logout.indexOf("protected void doGet");
        int postStart = logout.indexOf("protected void doPost");
        assertTrue(getStart >= 0 && postStart > getStart);
        assertFalse(logout.substring(getStart, postStart).contains("invalidate()"));
        assertTrue(logout.substring(postStart).contains("invalidate()"));

        String headerScript = Files.readString(
                WEBAPP.resolve("resources/js/header_nav.js"));
        assertTrue(headerScript.contains("form.method = 'POST'"));
        assertTrue(headerScript.contains("Frog2Csrf.appendTo(form)"));
    }
}