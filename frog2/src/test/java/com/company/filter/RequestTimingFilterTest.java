package com.company.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.performance.RequestPerformanceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RequestTimingFilterTest {
    @Test
    void emitsOneSanitizedEventWithAggregatedSqlTiming() throws Exception {
        AtomicLong clock = new AtomicLong();
        AtomicReference<RequestTimingFilter.RequestEvent> captured =
                new AtomicReference<>();
        RequestTimingFilter filter = new RequestTimingFilter(
                () -> clock.getAndAdd(25_000_000),
                20,
                captured::set);
        HttpServletRequest request = request("GET", "/frog2/dashboard", "/frog2");
        HttpServletResponse response = response(200);
        FilterChain chain = (req, res) -> {
            RequestPerformanceContext.markOperation(
                    RequestPerformanceContext.Operation.TROUBLESHOOTING_SUMMARY_SEARCH);
            RequestPerformanceContext.recordSql(7_000_000);
            RequestPerformanceContext.recordDbAcquisition(3_000_000);
            RequestPerformanceContext.recordFileSnapshotCacheMiss();
            RequestPerformanceContext.recordFileSnapshotScan(5_000_000);
        };

        filter.doFilter(request, response, chain);

        RequestTimingFilter.RequestEvent event = captured.get();
        assertTrue(event.requestId().matches("[0-9a-f]{32}"));
        assertEquals("GET", event.method());
        assertEquals("/dashboard", event.path());
        assertEquals(200, event.status());
        assertEquals(25_000_000, event.elapsedNanos());
        assertEquals(1, event.sqlCount());
        assertEquals(7_000_000, event.sqlDurationNanos());
        assertEquals("troubleshooting.summarySearch", event.operation());
        assertEquals(1, event.dbAcquisitionCount());
        assertEquals(3_000_000, event.dbAcquisitionDurationNanos());
        assertEquals(0, event.fileSnapshotCacheHits());
        assertEquals(1, event.fileSnapshotCacheMisses());
        assertEquals(1, event.fileSnapshotScanCount());
        assertEquals(5_000_000, event.fileSnapshotScanDurationNanos());
        assertTrue(event.slow());
    }

    @Test
    void doesNotLogPublicStaticResources() throws Exception {
        AtomicInteger events = new AtomicInteger();
        RequestTimingFilter filter = new RequestTimingFilter(
                System::nanoTime,
                20,
                event -> events.incrementAndGet());
        AtomicInteger chainCalls = new AtomicInteger();

        filter.doFilter(
                request("GET", "/frog2/resources/css/ui-system.css", "/frog2"),
                response(200),
                (req, res) -> chainCalls.incrementAndGet());

        assertEquals(1, chainCalls.get());
        assertEquals(0, events.get());
    }

    @Test
    void stripsUrlSessionTokensAndReturnsAnOpaqueRequestId()
            throws Exception {
        AtomicReference<RequestTimingFilter.RequestEvent> captured =
                new AtomicReference<>();
        AtomicReference<String> responseRequestId = new AtomicReference<>();
        RequestTimingFilter filter = new RequestTimingFilter(
                System::nanoTime,
                500,
                captured::set);

        filter.doFilter(
                request(
                        "GET",
                        "/frog2/maintenance;jsessionid=SECRET/history;v=1",
                        "/frog2"),
                response(200, responseRequestId),
                (req, res) -> { });

        assertEquals("/maintenance/history", captured.get().path());
        assertEquals(captured.get().requestId(), responseRequestId.get());
        assertTrue(!captured.get().path().contains("SECRET"));
    }

    private static HttpServletRequest request(
            String method,
            String requestUri,
            String contextPath) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, called, args) -> switch (called.getName()) {
                    case "getMethod" -> method;
                    case "getRequestURI" -> requestUri;
                    case "getContextPath" -> contextPath;
                    default -> defaultValue(called.getReturnType());
                });
    }

    private static HttpServletResponse response(int status) {
        return response(status, null);
    }

    private static HttpServletResponse response(
            int status,
            AtomicReference<String> requestId) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[] { HttpServletResponse.class },
                (proxy, called, args) -> {
                    if ("getStatus".equals(called.getName())) {
                        return status;
                    }
                    if (requestId != null
                            && "setHeader".equals(called.getName())
                            && "X-Request-Id".equals(args[0])) {
                        requestId.set((String) args[1]);
                    }
                    return defaultValue(called.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == long.class) {
            return 0L;
        }
        return 0;
    }
}
