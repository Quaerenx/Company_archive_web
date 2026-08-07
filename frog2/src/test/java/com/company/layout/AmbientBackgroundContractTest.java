package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AmbientBackgroundContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void authenticatedShellOwnsOneDecorativeAmbientCanvas() throws Exception {
        String header = read("includes/header.jsp");
        String footer = read("includes/footer.jsp");
        String login = read("login.jsp");

        assertTrue(header.contains("has-ambient-background"));
        assertTrue(header.contains("data-app-ambient-background"));
        assertTrue(header.contains("aria-hidden=\"true\""));
        assertTrue(footer.contains("/resources/js/ambient-background.js?v=${frog2AssetVersion}"));
        assertFalse(login.contains("data-app-ambient-background"));
        assertFalse(login.contains("ambient-background.js"));
    }

    @Test
    void ambienceContinuesBehindTheCentralContentRail() throws Exception {
        String styles = read("resources/css/ambient-background.css");
        String script = read("resources/js/ambient-background.js");

        assertTrue(styles.contains("var(--color-ambient-background)"));
        assertTrue(styles.contains("background-image: none;"));
        assertTrue(styles.contains("border-block-start: 0;"));
        assertTrue(styles.contains("color: var(--color-text-inverse);"));
        assertTrue(styles.contains("pointer-events: none;"));
        assertTrue(styles.contains("@media (max-width: 1050px)"));
        assertTrue(script.contains("var DEFAULT_PARTICLE_COUNT = 72;"));
        assertTrue(script.contains("var LOW_POWER_PARTICLE_COUNT = 60;"));
        assertTrue(script.contains("var TARGET_FRAME_RATE = 30;"));
        assertTrue(script.contains("var MAX_DEVICE_PIXEL_RATIO = 1.5;"));
        assertTrue(script.contains("var SPEED = 0.45;"));
        assertTrue(script.contains("var BRIGHTNESS = 16;"));
        assertTrue(script.contains("(min-width: 1051px)"));
        assertFalse(styles.contains("typography-dashboard-page"));
        assertFalse(script.contains("typography-dashboard-page"));
        assertTrue(script.contains("document.documentElement.clientWidth"));
        assertFalse(script.contains("function beginGutterClip()"));
        assertFalse(script.contains("context.clip();"));
    }

    @Test
    void animationHonorsMotionVisibilityAndResizePreferences() throws Exception {
        String script = read("resources/js/ambient-background.js");

        assertTrue(script.contains("prefers-reduced-motion: reduce"));
        assertTrue(script.contains("visibilitychange"));
        assertTrue(script.contains("ResizeObserver"));
        assertTrue(script.contains("window.cancelAnimationFrame"));
        assertTrue(script.contains("navigator.hardwareConcurrency"));
        assertTrue(script.contains("navigator.deviceMemory"));
        assertTrue(script.contains("deltaMilliseconds >= FRAME_INTERVAL"));
        assertTrue(script.contains("deltaMilliseconds % FRAME_INTERVAL"));
        assertTrue(script.contains("pagehide"));
        assertTrue(script.contains("pageshow"));
        assertTrue(script.contains("MAX_DEVICE_PIXEL_RATIO"));
        assertFalse(script.contains("warmup < 120"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
