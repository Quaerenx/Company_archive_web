package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FlashMessageTest {
    @Test
    void storedMessageIsExposedExactlyOnce() {
        SessionFixture session = new SessionFixture();
        RequestFixture post = new RequestFixture(session);
        FlashMessage.store(post.proxy(), "saved", "success");

        RequestFixture firstGet = new RequestFixture(session);
        FlashMessage.expose(firstGet.proxy());
        assertEquals("saved", firstGet.attributes.get("message"));
        assertEquals("success", firstGet.attributes.get("messageType"));

        RequestFixture secondGet = new RequestFixture(session);
        FlashMessage.expose(secondGet.proxy());
        assertNull(secondGet.attributes.get("message"));
        assertNull(secondGet.attributes.get("messageType"));
    }

    @Test
    void unknownMessageTypeIsNormalized() {
        SessionFixture session = new SessionFixture();
        RequestFixture post = new RequestFixture(session);
        FlashMessage.store(post.proxy(), "notice", "unexpected-css-class");

        RequestFixture get = new RequestFixture(session);
        FlashMessage.expose(get.proxy());

        assertEquals("info", get.attributes.get("messageType"));
    }

    private static final class RequestFixture {
        private final SessionFixture session;
        private final Map<String, Object> attributes = new HashMap<>();

        private RequestFixture(SessionFixture session) {
            this.session = session;
        }

        private HttpServletRequest proxy() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[] { HttpServletRequest.class },
                    (ignored, call, args) -> switch (call.getName()) {
                        case "getSession" -> session.proxy();
                        case "setAttribute" -> {
                            attributes.put((String) args[0], args[1]);
                            yield null;
                        }
                        case "getAttribute" -> attributes.get((String) args[0]);
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
                        new Class<?>[] { HttpSession.class },
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
