package com.company.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AsyncSessionExpiryContractTest {
    private static final Path WEBAPP = Path.of("src/main/webapp");

    @Test
    void sharedSessionHandlerLoadsBeforePageScripts() throws Exception {
        String footer = read("includes/footer.jsp");
        int uiSystem = footer.indexOf("/resources/js/ui-system.js");
        int sessionExpiry = footer.indexOf("/resources/js/session-expiry.js");
        int pageScripts = footer.indexOf("not empty pageScript");

        assertTrue(uiSystem >= 0);
        assertTrue(sessionExpiry > uiSystem);
        assertTrue(pageScripts > sessionExpiry);
        assertTrue(footer.contains("data-context-path="));
    }

    @Test
    void asynchronousFeaturesDistinguishSessionExpiryFromOperationFailure()
            throws Exception {
        for (String path : new String[] {
                "resources/js/pages/maintenance_form.js",
                "resources/js/pages/meeting_view.js",
                "resources/js/pages/file_repository_upload.js"}) {
            String script = read(path);
            assertTrue(script.contains("Frog2Session.requireActiveSession"), path);
            assertTrue(script.contains("Frog2Session.isSessionExpired"), path);
        }
    }

    private static String read(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
