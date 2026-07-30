package com.company.security;

import com.company.web.ApplicationError;
import com.company.web.RequestPaths;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public class CsrfFilter implements Filter {
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private static final int ANONYMOUS_SESSION_TIMEOUT_SECONDS = 600;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String path = RequestPaths.relativePath(request);

        if (SAFE_METHODS.contains(method)) {
            exposeTokenForPageRequest(request, path);
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        String suppliedToken = request.getHeader(CsrfToken.HEADER_NAME);
        if (suppliedToken == null || suppliedToken.isBlank()) {
            suppliedToken = request.getParameter(CsrfToken.PARAMETER_NAME);
        }

        if (!CsrfToken.isValid(session, suppliedToken)) {
            reject(request, response);
            return;
        }

        request.setAttribute(CsrfToken.REQUEST_ATTRIBUTE, CsrfToken.getOrCreate(session));
        chain.doFilter(req, res);
    }

    private static void exposeTokenForPageRequest(HttpServletRequest request, String path) {
        HttpSession session = request.getSession(false);
        if (session == null && RequestPaths.isLoginPath(path)) {
            session = request.getSession(true);
            session.setMaxInactiveInterval(ANONYMOUS_SESSION_TIMEOUT_SECONDS);
        }
        if (session != null) {
            request.setAttribute(CsrfToken.REQUEST_ATTRIBUTE, CsrfToken.getOrCreate(session));
        }
    }

    private static void reject(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ApplicationError.send(
                request,
                response,
                HttpServletResponse.SC_FORBIDDEN,
                "invalid_csrf",
                "Request token is invalid");
    }
}
