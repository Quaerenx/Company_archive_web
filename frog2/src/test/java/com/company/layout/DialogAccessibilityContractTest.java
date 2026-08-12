package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class DialogAccessibilityContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern LABELLED_BY = Pattern.compile(
            "aria-labelledby=\"([^\"]+)\"");

    @Test
    void everyDialogIsNamedFocusableAndHasAnInitialFocusTarget()
            throws Exception {
        Map<String, String> dialogs = new LinkedHashMap<>();
        dialogs.put("WEB-INF/includes/header_nav.jspf", "quickNavDialog");
        dialogs.put("meeting/meeting_write.jsp", "previewModal");
        dialogs.put("meeting/meeting_edit.jsp", "previewModal");
        dialogs.put("mypage/monthly_customer_response.jsp", "responseModal");
        dialogs.put(
                "WEB-INF/includes/mypage/host_manager.jspf",
                "vmHostModal");

        for (Map.Entry<String, String> entry : dialogs.entrySet()) {
            String page = read(entry.getKey());
            assertEquals(1, occurrences(page, "role=\"dialog\""), entry.getKey());
            assertTrue(page.contains("id=\"" + entry.getValue() + "\""),
                    entry.getKey());
            assertTrue(page.contains("aria-modal=\"true\""), entry.getKey());
            assertTrue(page.contains("aria-hidden=\"true\""), entry.getKey());
            assertTrue(page.contains("tabindex=\"-1\""), entry.getKey());
            assertTrue(page.contains("data-dialog-initial-focus"), entry.getKey());

            Matcher label = LABELLED_BY.matcher(page);
            assertTrue(label.find(), entry.getKey());
            assertTrue(page.contains("id=\"" + label.group(1) + "\""),
                    entry.getKey() + ": " + label.group(1));
        }
    }

    @Test
    void commonDialogControllerOwnsFocusTrapEscapeAndFocusReturn()
            throws Exception {
        String controller = read("resources/js/ui-system.js");

        assertTrue(controller.contains("function focusableElements(dialog)"));
        assertTrue(controller.contains("if (event.key === 'Escape')"));
        assertTrue(controller.contains("if (event.key !== 'Tab')"));
        assertTrue(controller.contains("opener = trigger"));
        assertTrue(controller.contains("focusTarget.isConnected"));
        assertTrue(controller.contains("focusTarget.focus()"));
        assertTrue(controller.contains("document.addEventListener('keydown', handleKeydown)"));
        assertTrue(controller.contains("document.removeEventListener('keydown', handleKeydown)"));

        assertTrue(read("resources/js/header_nav.js")
                .contains("Frog2UI.createDialogController(quickNavDialog)"));
        assertTrue(read("resources/js/pages/meeting_form.js")
                .contains("Frog2UI.createDialogController(modal)"));
        assertTrue(read("resources/js/pages/monthly_customer_response.js")
                .contains("Frog2UI.createDialogController(modal)"));
        assertTrue(read("resources/js/pages/mypage_hosts.js")
                .contains("Frog2UI.createDialogController(modal)"));
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }

    private static int occurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
