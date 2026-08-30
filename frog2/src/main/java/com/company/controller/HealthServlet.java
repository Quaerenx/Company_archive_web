package com.company.controller;

import com.company.health.OperationalReadiness;
import com.company.health.OperationalReadiness.Report;
import com.company.web.JsonResponse;
import com.company.web.RequestPaths;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public final class HealthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final Function<jakarta.servlet.ServletContext, Report> readiness;

    public HealthServlet() {
        this(new OperationalReadiness()::inspect);
    }

    HealthServlet(Function<jakarta.servlet.ServletContext, Report> readiness) {
        this.readiness = Objects.requireNonNull(readiness, "readiness");
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        String path = RequestPaths.relativePath(request);
        if ("/health/live".equals(path)) {
            JsonResponse.write(response, HttpServletResponse.SC_OK,
                    "{\"status\":\"ok\"}");
            return;
        }
        if (!"/health/ready".equals(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Report report = readiness.apply(request.getServletContext());
        int status = report.ready()
                ? HttpServletResponse.SC_OK
                : HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        if (!report.ready()) {
            response.setHeader("Retry-After", "5");
        }
        JsonResponse.write(response, status, json(report));
    }

    private static String json(Report report) {
        return "{\"status\":\""
                + (report.ready() ? "ready" : "not_ready")
                + "\",\"components\":{"
                + component("schema", report.schemaReady()) + ","
                + component("database", report.databaseReady()) + ","
                + component("fileRepository", report.fileRepositoryReady()) + ","
                + component("customerHistory", report.customerHistoryReady())
                + "}}";
    }

    private static String component(String name, boolean ready) {
        return JsonResponse.string(name) + ":"
                + JsonResponse.string(ready ? "up" : "down");
    }
}
