package com.company.controller;

import com.company.model.UserDTO;
import com.company.search.GlobalSearchJson;
import com.company.search.GlobalSearchOutcome;
import com.company.search.GlobalSearchService;
import com.company.performance.RequestPerformanceContext;
import com.company.security.SessionPrincipal;
import com.company.util.SearchQueryPolicy;
import com.company.web.ApplicationError;
import com.company.web.JsonResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class GlobalSearchServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private GlobalSearchService searchService;

    @Override
    public void init() throws ServletException {
        try {
            searchService = new GlobalSearchService();
        } catch (IOException | IllegalStateException exception) {
            throw new ServletException(
                    "Global search storage is not configured safely",
                    exception);
        }
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        UserDTO user = SessionPrincipal.expose(request);
        if (user == null) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "authentication_required",
                    "Authentication is required");
            return;
        }

        String query;
        try {
            query = SearchQueryPolicy.normalize(request.getParameter("q"));
            if (query == null) {
                throw new IllegalArgumentException("검색어를 입력해 주세요.");
            }
        } catch (IllegalArgumentException exception) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "invalid_query",
                    exception.getMessage());
            return;
        }

        GlobalSearchOutcome outcome;
        try {
            outcome = searchService.search(query);
        } finally {
            RequestPerformanceContext.markOperation(
                    RequestPerformanceContext.Operation.GLOBAL_SEARCH);
        }
        if (outcome.allSourcesUnavailable()) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "search_unavailable",
                    "검색 서비스를 일시적으로 사용할 수 없습니다.");
            return;
        }

        response.setHeader("Cache-Control", "no-store");
        JsonResponse.write(
                response,
                HttpServletResponse.SC_OK,
                GlobalSearchJson.encode(
                        query, request.getContextPath(), outcome));
    }
}
