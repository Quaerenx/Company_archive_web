package com.company.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@Tag("e2e")
@TestMethodOrder(OrderAnnotation.class)
class DevelopmentServerSmokeTest {
    private static final Pattern CSRF_INPUT = Pattern.compile(
            "name=\\\"_csrf\\\"\\s+value=\\\"([^\\\"]+)\\\"");
    private static final Pattern CUSTOMER_DETAIL_LINK = Pattern.compile(
            "<a class=\\\"customer-detail-link\\\"\\s+href=\\\"([^\\\"]+)\\\"");
    private static final List<String> PROTECTED_ROUTES = List.of(
            "dashboard",
            "customers",
            "maintenance",
            "meeting",
            "troubleshooting",
            "customer-history",
            "mypage",
            "vm-hosts",
            "file-repository");
    private static final List<String> TABLE_ROUTES = List.of(
            "customers",
            "meeting",
            "troubleshooting",
            "customer-history",
            "vm-hosts",
            "file-repository");

    private static URI baseUri;
    private static HttpClient client;

    @BeforeAll
    static void configureClient() {
        String configuredBaseUrl = System.getProperty(
                "frog2.e2e.baseUrl",
                "http://127.0.0.1:18081/frog2/");
        if (!configuredBaseUrl.endsWith("/")) {
            configuredBaseUrl += "/";
        }
        baseUri = URI.create(configuredBaseUrl);
        requireLoopbackUnlessExplicitlyAllowed(baseUri);

        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Test
    @Order(1)
    void loginPageAndStaticAssetsAreHealthy() throws Exception {
        HttpResponse<String> login = get("login", "text/html");

        assertEquals(200, login.statusCode());
        assertTrue(header(login, "Content-Type").startsWith("text/html"));
        assertTrue(login.body().contains("id=\"loginForm\""));
        assertTrue(login.body().contains("autocomplete=\"off\""));
        assertFalse(login.body().contains("autocomplete=\"on\""));
        assertFalse(login.body().contains("autocomplete=\"username\""));
        assertTrue(CSRF_INPUT.matcher(login.body()).find());
        assertEquals("nosniff", header(login, "X-Content-Type-Options"));
        assertTrue(header(login, "Cache-Control").contains("no-store"));

        HttpResponse<String> css = get("resources/css/ui-system.css", "text/css");

        assertEquals(200, css.statusCode());
        assertTrue(header(css, "Content-Type").startsWith("text/css"));
        assertTrue(header(css, "Cache-Control").contains("public"));
        assertFalse(css.body().isBlank());

        assertHealthyAsset("resources/js/ui-system.js", "javascript");
        assertHealthyAsset("favicon.svg", "image/svg+xml");
        assertHealthyAsset(
                "resources/images/archive-compact-horizontal.svg",
                "image/svg+xml");
    }

    @Test
    @Order(2)
    void anonymousRequestsCannotReachProtectedPagesOrJsonEndpoints() throws Exception {
        for (String route : PROTECTED_ROUTES) {
            HttpResponse<String> response = get(route, "text/html");
            assertEquals(302, response.statusCode(), "Expected redirect for /" + route);
            URI redirect = baseUri.resolve(header(response, "Location"));
            assertEquals(baseUri.resolve("login").getPath(), redirect.getPath());
        }

        HttpResponse<String> jsonResponse = get("comment", "application/json");
        assertEquals(401, jsonResponse.statusCode());
        assertTrue(header(jsonResponse, "Content-Type").startsWith("application/json"));
        assertTrue(jsonResponse.body().contains("authentication_required"));
    }

    @Test
    @Order(3)
    void authenticatedUserCanOpenCoreReadOnlyPages() throws Exception {
        Assumptions.assumeTrue(
                Boolean.getBoolean("frog2.e2e.authenticated"),
                "Run e2eAuthenticatedSmoke to enable the login POST check");
        String userId = System.getenv("FROG2_E2E_USER_ID");
        String password = System.getenv("FROG2_E2E_PASSWORD");
        if (userId == null || userId.isBlank()
                || password == null || password.isEmpty()) {
            throw new IllegalStateException(
                    "Authenticated smoke requires both E2E credential environment variables");
        }

        HttpResponse<String> loginPage = get("login", "text/html");
        Matcher csrf = CSRF_INPUT.matcher(loginPage.body());
        assertTrue(csrf.find(), "Login page did not expose a CSRF token");

        String form = formField("userId", userId)
                + "&" + formField("password", password)
                + "&" + formField("_csrf", csrf.group(1));
        HttpRequest loginRequest = HttpRequest.newBuilder(baseUri.resolve("login"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> loginResponse = client.send(
                loginRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(302, loginResponse.statusCode(), "Authenticated login did not redirect");
        URI redirect = baseUri.resolve(header(loginResponse, "Location"));
        assertEquals(baseUri.resolve("dashboard").getPath(), redirect.getPath());

        for (String route : PROTECTED_ROUTES) {
            HttpResponse<String> response = get(route, "text/html");
            assertEquals(200, response.statusCode(), "Core page failed: /" + route);
            assertTrue(header(response, "Content-Type").startsWith("text/html"));
            assertTrue(response.body().toLowerCase(Locale.ROOT).contains("<html"));
            assertFalse(response.body().contains("id=\"loginForm\""));
        }

        for (String route : TABLE_ROUTES) {
            HttpResponse<String> response = get(route, "text/html");
            assertEquals(200, response.statusCode(), "Table page failed: /" + route);
            assertTrue(
                    response.body().contains("class=\"ui-table-footer\""),
                    "Shared table footer missing: /" + route);
            assertTrue(
                    response.body().contains("class=\"ui-table-pagination\""),
                    "Always-visible table pagination missing: /" + route);
        }

        HttpResponse<String> customers = get("customers", "text/html");
        Matcher detailLink = CUSTOMER_DETAIL_LINK.matcher(customers.body());
        assertTrue(detailLink.find(), "Customer list did not expose a detail link");
        URI detailUri = baseUri.resolve(
                detailLink.group(1).replace("&amp;", "&"));
        HttpResponse<String> detail = client.send(
                HttpRequest.newBuilder(detailUri)
                        .timeout(Duration.ofSeconds(20))
                        .header("Accept", "text/html")
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, detail.statusCode());
        assertTrue(detail.body().contains("detail-section--summary"));
        assertTrue(detail.body().contains("detail-section--collapsible"));
        assertTrue(detail.body().contains("environment-detail ui-detail"));
        assertFalse(detail.body().contains("id=\"loginForm\""));
    }

    private static HttpResponse<String> get(String relativePath, String accept) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve(relativePath))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", accept)
                .GET()
                .build();
        return client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void assertHealthyAsset(
            String relativePath, String expectedContentType) throws Exception {
        HttpResponse<String> response = get(relativePath, expectedContentType);
        assertEquals(200, response.statusCode(), relativePath);
        assertTrue(header(response, "Content-Type").contains(expectedContentType),
                relativePath);
        assertTrue(header(response, "Cache-Control").contains("public"),
                relativePath);
        assertFalse(response.body().isBlank(), relativePath);
    }

    private static String header(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name)
                .orElseThrow(() -> new AssertionError("Missing response header: " + name));
    }

    private static String formField(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void requireLoopbackUnlessExplicitlyAllowed(URI uri) {
        String host = uri.getHost();
        boolean loopback = "127.0.0.1".equals(host)
                || "localhost".equalsIgnoreCase(host)
                || "::1".equals(host);
        if (!loopback && !Boolean.getBoolean("frog2.e2e.allowRemote")) {
            throw new IllegalArgumentException(
                    "Remote E2E targets require -Dfrog2.e2e.allowRemote=true");
        }
    }
}
