package com.company.controller;

import com.company.model.UserDTO;
import com.company.performance.PerformanceMetricsRegistry;
import com.company.performance.PerformanceMetricsRegistry.SearchSnapshot;
import com.company.security.AdminAccessPolicy;
import com.company.security.SessionPrincipal;
import com.company.web.ApplicationError;
import com.company.web.JsonResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

public final class PerformanceMetricsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Supplier<PerformanceMetricsRegistry.Snapshot> metrics;

    public PerformanceMetricsServlet() {
        this(PerformanceMetricsRegistry::snapshot);
    }

    PerformanceMetricsServlet(
            Supplier<PerformanceMetricsRegistry.Snapshot> metrics) {
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        UserDTO user = SessionPrincipal.from(request);
        if (user == null) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "authentication_required",
                    "Authentication is required");
            return;
        }
        if (!AdminAccessPolicy.isAdmin(user)) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    "admin_access_required",
                    "Administrator access is required");
            return;
        }

        PerformanceMetricsRegistry.Snapshot snapshot = metrics.get();
        JsonResponse.write(
                response,
                HttpServletResponse.SC_OK,
                "{\"troubleshooting\":{\"summarySearch\":"
                        + json(snapshot.summarySearch())
                        + ",\"contentSearch\":"
                        + json(snapshot.contentSearch()) + "}}");
    }

    private static String json(SearchSnapshot metrics) {
        return String.format(
                Locale.ROOT,
                "{\"count\":%d,\"slowCount\":%d,"
                        + "\"averageRequestMs\":%.3f,"
                        + "\"averageSqlMs\":%.3f,"
                        + "\"maxRequestMs\":%.3f,"
                        + "\"maxSqlMs\":%.3f}",
                metrics.count(),
                metrics.slowCount(),
                metrics.averageRequestMillis(),
                metrics.averageSqlMillis(),
                metrics.maxRequestMillis(),
                metrics.maxSqlMillis());
    }
}
