package com.company.filter;

import com.company.web.RequestPaths;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public class SecurityHeadersFilter implements Filter {
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com",
            "style-src 'self' https://fonts.googleapis.com https://cdn.jsdelivr.net https://cdnjs.cloudflare.com",
            "img-src 'self' data:",
            "font-src 'self' data: https://fonts.gstatic.com https://fonts.googleapis.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "frame-ancestors 'self'",
            "form-action 'self'");
    @Override
    public void init(FilterConfig filterConfig) throws ServletException { }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse res = response instanceof HttpServletResponse
                ? (HttpServletResponse) response : null;
        HttpServletRequest req = request instanceof HttpServletRequest
                ? (HttpServletRequest) request : null;
        boolean staticResource = false;
        if (res != null) {
            applySecurityHeaders(req, res);
            staticResource = req != null && RequestPaths.isPublicStaticRequest(req);
            if (staticResource) {
                res.setHeader("Cache-Control", "public, max-age=300, must-revalidate");
                res.setDateHeader("Expires", System.currentTimeMillis() + 300_000L);
            } else {
                // HTML and JSON responses may contain session-specific or CSRF data.
                applyNoStore(res);
            }
        }
        ServletResponse downstreamResponse = res == null
                ? response
                : new SecurityResponseWrapper(req, res, staticResource);
        try {
            chain.doFilter(request, downstreamResponse);
        } catch (IOException | ServletException | RuntimeException | Error exception) {
            if (staticResource) {
                applyNoStore(res);
            }
            throw exception;
        }
        if (staticResource && res.getStatus() >= HttpServletResponse.SC_BAD_REQUEST) {
            applyNoStore(res);
        }
    }

    private static void applySecurityHeaders(
            HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        if (request != null && request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
    }

    private static void applyNoStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private static final class SecurityResponseWrapper extends HttpServletResponseWrapper {
        private final HttpServletRequest request;
        private final boolean staticResource;

        private SecurityResponseWrapper(
                HttpServletRequest request, HttpServletResponse response, boolean staticResource) {
            super(response);
            this.request = request;
            this.staticResource = staticResource;
        }

        @Override
        public void reset() {
            super.reset();
            applySecurityHeaders(request, this);
            if (staticResource) {
                setHeader("Cache-Control", "public, max-age=300, must-revalidate");
                setDateHeader("Expires", System.currentTimeMillis() + 300_000L);
            } else {
                applyNoStore(this);
            }
        }

        @Override
        public void setStatus(int status) {
            preventErrorCaching(status);
            super.setStatus(status);
        }

        @Override
        public void sendError(int status) throws IOException {
            preventErrorCaching(status);
            super.sendError(status);
        }

        @Override
        public void sendError(int status, String message) throws IOException {
            preventErrorCaching(status);
            super.sendError(status, message);
        }

        private void preventErrorCaching(int status) {
            if (status >= HttpServletResponse.SC_BAD_REQUEST) {
                applyNoStore(this);
            }
        }
    }

    @Override
    public void destroy() { }
}
