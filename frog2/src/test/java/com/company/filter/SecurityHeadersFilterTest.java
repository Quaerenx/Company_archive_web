package com.company.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SecurityHeadersFilterTest {
    private static final Pattern MAX_AGE = Pattern.compile("(?:^|,\\s*)max-age=(\\d+)(?:,|$)");

    @Test
    void cspExcludesUnusedJqueryOriginAndKeepsRequiredOrigins() throws Exception {
        Map<String, String> headers = new HashMap<>();
        AtomicBoolean chained = new AtomicBoolean();
        HttpServletRequest request = request("/frog2/login", null);
        HttpServletResponse response = response(headers);
        FilterChain chain = (req, res) -> chained.set(true);

        new SecurityHeadersFilter().doFilter(request, response, chain);

        String csp = headers.get("Content-Security-Policy");
        assertFalse(csp.contains("code.jquery.com"));
        assertFalse(csp.contains("'unsafe-inline'"));
        assertTrue(csp.contains("https://cdn.jsdelivr.net"));
        assertTrue(csp.contains("https://cdnjs.cloudflare.com"));
        assertTrue(chained.get());
    }

    @Test
    void dynamicResponsesAreAlwaysNoStore() throws Exception {
        for (String path : new String[] {"/frog2/login", "/frog2/dashboard"}) {
            for (HttpSession session : new HttpSession[] {null, authenticatedSession()}) {
                Map<String, String> headers = apply(path, session);

                assertSecurityHeaders(headers);
                assertTrue(headers.getOrDefault("Cache-Control", "").contains("no-store"),
                        () -> "dynamic response must not be cached: " + path);
            }
        }
    }

    @Test
    void staticResourcesUseShortLivedCacheWithoutLosingSecurityHeaders() throws Exception {
        for (String path : new String[] {
                "/frog2/resources/css/base.css",
                "/frog2/resources/js/header_nav.js",
                "/frog2/resources/fonts/ibm-plex-sans-kr/1.1.0/font.woff2",
                "/frog2/resources/images/archive-primary-logo.svg",
                "/frog2/resources/images/archive-compact-horizontal.svg",
                "/frog2/favicon.png",
                "/frog2/favicon.svg",
                "/frog2/favicon.ico"
        }) {
            Map<String, String> headers = apply(path, authenticatedSession());

            assertSecurityHeaders(headers);
            String cacheControl = headers.get("Cache-Control");
            assertNotNull(cacheControl, () -> "cache policy is missing: " + path);
            assertFalse(cacheControl.contains("no-store"), path);
            assertFalse(cacheControl.contains("no-cache"), path);

            Matcher maxAge = MAX_AGE.matcher(cacheControl);
            assertTrue(maxAge.find(), () -> "positive max-age is missing: " + path);
            int seconds = Integer.parseInt(maxAge.group(1));
            assertTrue(seconds > 0 && seconds <= 300,
                    () -> "static cache TTL must be short (1..300 seconds): " + path);
        }
    }

    @Test
    void staticSetStatus404SwitchesToNoStore() throws Exception {
        Map<String, String> headers = apply(
                "/frog2/resources/css/missing.css",
                authenticatedSession(),
                response -> response.setStatus(HttpServletResponse.SC_NOT_FOUND));

        assertSecurityHeaders(headers);
        assertNoStore(headers);
    }

    @Test
    void staticSendError404SwitchesToNoStore() throws Exception {
        Map<String, String> headers = apply(
                "/frog2/resources/js/missing.js",
                authenticatedSession(),
                response -> response.sendError(HttpServletResponse.SC_NOT_FOUND));

        assertSecurityHeaders(headers);
        assertNoStore(headers);
    }

    @Test
    void faviconRedirectKeepsShortLivedPublicCache() throws Exception {
        Map<String, String> headers = apply(
                "/frog2/favicon.png",
                authenticatedSession(),
                response -> response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY));

        assertSecurityHeaders(headers);
        String cacheControl = headers.get("Cache-Control");
        assertNotNull(cacheControl);
        assertTrue(cacheControl.contains("public"));
        assertFalse(cacheControl.contains("no-store"));
        Matcher maxAge = MAX_AGE.matcher(cacheControl);
        assertTrue(maxAge.find());
        int seconds = Integer.parseInt(maxAge.group(1));
        assertTrue(seconds > 0 && seconds <= 300);
    }

    @Test
    void staticRuntimeExceptionSwitchesToNoStoreAndIsRethrown() {
        Map<String, String> headers = new HashMap<>();
        RuntimeException downstream =
                new RuntimeException("downstream static failure");

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> apply(
                        "/frog2/resources/css/failing.css",
                        authenticatedSession(),
                        headers,
                        response -> {
                            throw downstream;
                        }));

        assertTrue(thrown == downstream);
        assertNoStore(headers);
    }


    @Test
    void unsafeStaticLookingRequestsAreNeverPubliclyCached() throws Exception {
        Object[][] requests = {
            {"POST", "/frog2/resources/js/app.js"},
            {"GET", "/frog2/resources/hidden.jsp"},
            {"GET", "/frog2/resources/hidden.jsp;asset=.js"},
            {"GET", "/frog2/resources/hidden.jsp%3Basset=.js"}
        };
        for (Object[] candidate : requests) {
            Map<String, String> headers = new HashMap<>();
            new SecurityHeadersFilter().doFilter(
                    request((String) candidate[0], (String) candidate[1], authenticatedSession()),
                    response(headers),
                    (req, res) -> { });

            assertSecurityHeaders(headers);
            assertNoStore(headers);
        }
    }

    @Test
    void resetReappliesSecurityAndNoStoreHeaders() throws Exception {
        Map<String, String> headers = apply(
                "/frog2/file-repository/download",
                authenticatedSession(),
                HttpServletResponse::reset);

        assertSecurityHeaders(headers);
        assertNoStore(headers);
    }

    private static Map<String, String> apply(String path, HttpSession session) throws Exception {
        return apply(path, session, response -> { });
    }

    private static Map<String, String> apply(
            String path,
            HttpSession session,
            ResponseAction responseAction) throws Exception {
        Map<String, String> headers = new HashMap<>();
        apply(path, session, headers, responseAction);
        return headers;
    }

    private static void apply(
            String path,
            HttpSession session,
            Map<String, String> headers,
            ResponseAction responseAction) throws Exception {
        AtomicBoolean chained = new AtomicBoolean();
        new SecurityHeadersFilter().doFilter(
                request(path, session),
                response(headers),
                (request, response) -> {
                    chained.set(true);
                    responseAction.apply((HttpServletResponse) response);
                });
        assertTrue(chained.get());
    }

    @FunctionalInterface
    private interface ResponseAction {
        void apply(HttpServletResponse response) throws IOException;
    }

    private static void assertNoStore(Map<String, String> headers) {
        String cacheControl = headers.getOrDefault("Cache-Control", "");
        assertTrue(cacheControl.contains("no-store"));
        assertTrue(cacheControl.contains("no-cache"));
    }

    private static void assertSecurityHeaders(Map<String, String> headers) {
        assertTrue("nosniff".equals(headers.get("X-Content-Type-Options")));
        assertTrue("SAMEORIGIN".equals(headers.get("X-Frame-Options")));
        assertTrue("strict-origin-when-cross-origin".equals(headers.get("Referrer-Policy")));
        assertTrue("same-origin".equals(headers.get("Cross-Origin-Opener-Policy")));
        assertTrue("same-origin".equals(headers.get("Cross-Origin-Resource-Policy")));
        assertNotNull(headers.get("Permissions-Policy"));
        assertNotNull(headers.get("Content-Security-Policy"));
    }

    private static HttpServletRequest request(String uri, HttpSession session) {
        return request("GET", uri, session);
    }

    private static HttpServletRequest request(
            String method, String uri, HttpSession session) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "isSecure" -> false;
                    case "getMethod" -> method;
                    case "getRequestURI" -> uri;
                    case "getContextPath" -> "/frog2";
                    case "getSession" -> session;
                    default -> defaultValue(call.getReturnType());
                });
    }

    private static HttpSession authenticatedSession() {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[] {HttpSession.class},
                (ignored, call, args) -> "getAttribute".equals(call.getName())
                        && "user".equals(args[0])
                        ? new Object()
                        : defaultValue(call.getReturnType()));
    }

    private static HttpServletResponse response(Map<String, String> headers) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (ignored, call, args) -> {
                    if ("setHeader".equals(call.getName())) {
                        headers.put((String) args[0], (String) args[1]);
                    } else if ("reset".equals(call.getName())) {
                        headers.clear();
                    }
                    return defaultValue(call.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
