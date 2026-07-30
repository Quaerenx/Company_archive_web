package com.company.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.company.model.UserDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class SessionPrincipalTest {
    @Test
    void readsOnlyTypedSessionPrincipalWithoutCreatingSession() {
        UserDTO user = new UserDTO("tester", "", "Tester", "QA");
        SessionFixture session = new SessionFixture();
        session.attributes.put(SessionPrincipal.SESSION_ATTRIBUTE, user);
        RequestFixture request = new RequestFixture(session);

        assertSame(user, SessionPrincipal.from(session.proxy()));
        assertSame(user, SessionPrincipal.from(request.proxy()));
        assertFalse(request.sessionCreated.get());
    }

    @Test
    void rejectsMissingAndUnexpectedSessionValues() {
        SessionFixture unexpected = new SessionFixture();
        unexpected.attributes.put(SessionPrincipal.SESSION_ATTRIBUTE, "unexpected");

        assertNull(SessionPrincipal.from((HttpSession) null));
        assertNull(SessionPrincipal.from((HttpServletRequest) null));
        assertNull(SessionPrincipal.from(new SessionFixture().proxy()));
        assertNull(SessionPrincipal.from(unexpected.proxy()));
    }

    @Test
    void exposesTypedPrincipalAndRemovesStaleRequestValue() {
        UserDTO user = new UserDTO("tester", "", "Tester", "QA");
        SessionFixture session = new SessionFixture();
        session.attributes.put(SessionPrincipal.SESSION_ATTRIBUTE, user);
        RequestFixture authenticated = new RequestFixture(session);

        assertSame(user, SessionPrincipal.expose(authenticated.proxy()));
        assertSame(user, authenticated.attributes.get(SessionPrincipal.REQUEST_ATTRIBUTE));
        assertSame(user, SessionPrincipal.expose(authenticated.proxy(), session.proxy()));

        SessionFixture invalidSession = new SessionFixture();
        invalidSession.attributes.put(SessionPrincipal.SESSION_ATTRIBUTE, "unexpected");
        RequestFixture invalid = new RequestFixture(invalidSession);
        invalid.attributes.put(SessionPrincipal.REQUEST_ATTRIBUTE, "stale");
        assertNull(SessionPrincipal.expose(invalid.proxy()));
        assertFalse(invalid.attributes.containsKey(SessionPrincipal.REQUEST_ATTRIBUTE));
        assertFalse(invalidSession.attributes.containsKey(SessionPrincipal.SESSION_ATTRIBUTE));
    }

    @Test
    void readsAnExposedRequestPrincipalBeforeConsultingTheSession() {
        UserDTO requestUser = new UserDTO("request-user", "", "Request User", "QA");
        UserDTO sessionUser = new UserDTO("session-user", "", "Session User", "QA");
        SessionFixture session = new SessionFixture();
        session.attributes.put(SessionPrincipal.SESSION_ATTRIBUTE, sessionUser);
        RequestFixture request = new RequestFixture(session);
        request.attributes.put(SessionPrincipal.REQUEST_ATTRIBUTE, requestUser);

        assertSame(requestUser, SessionPrincipal.from(request.proxy()));
        assertFalse(request.sessionCreated.get());
    }

    @Test
    void storesSanitizedCopyAndClearRemovesIt() {
        UserDTO source = new UserDTO("tester", "must-not-enter-session", "Tester", "QA");
        SessionFixture session = new SessionFixture();

        SessionPrincipal.store(session.proxy(), source);

        UserDTO stored = (UserDTO) session.attributes.get(SessionPrincipal.SESSION_ATTRIBUTE);
        assertNotSame(source, stored);
        assertEquals("tester", stored.getUserId());
        assertEquals("", stored.getPassword());
        assertEquals("Tester", stored.getUserName());
        assertEquals("QA", stored.getDepartment());

        SessionPrincipal.clear(session.proxy());
        assertFalse(session.attributes.containsKey(SessionPrincipal.SESSION_ATTRIBUTE));
        SessionPrincipal.clear(null);
    }

    @Test
    void profileStyleReplacementPreservesUpdatedFieldsWithoutPassword() {
        SessionFixture session = new SessionFixture();
        UserDTO current = new UserDTO("tester", "legacy-secret", "Old Name", "Old Department");
        current.setUserName("New Name");
        current.setDepartment("New Department");

        SessionPrincipal.store(session.proxy(), current);

        UserDTO stored = SessionPrincipal.from(session.proxy());
        assertEquals("tester", stored.getUserId());
        assertEquals("", stored.getPassword());
        assertEquals("New Name", stored.getUserName());
        assertEquals("New Department", stored.getDepartment());
    }

    private static final class RequestFixture {
        private final SessionFixture session;
        private final Map<String, Object> attributes = new HashMap<>();
        private final AtomicBoolean sessionCreated = new AtomicBoolean();
        private HttpServletRequest proxy;

        private RequestFixture(SessionFixture session) {
            this.session = session;
        }

        private HttpServletRequest proxy() {
            if (proxy == null) {
                proxy = (HttpServletRequest) Proxy.newProxyInstance(
                        HttpServletRequest.class.getClassLoader(),
                        new Class<?>[] {HttpServletRequest.class},
                        (ignored, call, args) -> switch (call.getName()) {
                            case "getSession" -> {
                                boolean create = args == null
                                        || args.length == 0
                                        || Boolean.TRUE.equals(args[0]);
                                if (create) {
                                    sessionCreated.set(true);
                                }
                                yield session == null ? null : session.proxy();
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
