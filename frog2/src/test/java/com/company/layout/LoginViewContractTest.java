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
    void loginUsesMinimalArchiveIdentityAndTheSharedUiSystem() throws Exception {
        String page = read("login.jsp");
        String styles = read("resources/css/login_style.css");

        assertTrue(page.contains("<html lang=\"ko\">"));
        assertTrue(page.contains("var=\"productName\" value=\"Archive\""));
        assertTrue(page.contains("content=\"Archive 로그인\""));
        assertTrue(page.contains("class=\"login-brand-logo\""));
        assertTrue(page.contains("/resources/images/archive-primary-logo.svg"));
        assertTrue(page.contains("width=\"1119\""));
        assertTrue(page.contains("height=\"288\""));
        assertTrue(page.contains("alt=\"${productName}\""));
        assertFalse(page.contains("ARCHIVE"));
        assertFalse(page.contains("WorkSpace"));
        assertFalse(page.contains("Company Inc."));
        assertFalse(page.contains("fonts.googleapis.com"));
        assertFalse(page.contains("dashboard_box.css"));
        assertFalse(page.toLowerCase().contains("ryan"));
        assertTrue(page.contains("/resources/css/tokens.css?v=${initParam.frog2AssetVersion}"));
        assertTrue(page.contains("/resources/css/ui-system.css"));
        assertTrue(page.indexOf("/resources/css/tokens.css")
                < page.indexOf("/resources/css/ui-system.css"));
        assertTrue(page.indexOf("/resources/css/ui-system.css")
                < page.indexOf("/resources/css/login_style.css"));
        assertFalse(styles.contains("@import"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-brand\\s*\\{[^}]*"
                        + "justify-content:\\s*center;.*"));
        assertFalse(page.contains("고객 운영 업무공간"));
        assertFalse(page.contains("업무 계정으로 로그인"));
        assertFalse(page.contains("한곳에서 확인하세요"));
        assertFalse(page.contains("승인된 사내 계정"));
        assertFalse(page.contains("아이디 저장"));
        assertFalse(page.contains("로그인 폼으로 건너뛰기"));
        assertFalse(page.contains("class=\"login-footer\""));
        assertTrue(page.contains(
                "class=\"login-background app-ambient-background\""));
        assertTrue(page.contains("app-ambient-background"));
        assertTrue(page.contains("data-app-ambient-background"));
        assertTrue(page.contains("has-ambient-background"));
        assertFalse(page.contains("data-glitter-wrap"));
        assertFalse(page.contains("graphitePreview"));
        assertFalse(page.contains("data-login-preview="));
        assertFalse(page.contains("data-glitter-preset="));
        assertTrue(page.contains("aria-hidden=\"true\""));
        assertTrue(page.contains("/resources/css/ambient-background.css?v=${initParam.frog2AssetVersion}"));
        assertTrue(page.contains("/resources/js/ambient-background.js?v=${initParam.frog2AssetVersion}"));
        assertFalse(page.contains("/resources/js/pages/login.js"));
        assertTrue(styles.contains("background: var(--color-login-background);"));
        assertTrue(styles.contains("color: var(--color-login-particle);"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-form\\s*\\{[^}]*"
                        + "gap:\\s*var\\(--space-12\\);.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-submit\\s*\\{[^}]*"
                        + "background:\\s*var\\(--color-surface-inverse\\);.*"));
        assertTrue(styles.contains(".login-page .login-form input::placeholder"));
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

        assertTrue(form.contains("action=\"login\""));
        assertTrue(form.contains("method=\"post\""));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(userId.contains("name=\"userId\""));
        assertTrue(userId.contains("autocomplete=\"username\""));
        assertTrue(password.contains("name=\"password\""));
        assertTrue(password.contains("autocomplete=\"current-password\""));
        assertTrue(userId.contains("placeholder=\"아이디\""));
        assertTrue(password.contains("placeholder=\"비밀번호\""));
        assertTrue(page.contains("class=\"login-field-label\" for=\"userId\""));
        assertTrue(page.contains("class=\"login-field-label\" for=\"password\""));
        assertFalse(page.contains("rememberId"));
        assertFalse(page.contains("autocomplete=\"new-password\""));
    }

    @Test
    void loginErrorRemainsAccessibleWithoutStaticSupportingCopy() throws Exception {
        String page = read("login.jsp");
        String error = tagById(page, "login-error");

        assertTrue(error.contains("role=\"alert\""));
        assertTrue(error.contains("aria-live=\"assertive\""));
        assertTrue(error.contains("aria-atomic=\"true\""));
        assertTrue(page.contains("aria-invalid=\"${not empty errorMessage}\""));
        assertFalse(page.contains("login-help"));
        assertFalse(page.contains("loginDescriptionIds"));
        assertFalse(page.contains("localStorage."));
    }

    @Test
    void loginBackgroundUsesTheSharedLowPowerAmbientAnimation()
            throws Exception {
        String script = read("resources/js/ambient-background.js");

        assertTrue(script.contains("var DEFAULT_PARTICLE_COUNT = 36;"));
        assertTrue(script.contains("var LOW_POWER_PARTICLE_COUNT = 24;"));
        assertTrue(script.contains("var TARGET_FRAME_RATE = 30;"));
        assertTrue(script.contains("var SPEED = 0.18;"));
        assertTrue(script.contains("var DENSITY = 36;"));
        assertTrue(script.contains("var STAR_SIZE = 4;"));
        assertTrue(script.contains("var FOCAL_DEPTH = 21;"));
        assertTrue(script.contains("var BRIGHTNESS = 9;"));
        assertTrue(script.contains("var GLITTER_INTENSITY = 0.03;"));
        assertTrue(script.contains("var TRAIL_AMOUNT = 30;"));
        assertTrue(script.contains("requestAnimationFrame"));
        assertTrue(script.contains("ResizeObserver"));
        assertTrue(script.contains("(min-width: 1051px)"));
        assertTrue(script.contains("prefers-reduced-motion: reduce"));
        assertTrue(script.contains("document.hidden"));
        assertFalse(script.contains("localStorage"));
        assertFalse(script.contains("from \"react\""));
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
