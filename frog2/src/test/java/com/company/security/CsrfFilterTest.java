package com.company.security;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CsrfFilterTest {
    @Test
    void safePageRequestExposesTokenAndContinues() throws Exception {
        SessionFixture session = new SessionFixture();
        RequestFixture request = new RequestFixture("GET", "/frog2/dashboard", session);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new CsrfFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertNotNull(request.attributes.get(CsrfToken.REQUEST_ATTRIBUTE));
        assertTrue(CsrfToken.isValid(session.proxy(), (String) request.attributes.get(CsrfToken.REQUEST_ATTRIBUTE)));
    }

    @Test
    void headAndOptionsAreSafeMethods() throws Exception {
        for (String method : new String[] {"HEAD", "OPTIONS"}) {
            SessionFixture session = new SessionFixture();
            RequestFixture request = new RequestFixture(method, "/frog2/dashboard", session);
            AtomicInteger chainCalls = new AtomicInteger();

            new CsrfFilter().doFilter(
                    request.proxy(), new ResponseFixture().proxy(), countingChain(chainCalls));

            assertEquals(1, chainCalls.get(), method);
        }
    }

    @Test
    void validPostContinues() throws Exception {
        SessionFixture session = new SessionFixture();
        String token = CsrfToken.getOrCreate(session.proxy());
        RequestFixture request = new RequestFixture("POST", "/frog2/customers", session);
        request.parameters.put(CsrfToken.PARAMETER_NAME, token);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new CsrfFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertEquals(HttpServletResponse.SC_OK, response.status.get());
    }

    @Test
    void multipartHeaderTokenContinuesWithoutParsingRequestParameters() throws Exception {
        SessionFixture session = new SessionFixture();
        String token = CsrfToken.getOrCreate(session.proxy());
        RequestFixture request = new RequestFixture(
                "POST", "/frog2/file-repository/upload", session);
        request.headers.put(CsrfToken.HEADER_NAME, token);
        AtomicInteger chainCalls = new AtomicInteger();

        new CsrfFilter().doFilter(
                request.proxy(), new ResponseFixture().proxy(), countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertEquals(0, request.parameterReads.get());
    }

    @Test
    void invalidJsonPostReturns403WithoutCallingApplication() throws Exception {
        SessionFixture session = new SessionFixture();
        CsrfToken.getOrCreate(session.proxy());
        RequestFixture request = new RequestFixture("POST", "/frog2/comment", session);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new CsrfFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(0, chainCalls.get());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status.get());
        assertEquals("application/json", response.contentType);
        assertTrue(response.body.toString().contains("\"code\":\"invalid_csrf\""));
    }

    @Test
    void anonymousLoginCreatesShortLivedPreAuthenticationSession() throws Exception {
        RequestFixture request = new RequestFixture("GET", "/frog2/login", null);
        AtomicInteger chainCalls = new AtomicInteger();

        new CsrfFilter().doFilter(
                request.proxy(), new ResponseFixture().proxy(), countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertTrue(request.sessionCreated.get());
        assertEquals(600, request.session.maxInactiveInterval.get());
        assertEquals(1, request.session.maxInactiveIntervalUpdates.get());
    }

    @Test
    void anonymousProtectedAndStaticGetsDoNotCreateSession() throws Exception {
        for (String path : new String[] {
                "/frog2/dashboard",
                "/frog2/resources/css/base.css"
        }) {
            RequestFixture request = new RequestFixture("GET", path, null);
            AtomicInteger chainCalls = new AtomicInteger();

            new CsrfFilter().doFilter(
                    request.proxy(), new ResponseFixture().proxy(), countingChain(chainCalls));

            assertEquals(1, chainCalls.get(), path);
            assertFalse(request.sessionCreated.get(), path);
        }
    }

    @Test
    void existingAuthenticatedSessionTimeoutIsNotChanged() throws Exception {
        SessionFixture session = new SessionFixture();
        session.attributes.put("user", new Object());
        RequestFixture request = new RequestFixture("GET", "/frog2/dashboard", session);
        AtomicInteger chainCalls = new AtomicInteger();

        new CsrfFilter().doFilter(
                request.proxy(), new ResponseFixture().proxy(), countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertNotNull(request.attributes.get(CsrfToken.REQUEST_ATTRIBUTE));
        assertEquals(21_600, session.maxInactiveInterval.get());
        assertEquals(0, session.maxInactiveIntervalUpdates.get());
    }


    @Test
    void everyUnsafeMethodRequiresACsrfToken() throws Exception {
        for (String method : new String[] {"POST", "PUT", "PATCH", "DELETE", "TRACE"}) {
            SessionFixture session = new SessionFixture();
            RequestFixture request = new RequestFixture(method, "/frog2/dashboard", session);
            ResponseFixture response = new ResponseFixture();
            AtomicInteger chainCalls = new AtomicInteger();

            new CsrfFilter().doFilter(
                    request.proxy(), response.proxy(), countingChain(chainCalls));

            assertEquals(0, chainCalls.get(), method);
            assertEquals(HttpServletResponse.SC_FORBIDDEN, response.status.get(), method);
        }
    }

    private static FilterChain countingChain(AtomicInteger calls) {
        return (request, response) -> calls.incrementAndGet();
    }

    private static final class RequestFixture {
        private final String method;
        private final String uri;
        private SessionFixture session;
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, String> headers = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final AtomicBoolean sessionCreated = new AtomicBoolean();
        private final AtomicInteger parameterReads = new AtomicInteger();

        private RequestFixture(String method, String uri, SessionFixture session) {
            this.method = method;
            this.uri = uri;
            this.session = session;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] { HttpServletRequest.class },
                    (proxy, methodCall, args) -> switch (methodCall.getName()) {
                        case "getMethod" -> method;
                        case "getRequestURI" -> uri;
                        case "getContextPath" -> "/frog2";
                        case "getParameter" -> {
                            parameterReads.incrementAndGet();
                            yield parameters.get((String) args[0]);
                        }
                        case "getHeader" -> headers.get((String) args[0]);
                        case "getAttribute" -> attributes.get((String) args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getSession" -> getSession(args);
                        default -> defaultValue(methodCall.getReturnType());
                    });
        }

        private HttpSession getSession(Object[] args) {
            boolean create = args == null || args.length == 0 || Boolean.TRUE.equals(args[0]);
            if (session == null && create) {
                session = new SessionFixture();
                sessionCreated.set(true);
            }
            return session == null ? null : session.proxy();
        }
    }

    private static final class SessionFixture {
        private final Map<String, Object> attributes = new HashMap<>();
        private final AtomicInteger maxInactiveInterval = new AtomicInteger(21_600);
        private final AtomicInteger maxInactiveIntervalUpdates = new AtomicInteger();
        private HttpSession proxy;

        private HttpSession proxy() {
            if (proxy == null) {
                proxy = (HttpSession) Proxy.newProxyInstance(
                        HttpSession.class.getClassLoader(),
                        new Class<?>[] { HttpSession.class },
                        (ignored, method, args) -> switch (method.getName()) {
                            case "getAttribute" -> attributes.get((String) args[0]);
                            case "setAttribute" -> {
                                attributes.put((String) args[0], args[1]);
                                yield null;
                            }
                            case "getMaxInactiveInterval" -> maxInactiveInterval.get();
                            case "setMaxInactiveInterval" -> {
                                maxInactiveInterval.set((Integer) args[0]);
                                maxInactiveIntervalUpdates.incrementAndGet();
                                yield null;
                            }
                            default -> defaultValue(method.getReturnType());
                        });
            }
            return proxy;
        }
    }

    private static final class ResponseFixture {
        private final AtomicInteger status = new AtomicInteger(HttpServletResponse.SC_OK);
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private String contentType;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setStatus" -> {
                            status.set((Integer) args[0]);
                            yield null;
                        }
                        case "sendError" -> {
                            status.set((Integer) args[0]);
                            yield null;
                        }
                        case "setContentType" -> {
                            contentType = (String) args[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }
}
