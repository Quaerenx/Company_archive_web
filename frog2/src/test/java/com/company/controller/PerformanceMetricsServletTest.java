package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.UserDTO;
import com.company.performance.PerformanceMetricsRegistry.PageSnapshot;
import com.company.performance.PerformanceMetricsRegistry.SearchSnapshot;
import com.company.performance.PerformanceMetricsRegistry.Snapshot;
import com.company.security.AdminAccessPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PerformanceMetricsServletTest {
    @AfterEach
    void clearAdminConfiguration() {
        System.clearProperty(AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY);
    }

    @Test
    void administratorCanReadAggregateMetricsWithoutSearchData()
            throws Exception {
        System.setProperty(
                AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY, "admin-user");
        SearchSnapshot summary = new SearchSnapshot(
                2, 1, 300_000_000, 100_000_000,
                200_000_000, 70_000_000);
        PerformanceMetricsServlet servlet = new PerformanceMetricsServlet(
                () -> new Snapshot(
                        summary,
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        new PageSnapshot(
                                2, 1, 1_000_000_000, 100_000_000,
                                400_000_000, 300_000_000, 700_000_000),
                        emptyPage(),
                        emptyPage()));
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(
                request(new UserDTO(
                        "admin-user", "", "Admin", "Operations")),
                response.proxy());

        assertEquals(HttpServletResponse.SC_OK, response.status);
        assertTrue(response.body.toString().contains("\"count\":2"));
        assertTrue(response.body.toString().contains(
                "\"averageSqlMs\":50.000"));
        assertTrue(response.body.toString().contains("\"globalSearch\""));
        assertTrue(response.body.toString().contains("\"pages\""));
        assertTrue(response.body.toString().contains(
                "\"averageDataLoadMs\":200.000"));
        assertTrue(!response.body.toString().contains("query"));
    }

    @Test
    void ordinaryUserCannotReadMetrics() throws Exception {
        System.setProperty(
                AdminAccessPolicy.ADMIN_USER_IDS_PROPERTY, "admin-user");
        PerformanceMetricsServlet servlet = new PerformanceMetricsServlet(
                () -> new Snapshot(
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        emptyPage(),
                        emptyPage(),
                        emptyPage()));
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(
                request(new UserDTO(
                        "ordinary-user", "", "User", "Operations")),
                response.proxy());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status);
    }

    @Test
    void anonymousRequestCannotReadMetrics() throws Exception {
        PerformanceMetricsServlet servlet = new PerformanceMetricsServlet(
                () -> new Snapshot(
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        new SearchSnapshot(0, 0, 0, 0, 0, 0),
                        emptyPage(),
                        emptyPage(),
                        emptyPage()));
        ResponseFixture response = new ResponseFixture();

        servlet.doGet(request(null), response.proxy());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status);
    }

    private static HttpServletRequest request(UserDTO user) {
        HttpSession session = (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[] {HttpSession.class},
                (ignored, method, arguments) ->
                        "getAttribute".equals(method.getName())
                                ? user
                                : defaultValue(method.getReturnType()));
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] {HttpServletRequest.class},
                (ignored, method, arguments) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "getAttribute" -> null;
                    case "getHeader" -> "application/json";
                    default -> defaultValue(method.getReturnType());
                });
    }

    private static PageSnapshot emptyPage() {
        return new PageSnapshot(0, 0, 0, 0, 0, 0, 0);
    }

    private static final class ResponseFixture {
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
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
                        case "getWriter" -> writer;
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }
}
