package com.company.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;

class RequestPathsTest {
    @Test
    void relativePathRemovesOnlyTheCurrentContextPath() {
        assertEquals(
                "/customers",
                RequestPaths.relativePath(request("/frog2/customers", "/frog2")));
        assertEquals(
                "/login",
                RequestPaths.relativePath(request("/login", "")));
        assertEquals(
                "/",
                RequestPaths.relativePath(request("/frog2", "/frog2")));
    }

    @Test
    void classifiesStaticResourcesWithoutMatchingLookalikePaths() {
        for (String path : new String[] {
                "/resources/css/main_style.css",
                "/images/logo.png",
                "/css/legacy.css",
                "/js/legacy.js",
                "/webjars/library/file.js",
                "/resources/fonts/ibm-plex-sans-kr/1.1.0/font.woff2",
                "/resources/images/archive-primary-logo.svg",
                "/resources/images/archive-compact-horizontal.svg",
                "/favicon.ico",
                "/favicon.svg",
                "/favicon.png"}) {
            assertTrue(RequestPaths.isStaticResource(path), () -> "Expected static path: " + path);
        }

        for (String path : new String[] {
                "/resources",
                "/resources-malicious/file.js",
                "/resources/hidden.jsp",
                "/resources/hidden.class",
                "/resources/active.html",
                "/resources/vector.svg",
                "/resources/images/archive-primary-logo.svg/extra",
                "/resources/images/other.svg",
                "/favicon.svg/extra",
                "/resources/hidden.jsp;asset=.js",
                "/resources/hidden.jsp%3Basset=.js",
                "/resources/../WEB-INF/hidden.css",
                "/favicon.png/extra",
                "/customers"}) {
            assertFalse(RequestPaths.isStaticResource(path), () -> "Unexpected static path: " + path);
        }
    }

    @Test
    void classifiesApplicationErrorPagesOnly() {
        assertTrue(RequestPaths.isErrorPage("/error/403.jsp"));
        assertTrue(RequestPaths.isErrorPage("/error/500.jsp"));
        assertFalse(RequestPaths.isErrorPage("/error"));
        assertFalse(RequestPaths.isErrorPage("/errors/500.jsp"));
        assertFalse(RequestPaths.isErrorPage("/error/custom.jsp"));
    }

    @Test
    void exposesOnlyGetAndHeadStaticRequests() {
        assertTrue(RequestPaths.isPublicStaticRequest(
                request("GET", "/frog2/resources/css/base.css", "/frog2")));
        assertTrue(RequestPaths.isPublicStaticRequest(
                request("GET", "/frog2/resources/fonts/font.woff2", "/frog2")));
        assertTrue(RequestPaths.isPublicStaticRequest(
                request("GET", "/frog2/resources/images/archive-primary-logo.svg", "/frog2")));
        assertTrue(RequestPaths.isPublicStaticRequest(
                request("HEAD", "/frog2/favicon.ico", "/frog2")));
        assertTrue(RequestPaths.isPublicStaticRequest(
                request("GET", "/frog2/favicon.svg", "/frog2")));
        assertFalse(RequestPaths.isPublicStaticRequest(
                request("POST", "/frog2/resources/js/app.js", "/frog2")));
        assertFalse(RequestPaths.isPublicStaticRequest(
                request("GET", "/frog2/resources/hidden.jsp", "/frog2")));
    }

    @Test
    void classifiesCanonicalAndLegacyLoginPaths() {
        assertTrue(RequestPaths.isLoginPath("/login"));
        assertTrue(RequestPaths.isLoginPath("/login.jsp"));
        assertFalse(RequestPaths.isLoginPath("/login/"));
        assertFalse(RequestPaths.isLoginPath("/login.jsp/extra"));
    }

    private static HttpServletRequest request(String requestUri, String contextPath) {
        return request("GET", requestUri, contextPath);
    }

    private static HttpServletRequest request(
            String method, String requestUri, String contextPath) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "getMethod" -> method;
                    case "getRequestURI" -> requestUri;
                    case "getContextPath" -> contextPath;
                    default -> defaultValue(call.getReturnType());
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
