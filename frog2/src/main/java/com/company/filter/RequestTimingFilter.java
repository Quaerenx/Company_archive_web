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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public final class RequestTimingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(RequestTimingFilter.class);
    private static final int MAX_PATH_LENGTH = 256;
    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

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

        String requestId = UUID.randomUUID().toString().replace("-", "");
        String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        try {
            long start = nanoTime.getAsLong();
            RequestPerformanceContext.begin();
            try {
                chain.doFilter(request, response);
            } finally {
                long elapsedNanos = Math.max(
                        0, nanoTime.getAsLong() - start);
                Snapshot performance = RequestPerformanceContext.finish();
                eventSink.accept(new RequestEvent(
                        requestId,
                        httpRequest.getMethod(),
                        safePath(RequestPaths.relativePath(httpRequest)),
                        httpResponse.getStatus(),
                        elapsedNanos,
                        performance.sqlCount(),
                        performance.sqlDurationNanos(),
                        performance.maxSqlNanos(),
                        performance.operation().logValue(),
                        performance.dbAcquisitionCount(),
                        performance.dbAcquisitionDurationNanos(),
                        performance.maxDbAcquisitionNanos(),
                        performance.fileSnapshotCacheHits(),
                        performance.fileSnapshotCacheMisses(),
                        performance.fileSnapshotScanCount(),
                        performance.fileSnapshotScanDurationNanos(),
                        performance.maxFileSnapshotScanNanos(),
                        elapsedNanos >= slowRequestNanos));
            }
        } finally {
            if (previousRequestId == null) {
                MDC.remove(REQUEST_ID_MDC_KEY);
            } else {
                MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
            }
        }
    }

    private static void writeLog(RequestEvent event) {
        String message =
                "{} requestId={} method={} path={} status={} durationMs={} operation={} sqlCount={} sqlDurationMs={} maxSqlMs={} dbAcquireCount={} dbAcquireDurationMs={} maxDbAcquireMs={} fileCacheHits={} fileCacheMisses={} fileScanCount={} fileScanDurationMs={} maxFileScanMs={}";
        Object[] values = {
                event.slow() ? "Slow HTTP request" : "HTTP request completed",
                event.requestId(),
                event.method(),
                event.path(),
                event.status(),
                millis(event.elapsedNanos()),
                event.operation(),
                event.sqlCount(),
                millis(event.sqlDurationNanos()),
                millis(event.maxSqlNanos()),
                event.dbAcquisitionCount(),
                millis(event.dbAcquisitionDurationNanos()),
                millis(event.maxDbAcquisitionNanos()),
                event.fileSnapshotCacheHits(),
                event.fileSnapshotCacheMisses(),
                event.fileSnapshotScanCount(),
                millis(event.fileSnapshotScanDurationNanos()),
                millis(event.maxFileSnapshotScanNanos())
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
        String withoutPathParameters = stripPathParameters(path);
        int length = Math.min(
                withoutPathParameters.length(), MAX_PATH_LENGTH);
        StringBuilder safe = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            char character = withoutPathParameters.charAt(index);
            safe.append(Character.isISOControl(character) ? '_' : character);
        }
        return safe.toString();
    }

    private static String stripPathParameters(String path) {
        StringBuilder result = new StringBuilder(path.length());
        boolean inParameter = false;
        for (int index = 0; index < path.length(); index++) {
            char character = path.charAt(index);
            if (character == ';') {
                inParameter = true;
            } else if (character == '/') {
                inParameter = false;
                result.append(character);
            } else if (!inParameter) {
                result.append(character);
            }
        }
        return result.isEmpty() ? "/" : result.toString();
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long positiveLongProperty(String name, long defaultValue) {
        Long configured = Long.getLong(name);
        return configured == null || configured <= 0 ? defaultValue : configured;
    }

    record RequestEvent(
            String requestId,
            String method,
            String path,
            int status,
            long elapsedNanos,
            int sqlCount,
            long sqlDurationNanos,
            long maxSqlNanos,
            String operation,
            int dbAcquisitionCount,
            long dbAcquisitionDurationNanos,
            long maxDbAcquisitionNanos,
            int fileSnapshotCacheHits,
            int fileSnapshotCacheMisses,
            int fileSnapshotScanCount,
            long fileSnapshotScanDurationNanos,
            long maxFileSnapshotScanNanos,
            boolean slow) {
    }
}
