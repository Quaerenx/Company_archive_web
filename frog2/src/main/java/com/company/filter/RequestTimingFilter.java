package com.company.filter;

import com.company.performance.RequestPerformanceContext;
import com.company.performance.RequestPerformanceContext.Snapshot;
import com.company.web.RequestPaths;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RequestTimingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final int MAX_PATH_LENGTH = 256;

    private final LongSupplier nanoTime;
    private final long slowRequestNanos;
    private final Consumer<RequestEvent> eventSink;

    public RequestTimingFilter() {
        this(
                System::nanoTime,
                positiveLongProperty("frog2.performance.slowRequestMs", 500),
                RequestTimingFilter::writeLog);
    }

    RequestTimingFilter(
            LongSupplier nanoTime,
            long slowRequestMillis,
            Consumer<RequestEvent> eventSink) {
        this.nanoTime = nanoTime;
        this.slowRequestNanos = TimeUnit.MILLISECONDS.toNanos(
                Math.max(1, slowRequestMillis));
        this.eventSink = eventSink;
    }

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest)
                || !(response instanceof HttpServletResponse httpResponse)
                || RequestPaths.isPublicStaticRequest(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        long start = nanoTime.getAsLong();
        RequestPerformanceContext.begin();
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedNanos = Math.max(0, nanoTime.getAsLong() - start);
            Snapshot sql = RequestPerformanceContext.finish();
            eventSink.accept(new RequestEvent(
                    httpRequest.getMethod(),
                    safePath(RequestPaths.relativePath(httpRequest)),
                    httpResponse.getStatus(),
                    elapsedNanos,
                    sql.sqlCount(),
                    sql.sqlDurationNanos(),
                    sql.maxSqlNanos(),
                    elapsedNanos >= slowRequestNanos));
        }
    }

    private static void writeLog(RequestEvent event) {
        String message =
                "{} method={} path={} status={} durationMs={} sqlCount={} sqlDurationMs={} maxSqlMs={}";
        Object[] values = {
                event.slow() ? "Slow HTTP request" : "HTTP request completed",
                event.method(),
                event.path(),
                event.status(),
                millis(event.elapsedNanos()),
                event.sqlCount(),
                millis(event.sqlDurationNanos()),
                millis(event.maxSqlNanos())
        };
        if (event.slow()) {
            logger.warn(message, values);
        } else {
            logger.info(message, values);
        }
    }

    private static String safePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        int length = Math.min(path.length(), MAX_PATH_LENGTH);
        StringBuilder safe = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            char character = path.charAt(index);
            safe.append(Character.isISOControl(character) ? '_' : character);
        }
        return safe.toString();
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long positiveLongProperty(String name, long defaultValue) {
        Long configured = Long.getLong(name);
        return configured == null || configured <= 0 ? defaultValue : configured;
    }

    record RequestEvent(
            String method,
            String path,
            int status,
            long elapsedNanos,
            int sqlCount,
            long sqlDurationNanos,
            long maxSqlNanos,
            boolean slow) {
    }
}
