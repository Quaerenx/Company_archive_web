package com.company.filter;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.UserDTO;
import com.company.model.DataAccessException;
import com.company.security.CredentialFingerprint;
import com.company.security.CsrfFilter;
import com.company.security.CsrfToken;
import com.company.controller.LogoutServlet;
import jakarta.servlet.http.Cookie;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.sql.SQLException;
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
    void anonymousHtmlCustomerDetailSaveRedirectsToLogin() throws Exception {
        RequestFixture request = new RequestFixture(
                "POST", "/frog2/customers", null);
        request.parameters.put("action", "saveDetail");
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthFilter().doFilter(
                request.proxy(), response.proxy(), countingChain(chainCalls));

        assertEquals(0, chainCalls.get());
        assertEquals("/frog2/login", response.redirect);
        assertEquals(1, response.redirectCalls.get());
        assertEquals(null, response.contentType);
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
    void healthEndpointsRemainAvailableWithoutAuthentication()
            throws Exception {
        for (String path : new String[] {
                "/frog2/health/live", "/frog2/health/ready"}) {
            RequestFixture request = new RequestFixture("GET", path, null);
            ResponseFixture response = new ResponseFixture();
            AtomicInteger calls = new AtomicInteger();

            new AuthFilter().doFilter(
                    request.proxy(), response.proxy(), countingChain(calls));

            assertEquals(1, calls.get(), path);
            assertEquals(0, response.redirectCalls.get(), path);
        }
    }

    @Test
    void authenticatedRequestContinues() throws Exception {
        SessionFixture session = new SessionFixture();
        UserDTO user = new UserDTO("tester", "", "Tester", "QA");
        session.attributes.put("user", user);
        RequestFixture request = new RequestFixture("GET", "/frog2/dashboard", session);
        ResponseFixture response = new ResponseFixture();
        AtomicInteger chainCalls = new AtomicInteger();

        new AuthFilter(principal -> true).doFilter(
                request.proxy(), response.proxy(), countingChain(chainCalls));

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

    @Test
    void credentialChangeRevokesTwoExistingSessionsAcrossFilterInstances() throws Exception {
        Map<String, String> currentCredentials = new HashMap<>();
        currentCredentials.put("tester", CredentialFingerprint.from("tester", "old-hash"));
        currentCredentials.put("other", CredentialFingerprint.from("other", "other-hash"));
        AuthFilter firstServer = new AuthFilter(user -> CredentialFingerprint.matches(
                user.getAuthenticationFingerprint(), currentCredentials.get(user.getUserId())));
        AuthFilter secondServer = new AuthFilter(user -> CredentialFingerprint.matches(
                user.getAuthenticationFingerprint(), currentCredentials.get(user.getUserId())));
        SessionFixture firstSession = authenticatedSession("tester", "old-hash");
        SessionFixture secondSession = authenticatedSession("tester", "old-hash");
        AtomicInteger calls = new AtomicInteger();
        firstServer.doFilter(new RequestFixture("GET", "/frog2/dashboard", firstSession).proxy(),
                new ResponseFixture().proxy(), countingChain(calls));
        secondServer.doFilter(new RequestFixture("GET", "/frog2/dashboard", secondSession).proxy(),
                new ResponseFixture().proxy(), countingChain(calls));
        assertEquals(2, calls.get());

        currentCredentials.put("tester", CredentialFingerprint.from("tester", "new-hash"));
        RequestFixture staleGet = new RequestFixture("GET", "/frog2/dashboard", firstSession);
        ResponseFixture getResponse = new ResponseFixture();
        firstServer.doFilter(staleGet.proxy(), getResponse.proxy(), countingChain(calls));
        ResponseFixture postResponse = new ResponseFixture();
        secondServer.doFilter(new RequestFixture("POST", "/frog2/comment", secondSession).proxy(),
                postResponse.proxy(), countingChain(calls));
        assertEquals(2, calls.get());
        assertEquals("/frog2/login", getResponse.redirect);
        assertEquals(401, postResponse.status.get());
        assertFalse(firstSession.attributes.containsKey("user"));
        assertFalse(secondSession.attributes.containsKey("user"));
        assertFalse(staleGet.attributes.containsKey("user"));

        secondServer.doFilter(new RequestFixture("GET", "/frog2/dashboard",
                authenticatedSession("tester", "new-hash")).proxy(),
                new ResponseFixture().proxy(), countingChain(calls));
        firstServer.doFilter(new RequestFixture("GET", "/frog2/dashboard",
                authenticatedSession("other", "other-hash")).proxy(),
                new ResponseFixture().proxy(), countingChain(calls));
        assertEquals(4, calls.get());
    }

    @Test
    void deletedAccountAndLegacySessionAreRejectedAndLoginGetClearsStalePrincipal() throws Exception {
        AuthFilter revoked = new AuthFilter(user -> false);
        for (String path : new String[] {"/frog2/dashboard", "/frog2/login"}) {
            SessionFixture session = authenticatedSession("deleted", "old-hash");
            RequestFixture request = new RequestFixture("GET", path, session);
            AtomicInteger calls = new AtomicInteger();
            revoked.doFilter(request.proxy(), new ResponseFixture().proxy(), countingChain(calls));
            assertFalse(session.attributes.containsKey("user"));
            assertFalse(request.attributes.containsKey("user"));
            assertEquals(path.endsWith("login") ? 1 : 0, calls.get());
        }
        SessionFixture legacy = new SessionFixture();
        legacy.attributes.put("user", new UserDTO("tester", "", "Tester", "QA"));
        AtomicInteger calls = new AtomicInteger();
        new AuthFilter().doFilter(new RequestFixture("GET", "/frog2/dashboard", legacy).proxy(),
                new ResponseFixture().proxy(), countingChain(calls));
        assertEquals(0, calls.get());
        assertFalse(legacy.attributes.containsKey("user"));
    }

    @Test
    void publicRoutesAndLoginPostDoNotDependOnCredentialDatabase() throws Exception {
        AuthFilter failing = new AuthFilter(user -> {
            throw new AssertionError("Public request queried credential database");
        });
        for (String path : new String[] {"/frog2/health/live", "/frog2/health/ready",
                "/frog2/resources/css/tokens.css", "/frog2/error/503.jsp"}) {
            AtomicInteger calls = new AtomicInteger();
            failing.doFilter(new RequestFixture("GET", path,
                    authenticatedSession("tester", "hash")).proxy(),
                    new ResponseFixture().proxy(), countingChain(calls));
            assertEquals(1, calls.get(), path);
        }
        AtomicInteger loginCalls = new AtomicInteger();
        failing.doFilter(new RequestFixture("POST", "/frog2/login",
                authenticatedSession("tester", "hash")).proxy(),
                new ResponseFixture().proxy(), countingChain(loginCalls));
        assertEquals(1, loginCalls.get());
    }

    @Test
    void credentialDatabaseFailureDoesNotAuthenticateRequest() {
        AuthFilter failing = new AuthFilter(user -> {
            throw DataAccessException.from(new SQLException("synthetic unavailable", "08006"));
        });
        AtomicInteger calls = new AtomicInteger();
        assertThrows(DataAccessException.class, () -> failing.doFilter(
                new RequestFixture("GET", "/frog2/dashboard", authenticatedSession("tester", "hash")).proxy(),
                new ResponseFixture().proxy(), countingChain(calls)));
        assertEquals(0, calls.get());
    }

    @Test
    void logoutStillInvalidatesSessionDuringDatabaseFailureAndRequiresCsrf() throws Exception {
        AuthFilter failingDatabase = new AuthFilter(user -> {
            throw DataAccessException.from(new SQLException("synthetic unavailable", "08006"));
        });
        SessionFixture session = authenticatedSession("tester", "original-hash");
        RequestFixture request = new RequestFixture("POST", "/frog2/logout", session);
        request.parameters.put(CsrfToken.PARAMETER_NAME, CsrfToken.getOrCreate(session.proxy()));
        ResponseFixture response = new ResponseFixture();
        failingDatabase.doFilter(request.proxy(), response.proxy(), (req, res) ->
                new CsrfFilter().doFilter(req, res, (validated, validatedResponse) ->
                        new LogoutServlet().service(
                                (HttpServletRequest) validated,
                                (HttpServletResponse) validatedResponse)));

        assertFalse(session.attributes.containsKey("user"));
        assertEquals("/frog2/login", response.redirect);
        assertEquals(0, response.expiredCookie.getMaxAge());

        SessionFixture invalidCsrfSession = authenticatedSession("tester", "original-hash");
        RequestFixture invalidCsrf = new RequestFixture("POST", "/frog2/logout", invalidCsrfSession);
        invalidCsrf.parameters.put(CsrfToken.PARAMETER_NAME, "wrong-token");
        ResponseFixture rejected = new ResponseFixture();
        AtomicInteger logoutCalls = new AtomicInteger();
        failingDatabase.doFilter(invalidCsrf.proxy(), rejected.proxy(), (req, res) ->
                new CsrfFilter().doFilter(req, res, countingChain(logoutCalls)));
        assertEquals(403, rejected.status.get());
        assertEquals(0, logoutCalls.get());
        assertTrue(invalidCsrfSession.attributes.containsKey("user"));
    }

    private static SessionFixture authenticatedSession(String userId, String hash) {
        SessionFixture session = new SessionFixture();
        UserDTO user = new UserDTO(userId, "", "Synthetic User", "QA");
        user.setAuthenticationFingerprint(CredentialFingerprint.from(userId, hash));
        session.attributes.put("user", user);
        return session;
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
                            case "invalidate" -> {
                                attributes.clear();
                                yield null;
                            }
                            case "getAttribute" -> attributes.get((String) args[0]);
                            case "setAttribute" -> {
                                attributes.put((String) args[0], args[1]);
                                yield null;
                            }
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
        private Cookie expiredCookie;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "isCommitted" -> false;
                        case "addCookie" -> {
                            expiredCookie = (Cookie) args[0];
                            yield null;
                        }
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
                        case "sendError" -> {
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

}
