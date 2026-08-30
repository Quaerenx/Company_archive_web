package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.health.OperationalReadiness.Report;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class HealthServletTest {
    @Test
    void livenessDoesNotInvokeDependencyChecks() throws Exception {
        AtomicInteger checks = new AtomicInteger();
        HealthServlet servlet = new HealthServlet(context -> {
            checks.incrementAndGet();
            return new Report(false, false, false, false);
        });
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request("/frog2/health/live"), response.proxy());

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertEquals(0, checks.get());
        assertTrue(response.body.toString().contains("\"status\":\"ok\""));
    }

    @Test
    void readyReportReturnsOnlyComponentStates() throws Exception {
        HealthServlet servlet = new HealthServlet(context ->
                new Report(true, true, true, true));
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request("/frog2/health/ready"), response.proxy());

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertTrue(response.body.toString().contains("\"status\":\"ready\""));
        assertTrue(response.body.toString().contains("\"database\":\"up\""));
        assertFalse(response.body.toString().contains("/opt/"));
    }

    @Test
    void unavailableComponentReturns503AndRetryHeader() throws Exception {
        HealthServlet servlet = new HealthServlet(context ->
                new Report(true, false, true, true));
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request("/frog2/health/ready"), response.proxy());

        assertEquals(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                response.status);
        assertEquals("5", response.headers.get("Retry-After"));
        assertTrue(response.body.toString().contains("not_ready"));
        assertTrue(response.body.toString().contains("\"database\":\"down\""));
    }

    private static HttpServletRequest request(String uri) {
        ServletContext context = (ServletContext) Proxy.newProxyInstance(
                ServletContext.class.getClassLoader(),
                new Class<?>[] {ServletContext.class},
                (ignored, method, arguments) ->
                        defaultValue(method.getReturnType()));
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, method, arguments) -> switch (method.getName()) {
                    case "getRequestURI" -> uri;
                    case "getContextPath" -> "/frog2";
                    case "getServletContext" -> context;
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static final class ResponseFixture {
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private final Map<String, String> headers = new HashMap<>();
        private int status = HttpServletResponse.SC_OK;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, method, arguments) -> switch (method.getName()) {
                        case "setStatus" -> {
                            status = (Integer) arguments[0];
                            yield null;
                        }
                        case "setHeader" -> {
                            headers.put(
                                    (String) arguments[0],
                                    (String) arguments[1]);
                            yield null;
                        }
                        case "getWriter" -> writer;
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }
}
