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
        String tokens = read("resources/css/tokens.css");

        assertTrue(page.contains("<html lang=\"ko\">"));
        assertTrue(page.contains("var=\"productName\" value=\"Archive\""));
        assertTrue(page.contains("content=\"Archive 로그인\""));
        assertTrue(page.contains("class=\"login-brand-logo\""));
        assertTrue(page.contains(
                "/resources/images/archive-logo.svg?v=${initParam.frog2AssetVersion}"));
        assertTrue(page.contains("width=\"3664\""));
        assertTrue(page.contains("height=\"1480\""));
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
        assertTrue(page.contains("아이디 저장"));
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
        assertTrue(page.contains(
                "/resources/js/pages/login.js?v=${initParam.frog2AssetVersion}"));
        assertTrue(styles.contains("background: var(--color-login-background);"));
        assertTrue(styles.contains("color: var(--color-login-particle);"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-form\\s*\\{[^}]*"
                        + "gap:\\s*var\\(--space-12\\);.*"));
        assertTrue(styles.matches(
                "(?s).*body\\.login-page\\s*\\{[^}]*"
                        + "min-height:\\s*100vh;[^}]*"
                        + "min-height:\\s*100dvh;.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-shell\\s*\\{[^}]*"
                        + "inline-size:\\s*min\\(100%, 408px\\);.*"));
        assertTrue(styles.matches(
                "(?s).*body\\.login-page::after\\s*\\{[^}]*"
                        + "background:\\s*radial-gradient\\([^}]*"
                        + "var\\(--color-login-halo\\)[^}]*"
                        + "inset:\\s*0;.*"));
        assertFalse(styles.contains(".login-shell::before"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-card\\s*\\{[^}]*"
                        + "background:\\s*var\\(--color-login-card\\);[^}]*"
                        + "border:\\s*1px solid var\\(--color-login-card-border\\);[^}]*"
                        + "border-radius:\\s*var\\(--radius-2xl\\);[^}]*"
                        + "box-shadow:\\s*var\\(--shadow-login\\);[^}]*"
                        + "backdrop-filter:\\s*blur\\(var\\(--login-card-blur\\)\\);[^}]*"
                        + "padding:\\s*var\\(--space-40\\) var\\(--space-40\\) "
                        + "var\\(--space-32\\);.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-header\\s*\\{[^}]*"
                        + "margin-block-end:\\s*var\\(--space-24\\);.*"));
        // 크롭 우회는 걷어냈다. SVG viewBox가 그림 경계에 맞춰져 로고가 그대로
        // 카드 폭을 채우고, 헤더 로고와 종횡비가 같아져 전환 모프가 깨끗해진다.
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-brand\\s*\\{[^}]*"
                        + "inline-size:\\s*100%;.*"));
        assertFalse(styles.contains("aspect-ratio: 2.49 / 1;"));
        assertFalse(styles.contains("transform: scale(1.12);"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-brand-logo\\s*\\{[^}]*"
                        + "block-size:\\s*auto;[^}]*"
                        + "inline-size:\\s*100%;.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-form \\.form-group\\s*\\{[^}]*"
                        + "position:\\s*relative;.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page #loginForm \\.login-field-label\\s*\\{[^}]*"
                        + "line-height:\\s*var\\(--line-height-tight\\);.*"));
        assertTrue(styles.matches(
                "(?s).*#loginForm \\.form-group > input:not\\(:placeholder-shown\\) "
                        + "\\+ \\.login-field-label\\s*\\{[^}]*"
                        + "top:\\s*var\\(--space-4\\);.*"));
        assertTrue(styles.contains(".login-page #loginForm .form-group > input {"));
        assertTrue(styles.contains(
                "#loginForm .form-group > input:focus + .login-field-label"));
        assertTrue(styles.contains(
                "#loginForm .form-group > input:not(:placeholder-shown) + .login-field-label {"));
        assertTrue(styles.contains(
                "#loginForm .form-group > input:-webkit-autofill + .login-field-label {"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page #loginForm \\.form-group > input\\s*\\{[^}]*"
                        + "background:\\s*var\\(--color-login-field\\);[^}]*"
                        + "border-color:\\s*var\\(--color-login-field-border\\);[^}]*"
                        + "border-radius:\\s*var\\(--radius-xl\\);[^}]*"
                        + "min-block-size:\\s*52px;.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page #userId\\s*\\{[^}]*"
                        + "font-family:\\s*var\\(--font-base\\);[^}]*"
                        + "font-weight:\\s*500;[^}]*"
                        + "letter-spacing:\\s*0;.*"));
        assertFalse(styles.contains(".login-page #password {"));
        assertTrue(styles.contains(
                ".login-page #loginForm .form-group > input:focus-visible {"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-submit\\s*\\{[^}]*"
                        + "background:\\s*var\\(--color-login-action\\);[^}]*"
                        + "border-color:\\s*var\\(--color-login-action\\);[^}]*"
                        + "border-radius:\\s*var\\(--radius-xl\\);[^}]*"
                        + "min-block-size:\\s*52px;.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-submit:hover\\s*\\{[^}]*"
                        + "background:\\s*var\\(--color-login-action-hover\\);.*"));
        assertTrue(tokens.contains(
                "--color-login-action: var(--palette-brand-hover);"));
        assertTrue(tokens.contains(
                "--color-login-field-border: var(--palette-border);"));
        assertTrue(tokens.contains("--shadow-login-action-hover:"));
        assertTrue(styles.contains(
                ".login-page #loginForm .form-group > input::placeholder"));
        assertHasClasses(firstTag(page, "body"), "ui-system", "login-page");
        assertHasClasses(loginForm(page), "ui-form", "login-form");
        assertTrue(Pattern.compile(
                "<button\\b[^>]*class=\"(?=[^\"]*\\bui-button\\b)"
                        + "(?=[^\"]*\\bbutton--primary\\b)"
                        + "(?=[^\"]*\\bbutton--md\\b)[^\"]*\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(page).find());
    }

    @Test
    void loginPreservesItsPostContractAndExposesStandardCredentialAutocomplete()
            throws Exception {
        String page = read("login.jsp");
        String form = loginForm(page);
        String userId = tagById(page, "userId");
        String password = tagById(page, "password");
        String rememberId = tagById(page, "rememberId");

        assertTrue(form.contains("action=\"login\""));
        assertTrue(form.contains("method=\"post\""));
        assertTrue(form.contains("autocomplete=\"off\""));
        assertFalse(form.contains("autocomplete=\"on\""));
        assertTrue(page.contains("csrf_input.jspf"));
        assertTrue(userId.contains("name=\"userId\""));
        assertTrue(userId.contains("autocomplete=\"username\""));
        assertFalse(userId.contains("autocomplete=\"off\""));
        assertTrue(password.contains("name=\"password\""));
        assertTrue(password.contains("autocomplete=\"current-password\""));
        assertTrue(userId.contains("placeholder=\" \""));
        assertTrue(password.contains("placeholder=\" \""));
        assertTrue(userId.contains(
                "aria-describedby=\"${not empty errorMessage ? 'login-error' : ''}\""));
        assertTrue(password.contains(
                "aria-describedby=\"${not empty errorMessage ? 'login-error' : ''}\""));
        assertTrue(page.contains("class=\"login-field-label\" for=\"userId\""));
        assertTrue(page.contains("class=\"login-field-label\" for=\"password\""));
        assertTrue(page.indexOf(userId)
                < page.indexOf("class=\"login-field-label\" for=\"userId\""));
        assertTrue(page.indexOf(password)
                < page.indexOf("class=\"login-field-label\" for=\"password\""));
        assertTrue(rememberId.contains("type=\"checkbox\""));
        assertFalse(rememberId.contains("name="));
        assertTrue(page.contains("class=\"login-remember\" for=\"rememberId\""));
        assertFalse(page.contains("autocomplete=\"new-password\""));
    }

    @Test
    void loginPeekAndEyeAnimationsAreDecorativeResponsiveAndMotionSafe() throws Exception {
        String page = read("login.jsp");
        String styles = read("resources/css/login_style.css");

        assertTrue(page.contains("class=\"login-peek\" aria-hidden=\"true\""));
        assertTrue(page.contains("class=\"peek-doc\""));
        assertTrue(page.contains("class=\"peek-sheet\""));
        assertTrue(page.contains("class=\"brand-eyes\" aria-hidden=\"true\""));
        assertTrue(page.contains("class=\"brand-eye\""));
        assertTrue(page.contains("class=\"brand-pupil\""));
        assertTrue(styles.contains(".login-page .login-shell:hover .peek-sheet"));
        assertTrue(styles.contains(".login-page .login-shell:focus-within .peek-sheet"));
        assertTrue(styles.contains("@keyframes archive-eye-glance"));
        assertTrue(styles.contains("@keyframes archive-eye-duck"));
        assertTrue(styles.contains(".login-page .login-shell:hover .brand-eye"));
        assertTrue(styles.contains(".login-page .login-shell:focus-within .brand-pupil"));
        assertTrue(styles.contains("@media (hover: hover) and (pointer: fine)"));
        assertTrue(styles.contains("@media (max-width: 640px)"));
        assertTrue(styles.matches(
                "(?s).*@media \\(prefers-reduced-motion: reduce\\)\\s*\\{.*"
                        + "--peek-duration:\\s*0\\.01ms;.*"
                        + "--peek-fade:\\s*0\\.01ms;.*"
                        + "\\.login-page \\.brand-eye,.*"
                        + "\\.login-page \\.brand-pupil\\s*\\{[^}]*"
                        + "animation:\\s*none !important;.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.login-peek\\s*\\{[^}]*"
                        + "pointer-events:\\s*none;.*"));
        assertTrue(styles.matches(
                "(?s).*\\.login-page \\.brand-eyes\\s*\\{[^}]*"
                        + "pointer-events:\\s*none;.*"));
    }

    @Test
    void shortLoginViewportsKeepEveryControlVerticallyReachable() throws Exception {
        String styles = read("resources/css/login_style.css");

        assertTrue(styles.matches(
                "(?s).*body\\.login-page\\s*\\{[^}]*"
                        + "overflow-x:\\s*hidden;[^}]*"
                        + "overflow-y:\\s*auto;[^}]*"
                        + "overscroll-behavior-y:\\s*contain;.*"));
        assertFalse(styles.matches(
                "(?s).*body\\.login-page\\s*\\{[^}]*overflow:\\s*hidden;.*"));
        assertTrue(styles.matches(
                "(?s).*@media \\(max-height:\\s*640px\\)\\s*\\{\\s*"
                        + "body\\.login-page\\s*\\{[^}]*"
                        + "align-items:\\s*flex-start;[^}]*"
                        + "padding-block:\\s*var\\(--space-16\\);.*"));
    }

    @Test
    void rememberedUserIdUsesExpiringLocalStorageWithoutTouchingPasswords()
            throws Exception {
        String script = read("resources/js/pages/login.js");

        assertTrue(script.contains("archive.login.rememberedUserId.v1"));
        assertTrue(script.contains("90 * 24 * 60 * 60 * 1000"));
        assertTrue(script.contains("window.localStorage.getItem(STORAGE_KEY)"));
        assertTrue(script.contains("window.localStorage.setItem(STORAGE_KEY"));
        assertTrue(script.contains("window.localStorage.removeItem(STORAGE_KEY)"));
        assertTrue(script.contains("form.addEventListener('submit'"));
        assertTrue(script.contains("rememberIdInput.addEventListener('change'"));
        assertFalse(script.contains("getElementById('password')"));
        assertFalse(script.contains("name=\"password\""));
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
