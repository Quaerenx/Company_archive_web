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
        assertTrue(header.contains(
                "/resources/images/archive-logo.svg?v=${frog2AssetVersion}"));
        assertTrue(header.contains("width=\"3664\""));
        assertTrue(header.contains("height=\"1480\""));
        assertTrue(header.contains("aria-label=\"Archive 대시보드\""));
        String behavior = Files.readString(
                WEBAPP.resolve("resources/js/header_nav.js"));
        assertTrue(behavior.contains("focusReturnTarget = mobileToggle;"));
        assertFalse(behavior.contains(
                "primaryNavigation.addEventListener('keydown'"));
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
        assertTrue(header.contains(
                "|| navMaintenanceCurrent || navTroubleshootingCurrent"));
        assertTrue(header.contains(
                "value=\"${navMeetingCurrent || navFileRepositoryCurrent}\""));
        int customerMenuStart = header.indexOf("id=\"customerNavMenu\"");
        int resourceMenuStart = header.indexOf("id=\"resourceNavMenu\"");
        int troubleshootingLink = header.indexOf("/troubleshooting?view=list");
        assertTrue(customerMenuStart < troubleshootingLink);
        assertTrue(troubleshootingLink < resourceMenuStart);
        assertTrue(header.contains("/mypage"));
        assertTrue(header.contains("id=\"logoutLink\""));
        assertTrue(header.contains("id=\"quickNavStatus\""));
        assertTrue(header.contains(
                "data-search-url=\"${pageContext.request.contextPath}/search\""));
        assertTrue(header.contains("메뉴와 고객사 업무 데이터를 한 번에 검색합니다."));
        assertFalse(header.contains("href=\"#\""));
        assertFalse(header.contains("pageTitle eq"));

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
        assertTrue(behavior.contains("credentials: 'same-origin'"));
        assertTrue(behavior.contains("encodeURIComponent(query)"));
        assertTrue(behavior.contains("queryLength(query) < 2"));
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
        assertTrue(styles.contains("padding: var(--space-8) var(--space-24);"));
        // viewBox에서 여백을 덜어낸 만큼 줄인 값. 화면상 로고 크기는 이전과 같다.
        assertTrue(styles.contains("inline-size: 68px;"));
        assertTrue(styles.contains("min-block-size: var(--control-height-md);"));
        assertFalse(styles.contains("margin-inline-start: var(--space-16);"));
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
