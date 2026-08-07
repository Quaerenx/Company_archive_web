package com.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RuntimePerformanceContractTest {
    private static final Path JAVA = Path.of("src/main/java");
    private static final Path WEBAPP = Path.of("src/main/webapp");
    private static final Pattern JAVA_STRING =
            Pattern.compile("\"((?:\\\\.|[^\"\\\\])*)\"");

    @Test
    void meetingListQuerySelectsOnlyFieldsRenderedByTheList() throws Exception {
        String source = readJava("com/company/model/MeetingRecordDAO.java");
        Matcher declaration = Pattern.compile(
                        "static\\s+final\\s+String\\s+LIST_SQL\\s*=\\s*(.*?);",
                        Pattern.DOTALL)
                .matcher(source);
        assertTrue(declaration.find(), "MeetingRecordDAO.LIST_SQL is missing");

        String sql = javaStringValue(declaration.group(1))
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        int selectStart = sql.indexOf("select ");
        int fromStart = sql.indexOf(" from ");
        assertTrue(selectStart >= 0 && fromStart > selectStart, sql);

        Set<String> columns = Arrays.stream(
                        sql.substring(selectStart + "select ".length(), fromStart).split(","))
                .map(String::trim)
                .map(column -> column.replaceFirst("^[a-z][a-z0-9_]*\\.", ""))
                .collect(Collectors.toSet());

        assertEquals(
                Set.of("meeting_id", "title", "author_name", "meeting_datetime"),
                columns);
        assertFalse(sql.contains("meeting_comments"), sql);
        assertFalse(sql.contains("content"), sql);
    }

    @Test
    void maintenanceCardsFilterCustomersInTheDatabaseQuery() throws Exception {
        String source = readJava("com/company/controller/MaintenanceServlet.java");
        String method = between(
                source,
                "private Map<String, List<CustomerDTO>> getInspectorCustomersMap()",
                "@Override");

        assertTrue(
                Pattern.compile(
                                "getAllCustomers\\(\\s*\"manager_name\"\\s*,"
                                        + "\\s*\"ASC\"\\s*,\\s*\"maintenance\"\\s*\\)")
                        .matcher(method)
                        .find(),
                "maintenance cards must request only maintenance customers");
    }

    @Test
    void connectionPoolDiagnosticsAreLazyAtDebugLevel() throws Exception {
        String source = readJava("com/company/util/DBConnection.java");
        String method = between(
                source,
                "public static Connection getConnection()",
                "public static void close");

        int guard = method.indexOf("logger.isDebugEnabled()");
        int poolSnapshot = method.indexOf("getHikariPoolMXBean()");
        int debugLog = method.indexOf("logger.debug(");
        assertTrue(guard >= 0, "debug guard is missing");
        assertTrue(poolSnapshot > guard, "pool MXBean must be read inside the debug guard");
        assertTrue(debugLog > guard, "diagnostic logging must remain inside the debug guard");
    }

    @Test
    void coreAndStandaloneErrorStylesLoadFoundationsDirectly() throws Exception {
        String core = Files.readString(WEBAPP.resolve("WEB-INF/includes/core_styles.jspf"));
        assertFoundationOrder(core, "core_styles.jspf");
        assertEquals(1, countOccurrences(core, "/resources/css/components.css"));
        assertEquals(1, countOccurrences(core, "/resources/css/ui-system.css"));
        assertEquals(1, countOccurrences(core, "/resources/css/utilities.css"));
        assertTrue(core.indexOf("/resources/css/base.css")
                < core.indexOf("/resources/css/components.css"));
        assertTrue(core.indexOf("/resources/css/components.css")
                < core.indexOf("/resources/css/ui-system.css"));
        assertTrue(core.indexOf("/resources/css/ui-system.css")
                < core.indexOf("/resources/css/utilities.css"));

        for (String page : new String[] {"error/409.jsp", "error/503.jsp"}) {
            assertFoundationOrder(Files.readString(WEBAPP.resolve(page)), page);
        }
    }

    private static void assertFoundationOrder(String source, String label) {
        String tokens = "/resources/css/tokens.css";
        String base = "/resources/css/base.css";
        assertEquals(1, countOccurrences(source, tokens), label);
        assertEquals(1, countOccurrences(source, base), label);
        assertTrue(source.indexOf(tokens) < source.indexOf(base), label);
        assertFalse(source.contains("/resources/css/main_style.css"), label);
    }

    private static String readJava(String path) throws Exception {
        return Files.readString(JAVA.resolve(path));
    }

    private static String between(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        assertTrue(startIndex >= 0, () -> "method start is missing: " + start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertTrue(endIndex > startIndex, () -> "method end is missing: " + end);
        return source.substring(startIndex, endIndex);
    }

    private static String javaStringValue(String declaration) {
        Matcher literal = JAVA_STRING.matcher(declaration);
        StringBuilder value = new StringBuilder();
        while (literal.find()) {
            value.append(literal.group(1));
        }
        assertTrue(value.length() > 0, "SQL declaration contains no string literals");
        return value.toString();
    }

    private static int countOccurrences(String source, String target) {
        int count = 0;
        int offset = 0;
        while ((offset = source.indexOf(target, offset)) >= 0) {
            count++;
            offset += target.length();
        }
        return count;
    }
}
