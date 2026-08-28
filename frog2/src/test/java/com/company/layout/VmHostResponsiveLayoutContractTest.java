package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VmHostResponsiveLayoutContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void standaloneHostRowsExposeEveryColumnAsMobileLabelValuePairs()
            throws Exception {
        String page = read("vm_hosts/list.jsp");

        assertDataLabels(page, List.of(
                "IP", "목적", "OS", "VERTICA-ver", "원격지", "비고",
                "수정일", "작업"));
        assertMobileCardRules(read("resources/css/pages/vm_hosts.css"),
                ".vm-host-page");
    }

    @Test
    void myPageHostRowsExposeEveryColumnAsMobileLabelValuePairs()
            throws Exception {
        String page = read("WEB-INF/includes/mypage/host_manager.jspf");

        assertDataLabels(page, List.of(
                "사용 호스트", "목적", "OS", "VERTICA-ver", "원격지", "비고",
                "관리"));
        assertMobileCardRules(read("resources/css/pages/mypage_hosts.css"),
                ".page-mypage");
    }

    private static void assertDataLabels(String source, List<String> labels) {
        for (String label : labels) {
            assertTrue(source.contains("data-label=\"" + label + "\""), label);
        }
    }

    private static void assertMobileCardRules(String css, String scope) {
        int mobileStart = css.indexOf("@media (max-width: 768px)");
        assertTrue(mobileStart >= 0);
        String mobile = css.substring(mobileStart);

        assertTrue(mobile.contains(scope + " .vm-table-wrap"));
        assertTrue(mobile.contains("overflow-x: visible;"));
        assertTrue(mobile.contains(scope + " .vm-table td::before"));
        assertTrue(mobile.contains("content: attr(data-label);"));
        assertTrue(mobile.contains("min-width: 0;"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
