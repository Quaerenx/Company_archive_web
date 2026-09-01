package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ViewTransitionContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final String ROUTING_SCRIPT =
            "/resources/js/view-transition-routing.js";

    @Test
    void loginAndDashboardLoadTheRouteGateBeforeFirstRender() throws Exception {
        String login = read("login.jsp");
        String dashboard = read("dashboard.jsp");
        String header = read("includes/header.jsp");

        String loginScript = "<script src=\"${pageContext.request.contextPath}"
                + ROUTING_SCRIPT
                + "?v=${initParam.frog2AssetVersion}\"></script>";
        assertTrue(login.contains(loginScript));
        assertTrue(login.indexOf(loginScript) < login.indexOf("</head>"));
        assertFalse(loginScript.contains("async"));
        assertFalse(loginScript.contains("defer"));

        assertTrue(dashboard.contains(
                "var=\"pageHeadScript\" value=\"" + ROUTING_SCRIPT + "\""));
        assertTrue(header.contains("not empty pageHeadScript"));
        assertTrue(header.contains("items=\"${pageHeadScript}\""));
        assertTrue(header.indexOf("items=\"${pageHeadScript}\"")
                < header.indexOf("</head>"));
        assertFalse(header.contains("${pageHeadScript}" + " async"));
        assertFalse(header.contains("${pageHeadScript}" + " defer"));
    }

    @Test
    void stageMotionRemainsMotionSafeWithoutAnInvisibleLogoutPeek() throws Exception {
        String styles = read("resources/css/view-transitions.css");

        assertTrue(styles.contains("::view-transition-old(archive-peek-left)"));
        assertTrue(styles.contains("::view-transition-old(archive-peek-center)"));
        assertTrue(styles.contains("::view-transition-old(archive-peek-right)"));
        assertFalse(styles.contains("::view-transition-new(archive-peek-left)"));
        assertFalse(styles.contains("::view-transition-new(archive-peek-center)"));
        assertFalse(styles.contains("::view-transition-new(archive-peek-right)"));
        assertFalse(styles.contains("@keyframes archive-peek-enter"));
        assertTrue(styles.matches(
                "(?s).*@media \\(prefers-reduced-motion: reduce\\)\\s*\\{.*"
                        + "::view-transition-group\\(\\*\\).*"
                        + "::view-transition-old\\(\\*\\).*"
                        + "::view-transition-new\\(\\*\\).*"
                        + "animation-delay:\\s*0ms !important;.*"
                        + "animation-duration:\\s*0\\.01ms !important;.*"));
    }

    @Test
    void flyingDocumentsStayBehindTheEnteringWorkspace() throws Exception {
        String styles = read("resources/css/view-transitions.css");

        assertTrue(styles.contains("::view-transition-group(root) {\n    z-index: 0;\n}"));
        assertTrue(styles.contains(
                "::view-transition-group(archive-peek-left),\n"
                        + "::view-transition-group(archive-peek-center),\n"
                        + "::view-transition-group(archive-peek-right) {\n"
                        + "    z-index: 1;\n"
                        + "}"));
        assertTrue(styles.contains(
                "::view-transition-group(archive-stage) {\n    z-index: 2;\n}"));
        assertTrue(styles.contains(
                "::view-transition-group(archive-login-card) {\n    z-index: 3;\n}"));
        assertTrue(styles.contains(
                "::view-transition-group(archive-logo) {\n    z-index: 4;\n}"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
