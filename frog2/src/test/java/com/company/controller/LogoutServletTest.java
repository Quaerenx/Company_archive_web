package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LogoutServletTest {
    @Test
    void postInvalidatesSessionAndExpiresTheContextSessionCookie()
            throws Exception {
        AtomicBoolean invalidated = new AtomicBoolean();
        AtomicReference<Cookie> expiredCookie = new AtomicReference<>();
        AtomicReference<String> redirect = new AtomicReference<>();
        LogoutServlet servlet = new LogoutServlet();

        servlet.doPost(
                request(invalidated, false),
                response(expiredCookie, redirect));

        assertTrue(invalidated.get());
        Cookie cookie = expiredCookie.get();
        assertNotNull(cookie);
        assertEquals("JSESSIONID", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals("/frog2", cookie.getPath());
        assertEquals(0, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertFalse(cookie.getSecure());
        assertEquals("Strict", cookie.getAttribute("SameSite"));
        assertEquals("/frog2/login", redirect.get());
    }

    @Test
    void httpsLogoutExpiresASecureSessionCookie() throws Exception {
        AtomicReference<Cookie> expiredCookie = new AtomicReference<>();

        new LogoutServlet().doPost(
                request(new AtomicBoolean(), true),
                response(expiredCookie, new AtomicReference<>()));

        assertNotNull(expiredCookie.get());
        assertTrue(expiredCookie.get().getSecure());
    }

    private static HttpServletRequest request(
            AtomicBoolean invalidated, boolean secure) {
        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[] {HttpSession.class},
                (ignored, method, args) -> {
                    if ("invalidate".equals(method.getName())) {
                        invalidated.set(true);
                    }
                    return defaultValue(method.getReturnType());
                });
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "getContextPath" -> "/frog2";
                    case "isSecure" -> secure;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static HttpServletResponse response(
            AtomicReference<Cookie> expiredCookie,
            AtomicReference<String> redirect) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (ignored, method, args) -> switch (method.getName()) {
                    case "addCookie" -> {
                        expiredCookie.set((Cookie) args[0]);
                        yield null;
                    }
                    case "sendRedirect" -> {
                        redirect.set((String) args[0]);
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
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
