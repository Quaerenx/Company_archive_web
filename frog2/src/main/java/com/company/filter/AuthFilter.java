package com.company.filter;

import com.company.security.SessionPrincipal;
import com.company.model.UserDAO;
import com.company.model.UserDTO;
import com.company.web.ApplicationError;
import com.company.web.JsonResponse;
import com.company.web.RequestPaths;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

public class AuthFilter implements Filter {
    private final Predicate<UserDTO> authenticationCurrent;

    public AuthFilter() {
        this(new UserDAO()::isAuthenticationCurrent);
    }

    public AuthFilter(Predicate<UserDTO> authenticationCurrent) {
        this.authenticationCurrent = Objects.requireNonNull(
                authenticationCurrent, "authenticationCurrent");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = RequestPaths.relativePath(request);
        UserDTO user = SessionPrincipal.expose(request);
        boolean logoutPost = "/logout".equals(path)
                && "POST".equals(request.getMethod());
        boolean open = RequestPaths.isLoginPath(path)
                || RequestPaths.isHealthPath(path)
                || RequestPaths.isPublicStaticRequest(request)
                || RequestPaths.isErrorPage(path)
                || logoutPost;
        boolean verify = !open
                || (RequestPaths.isLoginPath(path)
                    && "GET".equals(request.getMethod()));
        if (user != null && verify
                && !authenticationCurrent.test(user)) {
            SessionPrincipal.invalidate(request);
            user = null;
        }
        boolean authenticated = user != null;
        if (open || authenticated) {
            chain.doFilter(req, res);
            return;
        }

        if (JsonResponse.isExpected(request)) {
            ApplicationError.send(
                    request,
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "authentication_required",
                    "Authentication is required");
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    @Override
    public void destroy() {
        // no-op
    }
}
