package com.company.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.util.PasswordUtils;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e-write")
class AuthenticatedMaintenanceE2ETest {
    private static final String ENABLE_ENV = "FROG2_E2E_WRITE_ENABLED";
    private static final String BASE_URL_ENV = "FROG2_E2E_BASE_URL";
    private static final String DB_CONFIG_ENV = "FROG2_E2E_DB_CONFIG";
    private static final String SHARED_DB_CONFIG_ENV =
            "FROG2_E2E_SHARED_DB_CONFIG";
    private static final Pattern CSRF_INPUT = Pattern.compile(
            "name=\\\"_csrf\\\"\\s+value=\\\"([^\\\"]+)\\\"");
    private static final List<String> LOAD_ROUTES = List.of(
            "dashboard", "customers", "maintenance", "meeting",
            "troubleshooting", "mypage", "vm-hosts", "file-repository");
    private static final int LOAD_CONCURRENCY = 6;
    private static final int LOAD_REQUESTS = 120;

    @Test
    void ownerBoundCrudAndBoundedReadLoad() throws Exception {
        requireExplicitEnablement();
        URI baseUri = configuredBaseUri();
        Path configPath = configuredDevelopmentDatabase();
        Path sharedConfigPath = configuredDatabase(SHARED_DB_CONFIG_ENV);
        Properties databaseProperties = IsolatedDatabaseConfig.load(
                configPath, sharedConfigPath);

        String suffix = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8);
        String password = "E2E-" + suffix + "-Aa9!";
        Credentials owner = new Credentials(
                "e2eo" + suffix, password, "E2E Owner");
        Credentials attacker = new Credentials(
                "e2ea" + suffix, password, "E2E Attacker");
        String customerName = "E2E-" + suffix;
        String createdNote = "E2E-AUTH-" + suffix + "-created";
        String attackerNote = "E2E-AUTH-" + suffix + "-attacker";
        String ownerNote = "E2E-AUTH-" + suffix + "-owner";

        try (Connection database = openDatabase(databaseProperties)) {
            database.setReadOnly(false);
            try {
                insertUser(database, owner);
                insertUser(database, attacker);

                SessionClient ownerClient = SessionClient.create(baseUri);
                ownerClient.login(owner);
                String ownerCsrf = ownerClient.csrfFromGet(
                        "maintenance?view=add&customerName="
                                + encode(customerName));

                HttpResponse<String> createResponse = ownerClient.postForm(
                        "maintenance",
                        maintenanceForm(
                                "add", null, customerName, owner.userName(),
                                createdNote, ownerCsrf));
                assertEquals(302, createResponse.statusCode());

                DatabaseRecord created = findMaintenanceRecord(
                        database, customerName, owner.userId());
                assertNotNull(created, "Owner create did not persist a record");
                assertEquals(createdNote, created.note());

                HttpResponse<String> ownerEdit = ownerClient.get(
                        "maintenance?view=edit&id=" + created.id());
                assertEquals(200, ownerEdit.statusCode());

                SessionClient attackerClient = SessionClient.create(baseUri);
                attackerClient.login(attacker);
                String attackerCsrf = attackerClient.csrfFromGet(
                        "maintenance?view=add&customerName="
                                + encode(customerName));

                HttpResponse<String> attackerEdit = attackerClient.get(
                        "maintenance?view=edit&id=" + created.id());
                assertEquals(302, attackerEdit.statusCode());

                HttpResponse<String> attackerUpdate = attackerClient.postForm(
                        "maintenance",
                        maintenanceForm(
                                "update", created.id(), customerName,
                                attacker.userName(), attackerNote, attackerCsrf));
                assertEquals(302, attackerUpdate.statusCode());
                assertEquals(
                        createdNote,
                        requiredMaintenanceRecord(
                                database, customerName, owner.userId()).note());

                HttpResponse<String> attackerDelete = attackerClient.postForm(
                        "maintenance",
                        form(
                                "action", "delete",
                                "maintenance_id", Long.toString(created.id()),
                                "customer_name", customerName,
                                "_csrf", attackerCsrf));
                assertEquals(302, attackerDelete.statusCode());
                assertNotNull(findMaintenanceRecord(
                        database, customerName, owner.userId()));

                HttpResponse<String> ownerUpdate = ownerClient.postForm(
                        "maintenance",
                        maintenanceForm(
                                "update", created.id(), customerName,
                                owner.userName(), ownerNote, ownerCsrf));
                assertEquals(302, ownerUpdate.statusCode());
                assertEquals(
                        ownerNote,
                        requiredMaintenanceRecord(
                                database, customerName, owner.userId()).note());

                HttpResponse<String> ownerDelete = ownerClient.postForm(
                        "maintenance",
                        form(
                                "action", "delete",
                                "maintenance_id", Long.toString(created.id()),
                                "customer_name", customerName,
                                "_csrf", ownerCsrf));
                assertEquals(302, ownerDelete.statusCode());
                assertFalse(maintenanceRecordExists(
                        database, customerName, owner.userId()));

                runBoundedReadLoad(ownerClient);
            } finally {
                cleanupTemporaryData(
                        database, customerName,
                        owner.userId(), attacker.userId());
            }
        }
    }

    private static void requireExplicitEnablement() {
        assertEquals(
                "true",
                System.getenv(ENABLE_ENV),
                ENABLE_ENV + "=true is required for write E2E");
    }

    private static URI configuredBaseUri() {
        String configured = requiredEnvironment(BASE_URL_ENV);
        if (!configured.endsWith("/")) {
            configured += "/";
        }
        URI uri = URI.create(configured);
        String host = uri.getHost();
        boolean loopback = "127.0.0.1".equals(host)
                || "localhost".equalsIgnoreCase(host)
                || "::1".equals(host);
        if (!loopback || !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Write E2E requires a loopback HTTP target");
        }
        return uri;
    }

    private static Path configuredDevelopmentDatabase() throws Exception {
        return configuredDatabase(DB_CONFIG_ENV);
    }

    private static Path configuredDatabase(String environmentName)
            throws Exception {
        Path path = Path.of(requiredEnvironment(environmentName))
                .toRealPath()
                .normalize();
        Path allowedRoot = Path.of("/opt/frog2-dev").toRealPath();
        if (!path.startsWith(allowedRoot) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException(
                    "Write E2E requires a database config under /opt/frog2-dev");
        }
        return path;
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.trim();
    }

    private static Connection openDatabase(Properties properties)
            throws Exception {
        Class.forName(properties.getProperty("db.driver"));
        return DriverManager.getConnection(
                properties.getProperty("db.url"),
                properties.getProperty("db.user"),
                properties.getProperty("db.password"));
    }

    private static void insertUser(
            Connection connection, Credentials credentials) throws Exception {
        String sql = "INSERT INTO company_users "
                + "(userId, password, userName) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, credentials.userId());
            statement.setString(
                    2, PasswordUtils.hashPassword(credentials.password()));
            statement.setString(3, credentials.userName());
            assertEquals(1, statement.executeUpdate());
        }
    }

    private static DatabaseRecord findMaintenanceRecord(
            Connection connection,
            String customerName,
            String creatorUserId) throws Exception {
        String sql = "SELECT maintenance_id, note "
                + "FROM maintenance_records "
                + "WHERE customer_name = ? AND created_by_user_id = ? "
                + "ORDER BY maintenance_id DESC LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, customerName);
            statement.setString(2, creatorUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return null;
                }
                return new DatabaseRecord(
                        resultSet.getLong("maintenance_id"),
                        resultSet.getString("note"));
            }
        }
    }

    private static DatabaseRecord requiredMaintenanceRecord(
            Connection connection,
            String customerName,
            String creatorUserId) throws Exception {
        DatabaseRecord record = findMaintenanceRecord(
                connection, customerName, creatorUserId);
        assertNotNull(record, "Expected temporary maintenance record");
        return record;
    }

    private static boolean maintenanceRecordExists(
            Connection connection,
            String customerName,
            String creatorUserId) throws Exception {
        return findMaintenanceRecord(
                connection, customerName, creatorUserId) != null;
    }

    private static void cleanupTemporaryData(
            Connection connection,
            String customerName,
            String ownerUserId,
            String attackerUserId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM maintenance_records "
                        + "WHERE customer_name = ? "
                        + "AND created_by_user_id IN (?, ?)")) {
            statement.setString(1, customerName);
            statement.setString(2, ownerUserId);
            statement.setString(3, attackerUserId);
            statement.executeUpdate();
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM company_users WHERE userId IN (?, ?)")) {
            statement.setString(1, ownerUserId);
            statement.setString(2, attackerUserId);
            statement.executeUpdate();
        }
    }

    private static Map<String, String> maintenanceForm(
            String action,
            Long maintenanceId,
            String customerName,
            String inspectorName,
            String note,
            String csrf) {
        Map<String, String> fields = form(
                "action", action,
                "customer_name", customerName,
                "inspector_name", inspectorName,
                "inspection_date", "2026-08-03",
                "vertica_version", "E2E",
                "note", note,
                "license_size_gb", "1TB",
                "license_usage_size", "0.5TB",
                "license_usage_pct", "50%",
                "_csrf", csrf);
        if (maintenanceId != null) {
            fields.put("maintenance_id", Long.toString(maintenanceId));
        }
        return fields;
    }

    private static Map<String, String> form(String... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("Form entries must be pairs");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            fields.put(entries[index], entries[index + 1]);
        }
        return fields;
    }

    private static void runBoundedReadLoad(SessionClient client)
            throws Exception {
        for (String route : LOAD_ROUTES) {
            HttpResponse<Void> response = client.getWithoutBody(route);
            assertEquals(200, response.statusCode(), "Warm-up failed: /" + route);
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                LOAD_CONCURRENCY);
        List<Future<LoadSample>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < LOAD_REQUESTS; index++) {
                String route = LOAD_ROUTES.get(index % LOAD_ROUTES.size());
                futures.add(executor.submit(() -> timedGet(client, route)));
            }

            List<LoadSample> samples = new ArrayList<>();
            for (Future<LoadSample> future : futures) {
                samples.add(future.get(45, TimeUnit.SECONDS));
            }
            reportLoad(samples);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static LoadSample timedGet(
            SessionClient client, String route) throws Exception {
        long started = System.nanoTime();
        HttpResponse<Void> response = client.getWithoutBody(route);
        long duration = System.nanoTime() - started;
        assertEquals(200, response.statusCode(), "Load read failed: /" + route);
        return new LoadSample(route, duration);
    }

    private static void reportLoad(List<LoadSample> samples) {
        List<Long> allDurations = new ArrayList<>();
        for (LoadSample sample : samples) {
            allDurations.add(sample.durationNanos());
        }
        System.out.printf(
                Locale.ROOT,
                "LOAD_PROBE|requests=%d|concurrency=%d|success=%d|"
                        + "clientP50Ms=%.2f|clientP95Ms=%.2f|clientMaxMs=%.2f%n",
                LOAD_REQUESTS,
                LOAD_CONCURRENCY,
                samples.size(),
                percentileMillis(allDurations, 0.50),
                percentileMillis(allDurations, 0.95),
                maxMillis(allDurations));

        for (String route : LOAD_ROUTES) {
            List<Long> routeDurations = new ArrayList<>();
            for (LoadSample sample : samples) {
                if (route.equals(sample.route())) {
                    routeDurations.add(sample.durationNanos());
                }
            }
            System.out.printf(
                    Locale.ROOT,
                    "LOAD_ROUTE|path=/%s|requests=%d|clientP50Ms=%.2f|"
                            + "clientP95Ms=%.2f|clientMaxMs=%.2f%n",
                    route,
                    routeDurations.size(),
                    percentileMillis(routeDurations, 0.50),
                    percentileMillis(routeDurations, 0.95),
                    maxMillis(routeDurations));
        }
    }

    private static double percentileMillis(
            List<Long> durations, double percentile) {
        List<Long> sorted = new ArrayList<>(durations);
        sorted.sort(Long::compareTo);
        int index = Math.max(
                0,
                (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index) / 1_000_000.0;
    }

    private static double maxMillis(List<Long> durations) {
        long maximum = 0;
        for (long duration : durations) {
            maximum = Math.max(maximum, duration);
        }
        return maximum / 1_000_000.0;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record Credentials(
            String userId, String password, String userName) {
    }

    private record DatabaseRecord(long id, String note) {
    }

    private record LoadSample(String route, long durationNanos) {
    }

    private static final class SessionClient {
        private final URI baseUri;
        private final HttpClient client;

        private SessionClient(URI baseUri, HttpClient client) {
            this.baseUri = baseUri;
            this.client = client;
        }

        private static SessionClient create(URI baseUri) {
            CookieManager cookies = new CookieManager(
                    null, CookiePolicy.ACCEPT_ALL);
            HttpClient client = HttpClient.newBuilder()
                    .cookieHandler(cookies)
                    .connectTimeout(Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
            return new SessionClient(baseUri, client);
        }

        private void login(Credentials credentials) throws Exception {
            String csrf = csrfFromGet("login");
            HttpResponse<String> response = postForm(
                    "login",
                    form(
                            "userId", credentials.userId(),
                            "password", credentials.password(),
                            "_csrf", csrf));
            assertEquals(302, response.statusCode());
            URI redirect = baseUri.resolve(requiredHeader(
                    response, "Location"));
            assertEquals(baseUri.resolve("dashboard").getPath(), redirect.getPath());
        }

        private String csrfFromGet(String relativePath) throws Exception {
            HttpResponse<String> response = get(relativePath);
            assertEquals(200, response.statusCode());
            Matcher matcher = CSRF_INPUT.matcher(response.body());
            assertTrue(matcher.find(), "Missing CSRF input on /" + relativePath);
            return matcher.group(1);
        }

        private HttpResponse<String> get(String relativePath)
                throws Exception {
            HttpRequest request = HttpRequest.newBuilder(
                            baseUri.resolve(relativePath))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "text/html")
                    .GET()
                    .build();
            return client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        private HttpResponse<Void> getWithoutBody(String relativePath)
                throws Exception {
            HttpRequest request = HttpRequest.newBuilder(
                            baseUri.resolve(relativePath))
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "text/html")
                    .GET()
                    .build();
            return client.send(
                    request, HttpResponse.BodyHandlers.discarding());
        }

        private HttpResponse<String> postForm(
                String relativePath, Map<String, String> fields)
                throws Exception {
            StringBuilder body = new StringBuilder();
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (!body.isEmpty()) {
                    body.append('&');
                }
                body.append(encode(entry.getKey()))
                        .append('=')
                        .append(encode(entry.getValue()));
            }
            HttpRequest request = HttpRequest.newBuilder(
                            baseUri.resolve(relativePath))
                    .timeout(Duration.ofSeconds(30))
                    .header(
                            "Content-Type",
                            "application/x-www-form-urlencoded")
                    .header("Accept", "text/html")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            body.toString(), StandardCharsets.UTF_8))
                    .build();
            return client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }

        private static String requiredHeader(
                HttpResponse<?> response, String name) {
            return response.headers().firstValue(name)
                    .orElseThrow(() -> new AssertionError(
                            "Missing response header: " + name));
        }
    }
}
