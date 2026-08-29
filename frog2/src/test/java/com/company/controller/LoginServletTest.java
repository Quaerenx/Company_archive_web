package com.company.controller;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.model.DataAccessException;
import com.company.model.UserDTO;
import com.company.security.LoginAttemptLimiter;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LoginServletTest {
    @Test
    void invalidCredentialsReturnUnauthorizedMessage() throws Exception {
        LoginServlet servlet = new LoginServlet((userId, password) -> null);
        RequestFixture request = new RequestFixture();
        request.parameters.put("userId", "missing");
        request.parameters.put("password", "wrong");
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status.get());
        assertEquals("아이디 또는 비밀번호가 올바르지 않습니다.",
                request.attributes.get("errorMessage"));
        assertTrue(request.forwarded.get());
    }

    @Test
    void databaseFailureIsNotReportedAsInvalidCredentials() throws Exception {
        LoginServlet servlet = new LoginServlet((userId, password) -> {
            throw DataAccessException.from("authenticate user", new SQLException("down", "08001"));
        });
        RequestFixture request = new RequestFixture();
        request.parameters.put("userId", "tester");
        request.parameters.put("password", "secret");
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertEquals(HttpServletResponse.SC_SERVICE_UNAVAILABLE, response.status.get());
        String message = (String) request.attributes.get("errorMessage");
        assertTrue(message.contains("데이터 서비스"));
        assertFalse(message.contains("아이디 또는 비밀번호"));
        assertTrue(request.forwarded.get());
    }

    @Test
    void successfulLoginRotatesAnonymousSessionWithoutCopyingItsShortTimeout() throws Exception {
        UserDTO user = new UserDTO("tester", "must-not-enter-session", "Tester", "QA");
        LoginServlet servlet = new LoginServlet((userId, password) -> user);
        SessionFixture anonymousSession = new SessionFixture(600);
        SessionFixture authenticatedSession = new SessionFixture(21_600);
        RequestFixture request = new RequestFixture(anonymousSession, authenticatedSession);
        request.parameters.put("userId", "tester");
        request.parameters.put("password", "secret");
        ResponseFixture response = new ResponseFixture();

        servlet.doPost(request.proxy(), response.proxy());

        assertTrue(anonymousSession.invalidated.get());
        assertEquals(1, request.createSessionCalls.get());
        UserDTO stored = (UserDTO) authenticatedSession.attributes.get("user");
        assertNotSame(user, stored);
        assertEquals("tester", stored.getUserId());
        assertEquals("", stored.getPassword());
        assertEquals("Tester", stored.getUserName());
        assertEquals("QA", stored.getDepartment());
        assertEquals("/frog2/dashboard", response.redirect);
        assertEquals(21_600, authenticatedSession.maxInactiveInterval.get());
        assertEquals(0, authenticatedSession.maxInactiveIntervalUpdates.get());
    }

    @Test
    void repeatedFailuresAreRejectedBeforeAnotherDatabaseAuthentication()
            throws Exception {
        AtomicInteger authenticationCalls = new AtomicInteger();
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(
                Clock.systemUTC(),
                2,
                100,
                Duration.ofMinutes(5),
                Duration.ofMinutes(1),
                100);
        LoginServlet servlet = new LoginServlet(
                (userId, password) -> {
                    authenticationCalls.incrementAndGet();
                    return null;
                },
                limiter);

        ResponseFixture firstResponse = submitInvalidLogin(servlet);
        ResponseFixture secondResponse = submitInvalidLogin(servlet);
        ResponseFixture thirdResponse = submitInvalidLogin(servlet);

        assertEquals(
                HttpServletResponse.SC_UNAUTHORIZED,
                firstResponse.status.get());
        assertEquals(429, secondResponse.status.get());
        assertEquals(429, thirdResponse.status.get());
        assertTrue(secondResponse.headers.containsKey("Retry-After"));
        assertEquals(2, authenticationCalls.get());
    }

    private static ResponseFixture submitInvalidLogin(LoginServlet servlet)
            throws Exception {
        RequestFixture request = new RequestFixture();
        request.parameters.put("userId", "tester");
        request.parameters.put("password", "wrong");
        ResponseFixture response = new ResponseFixture();
        servlet.doPost(request.proxy(), response.proxy());
        return response;
    }

    private static final class RequestFixture {
        private final Map<String, String> parameters = new HashMap<>();
        private final Map<String, Object> attributes = new HashMap<>();
        private final AtomicBoolean forwarded = new AtomicBoolean();
        private final AtomicInteger createSessionCalls = new AtomicInteger();
        private final SessionFixture oldSession;
        private final SessionFixture newSession;

        private RequestFixture() {
            this(null, null);
        }

        private RequestFixture(SessionFixture oldSession, SessionFixture newSession) {
            this.oldSession = oldSession;
            this.newSession = newSession;
        }

        private HttpServletRequest proxy() {
            RequestDispatcher dispatcher = (RequestDispatcher) Proxy.newProxyInstance(
                    RequestDispatcher.class.getClassLoader(),
                    new Class<?>[] { RequestDispatcher.class },
                    (ignored, call, args) -> {
                        if ("forward".equals(call.getName())) {
                            forwarded.set(true);
                        }
                        return defaultValue(call.getReturnType());
                    });
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] { HttpServletRequest.class },
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getParameter" -> parameters.get((String) args[0]);
                        case "getContextPath" -> "/frog2";
                        case "getRemoteAddr" -> "127.0.0.1";
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getAttribute" -> attributes.get((String) args[0]);
                        case "getRequestDispatcher" -> dispatcher;
                        case "getSession" -> {
                            boolean create = args == null
                                    || args.length == 0
                                    || Boolean.TRUE.equals(args[0]);
                            if (create) {
                                createSessionCalls.incrementAndGet();
                                yield newSession == null ? null : newSession.proxy();
                            }
                            yield oldSession == null ? null : oldSession.proxy();
                        }
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class SessionFixture {
        private final Map<String, Object> attributes = new HashMap<>();
        private final AtomicBoolean invalidated = new AtomicBoolean();
        private final AtomicInteger maxInactiveInterval;
        private final AtomicInteger maxInactiveIntervalUpdates = new AtomicInteger();
        private HttpSession proxy;

        private SessionFixture(int maxInactiveInterval) {
            this.maxInactiveInterval = new AtomicInteger(maxInactiveInterval);
        }

        private HttpSession proxy() {
            if (proxy == null) {
                proxy = (HttpSession) Proxy.newProxyInstance(
                        HttpSession.class.getClassLoader(),
                        new Class<?>[] {HttpSession.class},
                        (ignored, call, args) -> switch (call.getName()) {
                            case "getAttribute" -> attributes.get((String) args[0]);
                            case "setAttribute" -> {
                                attributes.put((String) args[0], args[1]);
                                yield null;
                            }
                            case "invalidate" -> {
                                invalidated.set(true);
                                yield null;
                            }
                            case "getMaxInactiveInterval" -> maxInactiveInterval.get();
                            case "setMaxInactiveInterval" -> {
                                maxInactiveInterval.set((Integer) args[0]);
                                maxInactiveIntervalUpdates.incrementAndGet();
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
        private final Map<String, String> headers = new HashMap<>();

        private String redirect;
        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] { HttpServletResponse.class },
                    (ignored, call, args) -> switch (call.getName()) {
                        case "setStatus" -> {
                            status.set((Integer) args[0]);
                            yield null;
                        }
                        case "sendRedirect" -> {
                            redirect = (String) args[0];
                            yield null;
                        }
                        case "setHeader" -> {
                            headers.put((String) args[0], (String) args[1]);
                            yield null;
                        }
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

}
