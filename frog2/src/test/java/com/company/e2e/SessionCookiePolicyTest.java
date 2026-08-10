package com.company.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SessionCookiePolicyTest {
    @Test
    void acceptsRequiredSessionCookieAttributesCaseInsensitively() {
        SessionCookiePolicy.Inspection inspection = SessionCookiePolicy.inspect(
                List.of(
                        "unrelated=value; Path=/",
                        "JSESSIONID=redacted; Path=/frog2; HTTPONLY; SECURE; SAMESITE=STRICT"));

        assertTrue(inspection.sessionCookiePresent());
        assertTrue(inspection.httpOnly());
        assertTrue(inspection.secure());
        assertTrue(inspection.sameSiteStrict());
    }

    @Test
    void doesNotAcceptSubstringLookalikes() {
        SessionCookiePolicy.Inspection inspection = SessionCookiePolicy.inspect(
                List.of(
                        "XJSESSIONID=value; HttpOnly; Secure; SameSite=Strict",
                        "JSESSIONID=redacted; XHttpOnly=true; XSecure=true; SameSite=Lax"));

        assertTrue(inspection.sessionCookiePresent());
        assertFalse(inspection.httpOnly());
        assertFalse(inspection.secure());
        assertFalse(inspection.sameSiteStrict());
    }

    @Test
    void reportsMissingSessionCookieWithoutExposingValues() {
        SessionCookiePolicy.Inspection inspection = SessionCookiePolicy.inspect(
                List.of(
                        "JSESSIONID=; HttpOnly; Secure; SameSite=Strict",
                        "other=redacted; HttpOnly; Secure; SameSite=Strict"));

        assertFalse(inspection.sessionCookiePresent());
        assertFalse(inspection.httpOnly());
        assertFalse(inspection.secure());
        assertFalse(inspection.sameSiteStrict());
    }
}
