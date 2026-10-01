package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        String cardGroup = block(styles, "::view-transition-group(archive-login-card) {");
        assertTrue(cardGroup.contains("z-index: 3;"));
        assertTrue(cardGroup.contains("backdrop-filter: none;"));
        assertTrue(cardGroup.contains("-webkit-backdrop-filter: none;"));
        assertTrue(styles.contains(
                "::view-transition-group(archive-logo) {\n    z-index: 4;\n}"));
    }

    @Test
    void flyingDocumentsFadeBeforeTheirFinalTravelWithoutBlurredSnapshots()
            throws Exception {
        String styles = read("resources/css/view-transitions.css");
        for (String direction : new String[] {"left", "right", "center"}) {
            String frames = block(styles, "@keyframes archive-peek-exit-" + direction);
            assertTrue(block(frames, "16%").contains("opacity: 1;"));
            assertTrue(block(frames, "60%").contains("opacity: 0.35;"));
            assertTrue(block(frames, "82%").contains("opacity: 0;"));
            assertTrue(block(frames, "100%").contains("opacity: 0;"));
            assertTrue(block(frames, "100%").contains("transform: translate("));
            assertFalse(frames.contains("filter:"));
        }
        assertTrue(styles.contains("archive-peek-exit-left 588ms linear both"));
        assertTrue(styles.contains("archive-peek-exit-right 524ms linear both"));
        assertTrue(styles.contains("archive-peek-exit-center 560ms linear 90ms both"));
    }

    @Test
    void logoMovesAsOneImageWhileKeepingTheGroupMorphAndSingleImageFallback()
            throws Exception {
        String styles = read("resources/css/view-transitions.css");
        String images = block(styles,
                "::view-transition-old(archive-logo),\n::view-transition-new(archive-logo)");
        assertTrue(images.contains("animation: none;"));
        assertTrue(images.contains("mix-blend-mode: normal;"));
        assertTrue(block(styles, "::view-transition-old(archive-logo) {")
                .contains("opacity: 0;"));
        assertTrue(block(styles,
                "::view-transition-new(archive-logo),\n::view-transition-old(archive-logo):only-child")
                .contains("opacity: 1;"));
        assertTrue(block(styles, "::view-transition-old(archive-logo):only-child")
                .contains("opacity: 1;"));
        assertTrue(styles.contains("animation-delay: 60ms;\n    animation-duration: 560ms;"));
    }

    @Test
    void loginAndHeaderKeepOneLogoTargetAndThreeDistinctDocumentTargets()
            throws Exception {
        String login = read("login.jsp");
        String header = read("WEB-INF/includes/header_nav.jspf");
        assertTrue(login.contains("class=\"login-brand-logo\""));
        assertTrue(header.contains("class=\"brand-logo\""));
        assertTrue(login.indexOf("class=\"login-brand-logo\"")
                == login.lastIndexOf("class=\"login-brand-logo\""));
        assertTrue(header.indexOf("class=\"brand-logo\"")
                == header.lastIndexOf("class=\"brand-logo\""));
        assertEquals(3, login.split("class=\"peek-doc\"", -1).length - 1);
        assertEquals(3, login.split("class=\"peek-sheet\"", -1).length - 1);
        String styles = read("resources/css/view-transitions.css");
        for (int index = 1; index <= 3; index++) {
            assertTrue(styles.contains(".peek-doc:nth-child(" + index + ") .peek-sheet"));
        }
    }

    private static String block(String source, String selector) {
        int start = source.indexOf(selector);
        assertTrue(start >= 0, () -> "Missing CSS block: " + selector);
        int opening = source.indexOf('{', start);
        int depth = 1;
        for (int index = opening + 1; index < source.length(); index++) {
            char character = source.charAt(index);
            if (character == '{') {
                depth++;
            } else if (character == '}' && --depth == 0) {
                return source.substring(opening + 1, index);
            }
        }
        throw new AssertionError("Unclosed CSS block: " + selector);
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
