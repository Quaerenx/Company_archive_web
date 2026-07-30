package com.company.filter;

import com.company.model.DataAccessException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationExceptionFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationExceptionFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(req, res);
        } catch (DataAccessException exception) {
            handleDataAccess(req, res, exception);
        } catch (ServletException exception) {
            DataAccessException dataFailure = findCause(exception, DataAccessException.class);
            if (dataFailure != null) {
                handleDataAccess(req, res, dataFailure);
                return;
            }
            handleUnhandled(req, res, "servlet", exception);
        } catch (RuntimeException exception) {
            handleUnhandled(req, res, "application", exception);
        }
    }

    private static void handleDataAccess(
            ServletRequest req, ServletResponse res, DataAccessException exception)
            throws IOException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (response.isCommitted()) {
            throw exception;
        }

        String path = RequestPaths.relativePath(request);
        if (exception.isReadOnlyViolation()) {
            logger.warn("Read-only database operation blocked: method={}, path={}",
                    request.getMethod(), path);
            sendError(request, response, HttpServletResponse.SC_CONFLICT,
                    "read_only", "This environment is read-only");
        } else {
            logger.error("Database operation failed: method={}, path={}",
                    request.getMethod(), path, exception);
            sendError(request, response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "data_unavailable", "The data service is temporarily unavailable");
        }
    }

    private static void handleUnhandled(
            ServletRequest req, ServletResponse res, String failureType, Throwable exception)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        if (response.isCommitted()) {
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (exception instanceof ServletException servletException) {
                throw servletException;
            }
            throw new ServletException(exception);
        }

        String path = RequestPaths.relativePath(request);
        logger.error("Unhandled {} failure: method={}, path={}",
                failureType, request.getMethod(), path, exception);
        sendError(
                request,
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "internal_error",
                "The request could not be completed");
    }

    private static <T extends Throwable> T findCause(Throwable failure, Class<T> type) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    private static void sendError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message) throws IOException {
        ApplicationError.send(request, response, status, code, message);
    }
}
