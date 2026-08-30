package com.company.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GlobalSearchWebContractTest {
    @Test
    void authenticatedSearchEndpointIsMappedAsAGetOnlyServlet()
            throws Exception {
        String webXml = Files.readString(
                Path.of("src/main/webapp/WEB-INF/web.xml"));
        String source = Files.readString(Path.of(
                "src/main/java/com/company/controller/GlobalSearchServlet.java"));

        assertTrue(webXml.contains(
                "<servlet-class>com.company.controller.GlobalSearchServlet</servlet-class>"));
        assertTrue(webXml.contains("<url-pattern>/search</url-pattern>"));
        assertTrue(source.contains("protected void doGet("));
        assertTrue(source.contains("SessionPrincipal.expose(request)"));
        assertTrue(source.contains("response.setHeader(\"Cache-Control\", \"no-store\")"));
    }
}
