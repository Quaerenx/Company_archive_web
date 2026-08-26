package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class VmHostViewContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void hostFormsAndVisualConstantsRemainStable() throws Exception {
        String page = read("vm_hosts/list.jsp");
        assertTrue(page.contains("name=\"action\" value=\"save\""));
        assertTrue(page.contains("name=\"action\" value=\"delete\""));
        assertTrue(page.contains("name=\"originalIp\""));
        assertTrue(page.contains("name=\"ip\""));
        assertTrue(count(page, "csrf_input.jspf") >= 2);
        assertTrue(page.contains("/resources/css/pages/vm_hosts.css"));
        assertTrue(page.contains("/resources/js/pages/vm_hosts.js"));
        assertFalse(page.contains("<style>"));
        assertFalse(page.contains("style=\""));
        assertFalse(page.contains("onsubmit="));
        assertTrue(page.contains("<c:when test=\"${not empty vmHosts}\">"));
        assertTrue(page.contains("vm-host-empty ui-empty-state"));
        assertFalse(page.contains("colspan=\"8\""));

        String behavior = page.contains("/resources/js/pages/vm_hosts.js")
                ? read("resources/js/pages/vm_hosts.js")
                : page;
        assertTrue(behavior.contains("해당 호스트를 삭제하시겠습니까?"));

        String styles = page.contains("/resources/css/pages/vm_hosts.css")
                ? read("resources/css/pages/vm_hosts.css")
                : page;
        assertTrue(styles.contains("minmax(320px, 420px) minmax(0, 1fr)"));
        assertTrue(styles.contains("background: var(--color-primary)"));
        assertTrue(styles.contains("background: var(--color-danger)"));
    }

    private static int count(String source, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static String read(String path) throws Exception {
        return Files.readString(WEBAPP.resolve(path));
    }
}
