package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FaviconServletTest {
    @Test
    void legacyIcoRequestRedirectsToVersionedArchiveSvg() throws Exception {
        Map<String, String> headers = new HashMap<>();
        AtomicInteger status = new AtomicInteger();
        ServletContext servletContext = (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[] {ServletContext.class},
                (ignored, call, args) -> "getInitParameter".equals(call.getName())
                        ? "asset-test-version"
                        : defaultValue(call.getReturnType()));
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, call, args) -> switch (call.getName()) {
                    case "getContextPath" -> "/frog2";
                    case "getServletContext" -> servletContext;
                    default -> defaultValue(call.getReturnType());
                });
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] {HttpServletResponse.class},
                (ignored, call, args) -> {
                    if ("setStatus".equals(call.getName())) {
                        status.set((Integer) args[0]);
                    } else if ("setHeader".equals(call.getName())) {
                        headers.put((String) args[0], (String) args[1]);
                    }
                    return defaultValue(call.getReturnType());
                });

        new FaviconServlet().doGet(request, response);

        assertEquals(HttpServletResponse.SC_FOUND, status.get());
        assertEquals(
                "/frog2/favicon.svg?v=asset-test-version",
                headers.get("Location"));
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
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        return 0;
    }
}
