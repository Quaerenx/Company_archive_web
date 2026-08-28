package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlashMessageUsageContractTest {
    private static final Path CONTROLLERS = Path.of(
            "src/main/java/com/company/controller");

    @Test
    void pageControllersConsumeOnlyTheirRedirectToken() throws Exception {
        for (String file : List.of(
                "CustomersServlet.java",
                "MaintenanceServlet.java",
                "MeetingServlet.java",
                "TroubleshootingServlet.java",
                "CustomerHistoryServlet.java",
                "MyPageServlet.java")) {
            assertTrue(read(file).contains("FlashMessage.expose(request);"), file);
        }
    }

    @Test
    void migratedControllersDoNotUseSharedLegacySessionSlots() throws Exception {
        for (String file : List.of(
                "CustomerCommandController.java",
                "MaintenanceServlet.java",
                "MeetingServlet.java",
                "TroubleshootingServlet.java",
                "CustomerHistoryServlet.java",
                "MyPageServlet.java")) {
            String source = read(file);
            assertFalse(source.contains("session.setAttribute(\"message\""), file);
            assertFalse(source.contains("session.setAttribute(\"error\""), file);
        }
    }

    private static String read(String file) throws Exception {
        return Files.readString(CONTROLLERS.resolve(file));
    }
}
