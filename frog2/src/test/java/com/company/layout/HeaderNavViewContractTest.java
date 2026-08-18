package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class HeaderNavViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void navigationBehaviorAndVisualContractRemainStable() throws Exception {
        String header = read("WEB-INF/includes/header_nav.jspf");
        assertTrue(header.contains("class=\"main-header\""));
        assertTrue(header.contains("data-csrf-token="));
        assertTrue(header.contains("class=\"brand-logo\""));
        assertTrue(header.contains("/resources/images/archive-logo.svg"));
        assertTrue(header.contains("width=\"4096\""));
        assertTrue(header.contains("height=\"2286\""));
        assertTrue(header.contains("aria-label=\"Archive 대시보드\""));
        assertTrue(header.contains("id=\"mobileNavToggle\""));
        assertTrue(header.contains("id=\"primaryNavigation\""));
        assertTrue(header.contains("aria-label=\"주요 메뉴\""));
        assertTrue(header.contains("aria-controls=\"primaryNavigation\""));
        assertTrue(header.contains("aria-controls=\"customerNavMenu\""));
        assertTrue(header.contains("aria-controls=\"resourceNavMenu\""));
        assertTrue(header.contains("aria-expanded=\"false\""));
        assertTrue(header.contains("aria-current="));
        assertTrue(header.contains("jakarta.servlet.forward.request_uri"));
        assertTrue(header.contains("navContextRootPath"));
        assertTrue(header.contains("navVmHostsPath"));
        assertTrue(header.contains("navVmHostsPrefix"));
        assertFalse(header.contains("navVmHostsLegacy"));
        assertTrue(header.contains("navRequestUri eq navContextRootPath"));
        assertTrue(header.contains("navRequestUri eq navVmHostsPath"));
        assertTrue(header.contains("fn:startsWith(navRequestUri, navVmHostsPrefix)"));
        assertTrue(header.contains("/dashboard"));
        assertTrue(header.contains("/customers?view=list"));
        assertTrue(header.contains("/maintenance"));
        assertTrue(header.contains("/meeting?view=list"));
        assertTrue(header.contains("/file-repository"));
        assertTrue(header.contains("/troubleshooting?view=list"));
        assertTrue(header.contains("/mypage"));
        assertTrue(header.contains("id=\"logoutLink\""));
        assertFalse(header.contains("href=\"#\""));
        assertFalse(header.contains("pageTitle eq"));

        String behavior = Files.exists(
                WEBAPP.resolve("resources/js/header_nav.js"))
                ? read("resources/js/header_nav.js")
                : header;
        assertTrue(behavior.contains("Frog2Csrf"));
        assertTrue(behavior.contains("form.method = 'POST'"));
        assertTrue(behavior.contains("Frog2Csrf.appendTo(form)"));
        assertTrue(behavior.contains("matchMedia('(max-width: 768px)')"));
        assertTrue(behavior.contains("setMobileMenu"));
        assertTrue(behavior.contains("setDropdown"));
        assertTrue(behavior.contains("focusedDropdownMenu"));
        assertTrue(behavior.contains("focusCurrentDesktopNavigation"));
        assertTrue(behavior.contains("event.key !== 'Escape'"));
        assertTrue(behavior.contains("restoreFocus"));
        assertTrue(behavior.contains("aria-expanded"));
        assertTrue(behavior.contains("event.preventDefault()"));
        assertFalse(behavior.contains("window.location.pathname"));

        String styles = read("resources/css/pages/header.css");
        assertTrue(styles.contains(".main-header"));
        assertTrue(styles.contains(".main-nav"));
        assertTrue(styles.contains(".dropdown-menu"));
        assertTrue(styles.contains(".mobile-nav-toggle"));
        assertTrue(styles.contains(".mobile-nav-open"));
        assertTrue(styles.contains(":focus-visible"));
        assertTrue(styles.contains("prefers-reduced-motion"));
        assertTrue(styles.contains("@media (max-width: 768px)"));
        assertTrue(styles.contains("margin-inline-start: var(--space-16);"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
