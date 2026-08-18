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
        assertTrue(login.contains("has-ambient-background"));
        assertTrue(login.contains("data-app-ambient-background"));
        assertTrue(login.contains("ambient-background.js?v=${initParam.frog2AssetVersion}"));
    }

    @Test
    void ambienceContinuesBehindFloatingWorkSurfaces() throws Exception {
        String tokens = read("resources/css/tokens.css");
        String styles = read("resources/css/ambient-background.css");
        String script = read("resources/js/ambient-background.js");

        assertTrue(tokens.contains("--palette-ambient: #E8EDF2;"));
        assertTrue(tokens.contains("--palette-surface: #FAFBFC;"));
        assertTrue(tokens.contains("--palette-border: #D5DCE3;"));
        assertTrue(tokens.contains(
                "--color-navigation-surface: rgba(252, 252, 253, 0.94);"));
        assertTrue(tokens.contains(
                "--color-ambient-glow-light: rgba(255, 255, 255, 0.45);"));
        assertTrue(tokens.contains(
                "--color-ambient-glow-ink: rgba(52, 74, 96, 0.06);"));
        assertTrue(tokens.contains(
                "--color-ambient-particle: rgba(69, 95, 122, 0.14);"));
        assertTrue(styles.contains("var(--color-ambient-background)"));
        assertTrue(styles.contains("var(--color-ambient-glow-light)"));
        assertTrue(styles.contains("var(--color-ambient-glow-ink)"));
        assertTrue(styles.contains("circle at 50% 18%"));
        assertTrue(styles.contains("circle at 78% 72%"));
        assertTrue(styles.contains("background: transparent;"));
        assertTrue(styles.contains("border-radius: 0;"));
        assertTrue(styles.contains("box-shadow: none;"));
        assertTrue(styles.contains("max-width: none;"));
        assertTrue(styles.contains("width: 100%;"));
        assertTrue(styles.contains("max-width: var(--page-content-max-width);"));
        assertTrue(styles.contains(
                "width: calc(100% - var(--page-content-total-gutter));"));
        assertTrue(styles.contains("> .app-main > .content-shell"));
        assertTrue(styles.contains("padding-inline: 0;"));
        assertFalse(styles.contains(
                "var(--page-content-max-width) + var(--page-content-total-gutter)"));
        assertTrue(styles.contains("border-block-start: 0;"));
        assertTrue(styles.contains("color: var(--color-text-muted);"));
        assertTrue(styles.contains("pointer-events: none;"));
        assertTrue(styles.contains("@media (max-width: 1050px)"));
        assertTrue(script.contains("var DEFAULT_PARTICLE_COUNT = 36;"));
        assertTrue(script.contains("var LOW_POWER_PARTICLE_COUNT = 24;"));
        assertTrue(script.contains("var TARGET_FRAME_RATE = 30;"));
        assertTrue(script.contains("var MAX_DEVICE_PIXEL_RATIO = 1.5;"));
        assertTrue(script.contains("var SPEED = 0.18;"));
        assertTrue(script.contains("var BRIGHTNESS = 9;"));
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
