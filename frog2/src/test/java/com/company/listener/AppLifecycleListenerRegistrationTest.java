package com.company.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AppLifecycleListenerRegistrationTest {
    private static final Path LISTENER_SOURCE = Path.of(
            "src/main/java/com/company/listener/AppLifecycleListener.java");
    private static final Path WEB_XML = Path.of("src/main/webapp/WEB-INF/web.xml");
    private static final Path POOL_MONITOR_SOURCE = Path.of(
            "src/main/java/com/company/controller/PoolMonitorServlet.java");
    private static final String LISTENER_CLASS =
            "com.company.listener.AppLifecycleListener";

    @Test
    void lifecycleListenerUsesOnlyTheWebXmlRegistration() throws Exception {
        String source = Files.readString(LISTENER_SOURCE);
        String webXml = Files.readString(WEB_XML);

        assertFalse(source.contains("jakarta.servlet.annotation.WebListener"));
        assertFalse(source.contains("@WebListener"));
        assertEquals(1, occurrences(webXml, "<listener-class>" + LISTENER_CLASS
                + "</listener-class>"));
    }

    @Test
    void poolMonitorUsesOnlyTheWebXmlRegistration() throws Exception {
        String source = Files.readString(POOL_MONITOR_SOURCE);
        String webXml = Files.readString(WEB_XML);

        assertFalse(source.contains("jakarta.servlet.annotation.WebServlet"));
        assertFalse(source.contains("@WebServlet"));
        assertEquals(1, occurrences(webXml,
                "<servlet-class>com.company.controller.PoolMonitorServlet</servlet-class>"));
        assertEquals(1, occurrences(webXml, "<url-pattern>/admin/pool-status</url-pattern>"));
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
