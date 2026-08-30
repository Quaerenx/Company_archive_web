package com.company.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Set;

public final class RequestPaths {
    private static final Set<String> LOGIN_PATHS = Set.of("/login", "/login.jsp");
    private static final Set<String> HEALTH_PATHS =
            Set.of("/health/live", "/health/ready");
    private static final Set<String> FAVICON_PATHS =
            Set.of("/favicon.ico", "/favicon.png", "/favicon.svg");
    private static final Set<String> BRAND_ASSET_PATHS =
            Set.of("/resources/images/archive-logo.svg");
    private static final Set<String> ERROR_PATHS = Set.of(
            "/error/400.jsp",
            "/error/403.jsp",
            "/error/404.jsp",
            "/error/405.jsp",
            "/error/409.jsp",
            "/error/500.jsp",
            "/error/503.jsp");
    private static final Set<String> PUBLIC_STATIC_EXTENSIONS =
            Set.of(".css", ".js", ".png", ".woff2");
    private static final String[] STATIC_PREFIXES = {
        "/resources/", "/images/", "/css/", "/js/", "/webjars/"
    };

    private RequestPaths() {
    }

    public static String relativePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (requestUri == null || requestUri.isEmpty()) {
            return "/";
        }
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            String relative = requestUri.substring(contextPath.length());
            return relative.isEmpty() ? "/" : relative;
        }
        return requestUri;
    }

    public static boolean isStaticResource(String path) {
        if (path == null) {
            return false;
        }
        if (FAVICON_PATHS.contains(path) || BRAND_ASSET_PATHS.contains(path)) {
            return true;
        }
        if (path.indexOf(';') >= 0 || path.indexOf('%') >= 0
                || path.indexOf('\\') >= 0 || path.contains("..")) {
            return false;
        }
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        boolean allowedExtension = PUBLIC_STATIC_EXTENSIONS.stream()
                .anyMatch(normalizedPath::endsWith);
        if (!allowedExtension) {
            return false;
        }
        for (String prefix : STATIC_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPublicStaticRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return false;
        }

        return isStaticResource(relativePath(request));
    }

    public static boolean isErrorPage(String path) {
        return ERROR_PATHS.contains(path);
    }

    public static boolean isLoginPath(String path) {
        return LOGIN_PATHS.contains(path);
    }

    public static boolean isHealthPath(String path) {
        return HEALTH_PATHS.contains(path);
    }
}
