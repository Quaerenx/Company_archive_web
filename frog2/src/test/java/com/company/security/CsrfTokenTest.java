package com.company.security;

import static com.company.testsupport.ProxyDefaults.defaultValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.HttpSession;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsrfTokenTest {
    @Test
    void createsUrlSafeSessionTokenAndUsesConstantTimeComparison() {
        Map<String, Object> attributes = new HashMap<>();
        HttpSession session = session(attributes);

        String first = CsrfToken.getOrCreate(session);
        String second = CsrfToken.getOrCreate(session);

        assertSame(first, second);
        assertEquals(43, first.length());
        assertTrue(first.matches("[A-Za-z0-9_-]+"));
        assertTrue(CsrfToken.isValid(session, first));
        assertFalse(CsrfToken.isValid(session, "wrong"));
        assertFalse(CsrfToken.isValid(session, ""));
        assertFalse(CsrfToken.isValid(null, first));
    }

    private static HttpSession session(Map<String, Object> attributes) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[] { HttpSession.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> attributes.get((String) args[0]);
                    case "setAttribute" -> {
                        attributes.put((String) args[0], args[1]);
                        yield null;
                    }
                    default -> defaultValue(method.getReturnType());
                });
    }
}
