package com.company.filter;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.listener.AppLifecycleListener;
import com.company.listener.AppLifecycleListener.SchemaStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SchemaReadinessFilterTest {
    @Test
    void readyDatabaseRequestContinues() throws Exception {
        RequestFixture request = new RequestFixture(
                "GET", "/frog2/dashboard", SchemaStatus.READY);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger calls = new AtomicInteger();

        new SchemaReadinessFilter().doFilter(
                request.proxy(), response.proxy(), countingChain(calls));

        assertEquals(1, calls.get());
        assertEquals(HttpServletResponse.SC_OK, response.status);
    }

    @Test
    void incompatibleSchemaBlocksDatabaseBackedHtmlRequest() throws Exception {
        RequestFixture request = new RequestFixture(
                "GET", "/frog2/maintenance", SchemaStatus.INCOMPATIBLE);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger calls = new AtomicInteger();

        new SchemaReadinessFilter().doFilter(
                request.proxy(), response.proxy(), countingChain(calls));

        assertEquals(0, calls.get());
        assertEquals(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                response.status);
        assertEquals(1, response.sendErrorCalls);
    }

    @Test
    void directDatabaseBackedViewsCannotBypassTheGate() throws Exception {
        for (String path : new String[] {
                "/frog2/dashboard.jsp",
                "/frog2/customers/customers_list.jsp",
                "/frog2/customer-history/customer_history_list.jsp",
                "/frog2/maintenance/maintenance_history.jsp",
                "/frog2/meeting/meeting_list.jsp",
                "/frog2/troubleshooting/troubleshooting_list.jsp",
                "/frog2/mypage/mypage.jsp",
                "/frog2/vm_hosts/list.jsp"
        }) {
            RequestFixture request = new RequestFixture(
                    "GET", path, SchemaStatus.INCOMPATIBLE);
            ResponseFixture response = new ResponseFixture();
            AtomicInteger calls = new AtomicInteger();

            new SchemaReadinessFilter().doFilter(
                    request.proxy(), response.proxy(), countingChain(calls));

            assertEquals(0, calls.get(), path);
            assertEquals(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    response.status,
                    path);
        }
    }

    @Test
    void unavailableDatabaseUsesJsonErrorContract() throws Exception {
        RequestFixture request = new RequestFixture(
                "GET", "/frog2/customers", SchemaStatus.UNAVAILABLE);
        request.parameters.put("action", "getDetail");
        ResponseFixture response = new ResponseFixture();

        new SchemaReadinessFilter().doFilter(
                request.proxy(), response.proxy(), countingChain(
                        new AtomicInteger()));

        assertEquals(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                response.status);
        assertEquals("application/json", response.contentType);
        assertTrue(response.body.toString().contains(
                "\"code\":\"data_unavailable\""));
    }

    @Test
    void missingListenerStateFailsClosedForDatabaseRequest() throws Exception {
        RequestFixture request = new RequestFixture(
                "GET", "/frog2/dashboard", null);
        ResponseFixture response = new ResponseFixture();

        new SchemaReadinessFilter().doFilter(
                request.proxy(), response.proxy(), countingChain(
                        new AtomicInteger()));

        assertEquals(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                response.status);
    }

    @Test
    void publicAndNonDatabasePathsRemainAvailable() throws Exception {
        for (String[] route : new String[][] {
                {"GET", "/frog2/login"},
                {"HEAD", "/frog2/login.jsp"},
                {"GET", "/frog2/resources/css/base.css"},
                {"GET", "/frog2/error/503.jsp"},
                {"GET", "/frog2/file-repository"},
                {"GET", "/frog2/health/live"},
                {"GET", "/frog2/health/ready"},
                {"POST", "/frog2/logout"},
                {"GET", "/frog2/admin/pool-status"},
                {"GET", "/frog2/admin/performance-metrics"},
                {"GET", "/frog2/not-found"}
        }) {
            RequestFixture request = new RequestFixture(
                    route[0], route[1], SchemaStatus.UNAVAILABLE);
            AtomicInteger calls = new AtomicInteger();

            new SchemaReadinessFilter().doFilter(
                    request.proxy(), new ResponseFixture().proxy(),
                    countingChain(calls));

            assertEquals(1, calls.get(), route[1]);
        }
    }

    @Test
    void loginPostRequiresDatabaseAndFilterIsRegisteredAfterCsrf()
            throws Exception {
        RequestFixture request = new RequestFixture(
                "POST", "/frog2/login", SchemaStatus.UNAVAILABLE);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger calls = new AtomicInteger();

        new SchemaReadinessFilter().doFilter(
                request.proxy(), response.proxy(), countingChain(calls));

        assertEquals(0, calls.get());
        assertEquals(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                response.status);

        String webXml = Files.readString(Path.of(
                "src/main/webapp/WEB-INF/web.xml"));
        int csrf = webXml.lastIndexOf(
                "<filter-name>CsrfFilter</filter-name>");
        int readiness = webXml.indexOf(
                "<filter-name>SchemaReadinessFilter</filter-name>");
        assertTrue(csrf >= 0 && readiness > csrf);
    }

    private static FilterChain countingChain(AtomicInteger calls) {
        return (request, response) -> calls.incrementAndGet();
    }

    private static final class RequestFixture {
        private final String method;
        private final String uri;
        private final ServletContext context;
        private final Map<String, String> parameters = new HashMap<>();

        private RequestFixture(
                String method, String uri, SchemaStatus status) {
            this.method = method;
            this.uri = uri;
            Map<String, Object> attributes = new HashMap<>();
            if (status != null) {
                attributes.put(
                        AppLifecycleListener.SCHEMA_STATUS_ATTRIBUTE, status);
            }
            this.context = (ServletContext) Proxy.newProxyInstance(
                    ServletContext.class.getClassLoader(),
                    new Class<?>[] {ServletContext.class},
                    (ignored, call, arguments) -> switch (call.getName()) {
                        case "getAttribute" ->
                                attributes.get((String) arguments[0]);
                        default -> defaultValue(call.getReturnType());
                    });
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, arguments) -> switch (call.getName()) {
                        case "getMethod" -> method;
                        case "getRequestURI" -> uri;
                        case "getContextPath" -> "/frog2";
                        case "getServletContext" -> context;
                        case "getParameter" ->
                                parameters.get((String) arguments[0]);
                        case "getHeader" -> null;
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class ResponseFixture {
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private int status = HttpServletResponse.SC_OK;
        private int sendErrorCalls;
        private String contentType;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, arguments) -> switch (call.getName()) {
                        case "isCommitted" -> false;
                        case "resetBuffer" -> {
                            body.getBuffer().setLength(0);
                            yield null;
                        }
                        case "setStatus" -> {
                            status = (Integer) arguments[0];
                            yield null;
                        }
                        case "sendError" -> {
                            status = (Integer) arguments[0];
                            sendErrorCalls++;
                            yield null;
                        }
                        case "setContentType" -> {
                            contentType = (String) arguments[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        case "setHeader", "setCharacterEncoding" -> null;
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }
}
