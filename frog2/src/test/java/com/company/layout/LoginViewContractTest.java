package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LoginViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void loginUsesArchiveIdentityAndTheSharedUiSystem() throws Exception {
        String page = read("login.jsp");

        assertTrue(page.contains("<html lang=\"ko\">"));
        assertTrue(page.contains("ARCHIVE"));
        assertFalse(page.contains("WorkSpace"));
        assertFalse(page.contains("Company Inc."));
        assertFalse(page.contains("fonts.googleapis.com"));
        assertFalse(page.contains("dashboard_box.css"));
        assertFalse(page.toLowerCase().contains("ryan"));
        assertTrue(page.contains("/resources/css/tokens.css?v=${initParam.frog2AssetVersion}"));
        assertTrue(page.contains("/resources/css/ui-system.css"));
        assertFalse(read("resources/css/login_style.css").contains("@import"));
        assertHasClasses(firstTag(page, "body"), "ui-system", "login-page");
        assertHasClasses(loginForm(page), "ui-form", "login-form");
        assertTrue(Pattern.compile(
                "<button\\b[^>]*class=\"(?=[^\"]*\\bui-button\\b)"
                        + "(?=[^\"]*\\bbutton--primary\\b)"
                        + "(?=[^\"]*\\bbutton--md\\b)[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(page).find());
    }

    @Test
    void loginPreservesItsPostContractAndPasswordManagerHints() throws Exception {
        String page = read("login.jsp");
        String form = loginForm(page);
        String userId = tagById(page, "userId");
        String password = tagById(page, "password");
        String rememberId = tagById(page, "rememberId");

        assertTrue(form.contains("action=\"login\""));
        assertTrue(form.contains("method=\"post\""));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(userId.contains("name=\"userId\""));
        assertTrue(userId.contains("autocomplete=\"username\""));
        assertTrue(password.contains("name=\"password\""));
        assertTrue(password.contains("autocomplete=\"current-password\""));
        assertTrue(rememberId.contains("name=\"rememberId\""));
        assertFalse(page.contains("autocomplete=\"new-password\""));
    }

    @Test
    void loginErrorAndRememberIdBehaviorRemainAccessibleAndExternal() throws Exception {
        String page = read("login.jsp");
        String error = tagById(page, "login-error");

        assertTrue(error.contains("role=\"alert\""));
        assertTrue(error.contains("aria-live=\"assertive\""));
        assertTrue(error.contains("aria-atomic=\"true\""));
        assertTrue(page.contains("value=\"login-help login-error\""));
        assertTrue(page.contains("aria-describedby=\"${loginDescriptionIds}\""));
        assertTrue(page.contains("/resources/js/pages/login.js"));
        assertFalse(page.contains("localStorage."));

        String script = read("resources/js/pages/login.js");
        assertTrue(script.contains("savedUserId"));
        assertTrue(script.contains("rememberId"));
        assertTrue(script.contains("document.getElementById('userId')"));
        assertTrue(script.contains("document.getElementById('rememberId')"));
        assertFalse(script.toLowerCase().contains("ryan"));
    }

    private static String tagById(String source, String id) {
        Pattern pattern = Pattern.compile(
                "<[^>]+\\bid=\"" + Pattern.quote(id) + "\"[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(source);
        assertTrue(matcher.find(), () -> "Element is missing: #" + id);
        return matcher.group();
    }

    private static String loginForm(String source) {
        Matcher matcher = Pattern.compile(
                "<form\\b(?=[^>]*\\baction=\"login\")"
                        + "(?=[^>]*\\bmethod=\"post\")[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(source);
        assertTrue(matcher.find(), "Login POST form is missing");
        return matcher.group();
    }

    private static String firstTag(String source, String tagName) {
        Matcher matcher = Pattern.compile(
                "<" + Pattern.quote(tagName) + "\\b[^>]*>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(source);
        assertTrue(matcher.find(), () -> "Tag is missing: " + tagName);
        return matcher.group();
    }

    private static void assertHasClasses(String tag, String... classNames) {
        Matcher matcher = Pattern.compile(
                "\\bclass=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(tag);
        assertTrue(matcher.find(), () -> "class attribute is missing: " + tag);
        String classes = " " + matcher.group(1) + " ";
        for (String className : classNames) {
            assertTrue(classes.contains(" " + className + " "), className);
        }
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
