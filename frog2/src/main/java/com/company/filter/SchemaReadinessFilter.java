package com.company.filter;

import com.company.listener.AppLifecycleListener;
import com.company.listener.AppLifecycleListener.SchemaStatus;
import com.company.web.ApplicationError;
import com.company.web.RequestPaths;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public final class SchemaReadinessFilter implements Filter {
    private static final Set<String> DATABASE_PATHS = Set.of(
            "/",
            "/dashboard",
            "/dashboard.jsp",
            "/customers",
            "/customer-history",
            "/maintenance",
            "/meeting",
            "/troubleshooting",
            "/comment",
            "/mypage",
            "/vm-hosts");
    private static final String[] DATABASE_VIEW_PREFIXES = {
        "/customers/",
        "/customer-history/",
        "/maintenance/",
        "/meeting/",
        "/troubleshooting/",
        "/mypage/",
        "/vm_hosts/",
        "/vm-hosts/"
    };

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = RequestPaths.relativePath(httpRequest);
        if (!requiresDatabase(httpRequest.getMethod(), path)) {
            chain.doFilter(request, response);
            return;
        }

        Object value = httpRequest.getServletContext().getAttribute(
                AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE);
        if (value == SchemaStatus.READY) {
            chain.doFilter(request, response);
            return;
        }

        SchemaStatus status = value instanceof SchemaStatus schemaStatus
                ? schemaStatus
                : SchemaStatus.UNAVAILABLE;
        if (status == SchemaStatus.INCOMPATIBLE) {
            ApplicationError.send(
                    httpRequest,
                    httpResponse,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "schema_incompatible",
                    "The application database schema is not compatible");
        } else {
            ApplicationError.send(
                    httpRequest,
                    httpResponse,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "data_unavailable",
                    "The data service is temporarily unavailable");
        }
    }

    static boolean requiresDatabase(String method, String path) {
        if (RequestPaths.isLoginPath(path)) {
            return !"GET".equalsIgnoreCase(method)
                    && !"HEAD".equalsIgnoreCase(method);
        }
        if (DATABASE_PATHS.contains(path)) {
            return true;
        }
        if (path != null) {
            for (String prefix : DATABASE_VIEW_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }
}
