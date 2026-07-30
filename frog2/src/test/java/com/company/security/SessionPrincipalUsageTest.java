package com.company.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SessionPrincipalUsageTest {
    private static final Path CONTROLLERS =
            Path.of("src/main/java/com/company/controller");

    @Test
    void loginAndProfileRefreshUseSanitizedSessionStore() throws Exception {
        String login = Files.readString(CONTROLLERS.resolve("LoginServlet.java"));
        String myPage = Files.readString(CONTROLLERS.resolve("MyPageServlet.java"));

        assertTrue(login.contains("SessionPrincipal.store(session, user);"));
        assertTrue(myPage.contains("SessionPrincipal.store(session, currentUser);"));
        assertFalse(login.contains("session.setAttribute(\"user\""));
        assertFalse(myPage.contains("session.setAttribute(\"user\""));
    }

    @Test
    void controllersNoLongerUseLegacySessionHelperOrRelativeAuthRedirects() throws Exception {
        try (var paths = Files.walk(CONTROLLERS)) {
            for (Path path : paths.filter(file -> file.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("SessionUser"), path::toString);
                assertFalse(source.contains("sendRedirect(\"login\")"), path::toString);
                assertFalse(source.contains("sendRedirect(\"dashboard\")"), path::toString);
            }
        }
    }
}
