package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FlashMessageTest {
    @Test
    void messagesCanBeConsumedInReverseCreationOrderWithoutCrossTalk() {
        SessionFixture session = new SessionFixture();
        String firstToken = FlashMessage.store(
                new RequestFixture(session).proxy(), "first", "success", 1_000L);
        String secondToken = FlashMessage.store(
                new RequestFixture(session).proxy(), "second", "error", 1_001L);

        RequestFixture secondGet = new RequestFixture(session, secondToken);
        FlashMessage.expose(secondGet.proxy(), 2_000L);
        assertEquals("second", secondGet.attributes.get("message"));
        assertEquals("error", secondGet.attributes.get("messageType"));

        RequestFixture firstGet = new RequestFixture(session, firstToken);
        FlashMessage.expose(firstGet.proxy(), 2_001L);
        assertEquals("first", firstGet.attributes.get("message"));
        assertEquals("success", firstGet.attributes.get("messageType"));
    }

    @Test
    void tokenIsSingleUse() {
        SessionFixture session = new SessionFixture();
        String token = FlashMessage.store(
                new RequestFixture(session).proxy(), "saved", "success", 1_000L);

        RequestFixture firstGet = new RequestFixture(session, token);
        FlashMessage.expose(firstGet.proxy(), 2_000L);
        assertEquals("saved", firstGet.attributes.get("message"));

        RequestFixture secondGet = new RequestFixture(session, token);
        FlashMessage.expose(secondGet.proxy(), 2_001L);
        assertNull(secondGet.attributes.get("message"));
        assertNull(secondGet.attributes.get("messageType"));
    }

    @Test
    void missingAndInvalidTokensDoNotConsumeAStoredMessage() {
        SessionFixture session = new SessionFixture();
        String token = FlashMessage.store(
                new RequestFixture(session).proxy(), "notice", "warning", 1_000L);

        FlashMessage.expose(new RequestFixture(session).proxy(), 2_000L);
        FlashMessage.expose(new RequestFixture(session, "not-a-valid-token").proxy(), 2_000L);

        RequestFixture validGet = new RequestFixture(session, token);
        FlashMessage.expose(validGet.proxy(), 2_000L);
        assertEquals("notice", validGet.attributes.get("message"));
        assertEquals("warning", validGet.attributes.get("messageType"));
    }

    @Test
    void expiredMessageIsNotExposed() {
        SessionFixture session = new SessionFixture();
        String token = FlashMessage.store(
                new RequestFixture(session).proxy(), "old", "info", 1_000L);

        RequestFixture get = new RequestFixture(session, token);
        FlashMessage.expose(get.proxy(), 1_000L + FlashMessage.TTL_MILLIS);

        assertNull(get.attributes.get("message"));
        assertNull(get.attributes.get("messageType"));
    }

    @Test
    void storeEvictsOldestMessageAtBound() {
        SessionFixture session = new SessionFixture();
        String oldestToken = null;
        String newestToken = null;
        for (int index = 0; index <= FlashMessage.MAX_MESSAGES; index++) {
            String token = FlashMessage.store(
                    new RequestFixture(session).proxy(),
                    "message-" + index,
                    "info",
                    1_000L + index);
            if (index == 0) {
                oldestToken = token;
            }
            newestToken = token;
        }

        RequestFixture oldestGet = new RequestFixture(session, oldestToken);
        FlashMessage.expose(oldestGet.proxy(), 2_000L);
        assertNull(oldestGet.attributes.get("message"));

        RequestFixture newestGet = new RequestFixture(session, newestToken);
        FlashMessage.expose(newestGet.proxy(), 2_000L);
        assertEquals("message-20", newestGet.attributes.get("message"));
    }

    @Test
    void unknownMessageTypeIsNormalized() {
        SessionFixture session = new SessionFixture();
        String token = FlashMessage.store(
                new RequestFixture(session).proxy(),
                "notice",
                "unexpected-css-class",
                1_000L);

        RequestFixture get = new RequestFixture(session, token);
        FlashMessage.expose(get.proxy(), 2_000L);

        assertEquals("info", get.attributes.get("messageType"));
    }

    @Test
    void redirectPreservesQueryAndFragmentAndExposesItsToken() throws Exception {
        SessionFixture session = new SessionFixture();
        RequestFixture post = new RequestFixture(session);
        ResponseFixture response = new ResponseFixture();

        FlashMessage.redirect(
                post.proxy(),
                response.proxy(),
                "meeting?view=list#recent",
                "saved",
                "success");

        assertTrue(response.redirect.startsWith("meeting?view=list&_flash="));
        assertTrue(response.redirect.endsWith("#recent"));
        String token = response.redirect.substring(
                response.redirect.indexOf("_flash=") + 7,
                response.redirect.indexOf('#'));
        assertEquals(32, token.length());

        RequestFixture get = new RequestFixture(session, token);
        FlashMessage.expose(get.proxy());
        assertEquals("saved", get.attributes.get("message"));
    }

    @Test
    void blankMessageDoesNotCreateAFlashRedirectParameter() throws Exception {
        SessionFixture session = new SessionFixture();
        ResponseFixture response = new ResponseFixture();

        FlashMessage.redirect(
                new RequestFixture(session).proxy(),
                response.proxy(),
                "customers?view=list",
                " ",
                "success");

        assertEquals("customers?view=list", response.redirect);
        assertFalse(response.redirect.contains(FlashMessage.PARAMETER_NAME));
    }

    private static final class RequestFixture {
        private final SessionFixture session;
        private final String token;
        private final Map<String, Object> attributes = new HashMap<>();

        private RequestFixture(SessionFixture session) {
            this(session, null);
        }

        private RequestFixture(SessionFixture session, String token) {
            this.session = session;
            this.token = token;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] {HttpServletRequest.class},
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session == null ? null : session.proxy();
                        case "getParameter" -> FlashMessage.PARAMETER_NAME.equals(args[0])
                                ? token
                                : null;
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getAttribute" -> attributes.get((String) args[0]);
                        default -> defaultValue(call.getReturnType());
                    });
        }
    }

    private static final class ResponseFixture {
        private String redirect;

        private HttpServletResponse proxy() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[] {HttpServletResponse.class},
                    (ignored, call, args) -> {
                        if ("sendRedirect".equals(call.getName())) {
                            redirect = (String) args[0];
                        }
                        return defaultValue(call.getReturnType());
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
