package com.company.filter;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.DataAccessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ApplicationExceptionFilterTest {
    @Test
    void readOnlyFailureReturnsExplicitJsonConflict() throws Exception {
        RequestFixture request = new RequestFixture("POST", "/frog2/comment");
        ResponseFixture response = new ResponseFixture();
        FilterChain chain = (req, res) -> {
            throw DataAccessException.from(
                    "add comment", new SQLNonTransientException("blocked", "25006"));
        };

        new ApplicationExceptionFilter().doFilter(request.proxy(), response.proxy(), chain);

        assertEquals(HttpServletResponse.SC_CONFLICT, response.status.get());
        assertEquals("application/json", response.contentType);
        assertTrue(response.body.toString().contains("\"code\":\"read_only\""));
        assertTrue(response.body.toString().contains("\"success\":false"));
    }

    @Test
    void unexpectedJsonFailureReturnsGenericInternalError() throws Exception {
        RequestFixture request = new RequestFixture("POST", "/frog2/comment");
        ResponseFixture response = new ResponseFixture();
        FilterChain chain = (req, res) -> {
            throw new IllegalStateException("sensitive implementation detail");
        };

        new ApplicationExceptionFilter().doFilter(request.proxy(), response.proxy(), chain);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.status.get());
        assertEquals("application/json", response.contentType);
        assertTrue(response.body.toString().contains("\"code\":\"internal_error\""));
        assertTrue(!response.body.toString().contains("sensitive implementation detail"));
    }

    @Test
    void servletFailureReturnsGenericJsonInternalError() throws Exception {
        RequestFixture request = new RequestFixture("POST", "/frog2/comment");
        ResponseFixture response = new ResponseFixture();
        FilterChain chain = (req, res) -> { throw new ServletException("sensitive detail"); };

        new ApplicationExceptionFilter().doFilter(request.proxy(), response.proxy(), chain);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.status.get());
        assertEquals("application/json", response.contentType);
        assertTrue(response.body.toString().contains("\"code\":\"internal_error\""));
        assertTrue(!response.body.toString().contains("sensitive detail"));
    }

    @Test
    void wrappedDatabaseFailuresKeepTheirDatabaseErrorContract() throws Exception {
        RequestFixture readOnlyRequest = new RequestFixture("POST", "/frog2/comment");
        ResponseFixture readOnlyResponse = new ResponseFixture();
        FilterChain readOnlyChain = (req, res) -> {
            DataAccessException failure = DataAccessException.from(
                    "wrapped write", new SQLNonTransientException("blocked", "25006"));
            throw new ServletException("dispatcher failed", failure);
        };

        new ApplicationExceptionFilter().doFilter(
                readOnlyRequest.proxy(), readOnlyResponse.proxy(), readOnlyChain);

        assertEquals(HttpServletResponse.SC_CONFLICT, readOnlyResponse.status.get());
        assertTrue(readOnlyResponse.body.toString().contains("\"code\":\"read_only\""));

        RequestFixture unavailableRequest = new RequestFixture("POST", "/frog2/comment");
        ResponseFixture unavailableResponse = new ResponseFixture();
        FilterChain unavailableChain = (req, res) -> {
            DataAccessException failure = DataAccessException.from(
                    "wrapped read", new SQLException("down", "08001"));
            throw new ServletException("dispatcher failed", failure);
        };

        new ApplicationExceptionFilter().doFilter(
                unavailableRequest.proxy(), unavailableResponse.proxy(), unavailableChain);

        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, unavailableResponse.status.get());
        assertTrue(unavailableResponse.body.toString().contains(
                "\"code\":\"data_unavailable\""));
    }

    @Test
    void partialJsonBodyIsClearedBeforeWritingTheCommonError() throws Exception {
        RequestFixture request = new RequestFixture("POST", "/frog2/comment");
        ResponseFixture response = new ResponseFixture();
        FilterChain chain = (req, res) -> {
            ((HttpServletResponse) res).getWriter().write("partial");
            throw new IllegalStateException("failed after write");
        };

        new ApplicationExceptionFilter().doFilter(request.proxy(), response.proxy(), chain);

        String body = response.body.toString();
        assertFalse(body.contains("partial"));
        assertTrue(body.startsWith("{"));
        assertTrue(body.contains("\"code\":\"internal_error\""));
    }

    @Test
    void databaseFailureReturnsHtmlServiceUnavailable() throws Exception {
        RequestFixture request = new RequestFixture("GET", "/frog2/dashboard");
        ResponseFixture response = new ResponseFixture();
        FilterChain chain = (req, res) -> {
            throw DataAccessException.from("load dashboard", new SQLException("down", "08001"));
        };

        new ApplicationExceptionFilter().doFilter(request.proxy(), response.proxy(), chain);

        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.status.get());
        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.sentError.get());
    }

    private static final class RequestFixture {
        private final String method;
        private final String uri;
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, String> parameters = new HashMap<>();

        private RequestFixture(String method, String uri) {
            this.method = method;
            this.uri = uri;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] { HttpServletRequest.class },
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getMethod" -> method;
                        case "getRequestURI" -> uri;
                        case "getContextPath" -> "/frog2";
                        case "getHeader" -> headers.get((String) args[0]);
                        case "getParameter" -> parameters.get((String) args[0]);
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class ResponseFixture {
        private final AtomicInteger status = new AtomicInteger(HttpServletResponse.SC_OK);
        private final AtomicInteger sentError = new AtomicInteger();
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private String contentType;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class },
                    (ignored, call, args) -> switch (call.getName()) {
                        case "isCommitted" -> false;
                        case "resetBuffer" -> {
                            body.getBuffer().setLength(0);
                            yield null;
                        }
                        case "setStatus" -> {
                            status.set((Integer) args[0]);
                            yield null;
                        }
                        case "sendError" -> {
                            int errorStatus = (Integer) args[0];
                            status.set(errorStatus);
                            sentError.set(errorStatus);
                            yield null;
                        }
                        case "setContentType" -> {
                            contentType = (String) args[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }
}
