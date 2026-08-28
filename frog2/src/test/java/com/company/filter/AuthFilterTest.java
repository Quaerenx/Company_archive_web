package com.company.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.UserDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AuthFilterTest {
    @Test
    void anonymousPublicAssetsContinueWithoutRedirect() throws Exception {
        for (String path : new String[] {
                "/frog2/favicon.png",
                "/frog2/favicon.ico",
                "/frog2/favicon.svg",
                "/frog2/resources/images/archive-logo.svg",
                "/frog2/resources/fonts/ibm-plex-sans-kr/1.1.0/font.woff2"
        }) {
            RequestFixture request = new RequestFixture("GET", path, null);
            ResponseFixture response = new ResponseFixture();
            AtomicInteger chainCalls = new AtomicInteger();

            new AuthFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

            assertEquals(1, chainCalls.get(), path);
            assertEquals(0, response.redirectCalls.get(), path);
        }
    }

    @Test
    void anonymousCommentPostReturnsJsonUnauthorizedWithoutRedirect() throws Exception {
        RequestFixture request = new RequestFixture("POST", "/frog2/comment", null);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(0, chainCalls.get());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status.get());
        assertEquals("application/json", response.contentType);
        assertTrue(response.body.toString().contains("\"code\":\"authentication_required\""));
        assertEquals(0, response.redirectCalls.get());
        assertEquals("no-store", response.headers.get("Cache-Control"));
    }

    @Test
    void anonymousCustomerDetailRequestReturnsJsonUnauthorizedWithoutRedirect() throws Exception {
        RequestFixture request = new RequestFixture("GET", "/frog2/customers", null);
        request.parameters.put("action", "getDetail");
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(0, chainCalls.get());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status.get());
        assertEquals("application/json", response.contentType);
        assertTrue(response.body.toString().contains("\"code\":\"authentication_required\""));
        assertEquals(0, response.redirectCalls.get());
    }

    @Test
    void anonymousDashboardRedirectsToContextLogin() throws Exception {
        RequestFixture request = new RequestFixture("GET", "/frog2/dashboard", null);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(0, chainCalls.get());
        assertEquals("/frog2/login", response.redirect);
        assertEquals(1, response.redirectCalls.get());
    }

    @Test
    void authenticatedRequestContinues() throws Exception {
        SessionFixture session = new SessionFixture();
        UserDTO user = new UserDTO("tester", "", "Tester", "QA");
        session.attributes.put("user", user);
        RequestFixture request = new RequestFixture("GET", "/frog2/dashboard", session);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthFilter().doFilter(request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(1, chainCalls.get());
        assertEquals(0, response.redirectCalls.get());
        assertSame(user, request.attributes.get("user"));
    }


    @Test
    void uploadAliasesUseHtmlForGetAndJsonForPost() throws Exception {
        for (String path : new String[] {
                "/frog2/file-repository/upload",
                "/frog2/filerepo/filerepo_upload.jsp",
                "/frog2/filerepo/filerepo_uploadProcess.jsp"}) {
            RequestFixture get = new RequestFixture("GET", path, null);
            ResponseFixture getResponse = new ResponseFixture();
            new AuthFilter().doFilter(
                    get.proxy(), getResponse.proxy(), countingChain(new AtomicInteger()));
            assertEquals("/frog2/login", getResponse.redirect, path);

            RequestFixture post = new RequestFixture("POST", path, null);
            ResponseFixture postResponse = new ResponseFixture();
            new AuthFilter().doFilter(
                    post.proxy(), postResponse.proxy(), countingChain(new AtomicInteger()));
            assertEquals(HttpServletResponse.SC_UNAUTHORIZED, postResponse.status.get(), path);
            assertEquals("application/json", postResponse.contentType, path);
        }
    }

    @Test
    void invalidSessionPrincipalIsClearedAndRejected() throws Exception {
        SessionFixture session = new SessionFixture();
        session.attributes.put("user", "unexpected");
        RequestFixture request = new RequestFixture("GET", "/frog2/dashboard", session);
        ResponseFixture response = new ResponseFixture();

        new AuthFilter().doFilter(
                request.proxy(), response.proxy(), countingChain(new AtomicInteger()));

        assertEquals("/frog2/login", response.redirect);
        assertFalse(session.attributes.containsKey("user"));
        assertFalse(request.attributes.containsKey("user"));
    }

    @Test
    void anonymousPostToStaticLookingPathIsNotPublic() throws Exception {
        RequestFixture request = new RequestFixture("POST", "/frog2/resources/js/app.js", null);
        ResponseFixture response = new ResponseFixture();

        new AuthFilter().doFilter(request.proxy(), response.proxy(), countingChain(new AtomicInteger()));

        assertEquals("/frog2/login", response.redirect);

        for (String path : new String[] {
                "/frog2/resources/hidden.jsp",
                "/frog2/resources/hidden.class",
                "/frog2/resources/hidden.jsp;asset=.js",
                "/frog2/resources/hidden.jsp%3Basset=.js"}) {
            RequestFixture unsafe = new RequestFixture("GET", path, null);
            ResponseFixture unsafeResponse = new ResponseFixture();
            new AuthFilter().doFilter(
                    unsafe.proxy(), unsafeResponse.proxy(), countingChain(new AtomicInteger()));
            assertEquals("/frog2/login", unsafeResponse.redirect, path);
        }
    }

    private static FilterChain countingChain(AtomicInteger calls) {
        return (request, response) -> calls.incrementAndGet();
    }

    private static final class RequestFixture {
        private final String method;
        private final String uri;
        private final SessionFixture session;
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();

        private RequestFixture(String method, String uri, SessionFixture session) {
            this.method = method;
            this.uri = uri;
            this.session = session;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getMethod" -> method;
                        case "getRequestURI" -> uri;
                        case "getContextPath" -> "/frog2";
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getHeader" -> null;
                        case "getAttribute" -> attributes.get((String) args[0]);
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "removeAttribute" -> {
                            attributes.remove((String) args[0]);
                            yield null;
                        }
                        case "getSession" -> session == null ? null : session.proxy();
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class SessionFixture {
        private final Map<String, Object> attributes = new HashMap<>();
        private HttpSession proxy;

        private HttpSession proxy() {
            if (proxy == null) {
                proxy = (HttpSession) Proxy.newProxyInstance(
                        HttpSession.class.getClassLoader(),
                        new Class<?>[] {HttpSession.class},
                        (ignored, call, args) -> switch (call.getName()) {
                            case "getAttribute" -> attributes.get((String) args[0]);
                            case "removeAttribute" -> {
                                attributes.remove((String) args[0]);
                                yield null;
                            }
                            default -> defaultValue(call.getReturnType());
                        });
            }
            return proxy;
        }
    }

    private static final class ResponseFixture {
        private final AtomicInteger status = new AtomicInteger(HttpServletResponse.SC_OK);
        private final AtomicInteger redirectCalls = new AtomicInteger();
        private final StringWriter body = new StringWriter();
        private final PrintWriter writer = new PrintWriter(body);
        private final Map<String, String> headers = new HashMap<>();
        private String contentType;
        private String redirect;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "isCommitted" -> false;
                        case "resetBuffer" -> {
                            body.getBuffer().setLength(0);
                            yield null;
                        }
                        case "setHeader" -> {
                            headers.put((String) args[0], (String) args[1]);
                            yield null;
                        }
                        case "setStatus" -> {
                            status.set((Integer) args[0]);
                            yield null;
                        }
                        case "setContentType" -> {
                            contentType = (String) args[0];
                            yield null;
                        }
                        case "getWriter" -> writer;
                        case "sendRedirect" -> {
                            redirect = (String) args[0];
                            redirectCalls.incrementAndGet();
                            yield null;
                        }
                        default -> defaultValue(call.getReturnType());
                    });
        }
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
        return 0;
    }
}
