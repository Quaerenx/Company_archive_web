package com.company.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class OutputEncodingWebCoverageTest {
    private static final Path PROJECT = Path.of("");
    private static final Path WEBAPP = PROJECT.resolve("src/main/webapp");
    private static final Pattern INLINE_SERVER_DATA = Pattern.compile(
            "onclick\\s*=\\s*[\"'][^\"'\\r\\n]*\\$\\{",
            Pattern.CASE_INSENSITIVE);

    @Test
    void jspViewsDoNotUseRawExpressionOutput() throws Exception {
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(OutputEncodingWebCoverageTest::isJspView).toList()) {
                String source = Files.readString(path);
                assertFalse(source.contains("<%="), () -> "Raw JSP expression output: " + path);
            }
        }
    }

    @Test
    void inlineClickHandlersDoNotContainServerExpressions() throws Exception {
        try (var paths = Files.walk(WEBAPP)) {
            for (Path path : paths.filter(OutputEncodingWebCoverageTest::isJspView).toList()) {
                String source = Files.readString(path);
                assertFalse(
                        INLINE_SERVER_DATA.matcher(source).find(),
                        () -> "Server expression inside onclick attribute: " + path);
            }
        }
    }

    @Test
    void highRiskDynamicValuesUseEncodedDomData() throws Exception {
        String monthly = readWebapp("mypage/monthly_customer_response.jsp");
        assertFalse(monthly.contains("onclick=\"openEditModal("));
        assertFalse(monthly.contains("onclick=\"deleteResponse("));
        assertTrue(monthly.contains("class=\"response-customer-name\""));
        assertTrue(monthly.contains("class=\"response-action-content\" hidden"));

        String customerDetail = readWebapp("customers/customers_detail.jsp");
        assertFalse(customerDetail.contains("onclick=\"editCustomer("));
        assertFalse(customerDetail.contains("onclick=\"deleteCustomer("));
        assertTrue(customerDetail.contains("data-customer-name=\"<c:out"));

        String maintenance = readWebapp("maintenance/maintenance_history.jsp");
        assertFalse(maintenance.contains("data-customer-name="));
        assertTrue(maintenance.contains("data-usage-point"));
        assertTrue(maintenance.contains("data-date=\"<c:out"));
        assertTrue(maintenance.contains("data-value=\"<c:out"));
        assertTrue(maintenance.contains("data-pct=\"<c:out"));
        assertTrue(maintenance.contains("data-used-tb=\"<c:out"));
        assertTrue(maintenance.contains("data-size-tb=\"<c:out"));

        String maintenanceScript = readWebapp("resources/js/pages/maintenance_history.js");
        assertFalse(maintenanceScript.contains("$" + "{"));
    }

    @Test
    void publicErrorsDoNotExposeInternalDetails() throws Exception {
        String error500 = readWebapp("error/500.jsp");
        assertFalse(error500.contains("exception.getMessage"));
        assertFalse(error500.contains("<details"));

        String poolMonitor = Files.readString(PROJECT.resolve(
                "src/main/java/com/company/controller/PoolMonitorServlet.java"));
        assertFalse(poolMonitor.contains("getPoolStats()"));
        assertFalse(poolMonitor.contains("getAutoCommit()"));
        assertFalse(poolMonitor.contains("isReadOnly()"));

        String customers = Files.readString(PROJECT.resolve(
                "src/main/java/com/company/controller/CustomersServlet.java"));
        assertFalse(customers.contains("e.getMessage()"));
    }

    private static boolean isJspView(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".jsp") || name.endsWith(".jspf");
    }

    private static String readWebapp(String relativePath) throws Exception {
        return Files.readString(WEBAPP.resolve(relativePath));
    }
}
