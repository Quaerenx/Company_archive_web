package com.company.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("e2e-https")
class HttpsSessionCookieAuditTest {
    private static final String BASE_URL_ENV = "FROG2_HTTPS_BASE_URL";

    @Test
    void loginSessionCookieAndTransportHeadersAreHardened() throws Exception {
        URI baseUri = configuredBaseUri();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(baseUri.resolve("login"))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "text/html")
                .GET()
                .build();

        HttpResponse<Void> response = client.send(
                request, HttpResponse.BodyHandlers.discarding());
        assertEquals(200, response.statusCode(),
                "HTTPS login GET must succeed without a redirect");

        SessionCookiePolicy.Inspection cookie = SessionCookiePolicy.inspect(
                response.headers().allValues("Set-Cookie"));
        assertTrue(cookie.sessionCookiePresent(),
                "HTTPS login did not issue a JSESSIONID cookie");
        assertTrue(cookie.httpOnly(), "JSESSIONID must include HttpOnly");
        assertTrue(cookie.secure(), "JSESSIONID must include Secure");
        assertTrue(cookie.sameSiteStrict(),
                "JSESSIONID must include SameSite=Strict");

        String hsts = response.headers()
                .firstValue("Strict-Transport-Security")
                .orElse("")
                .toLowerCase(Locale.ROOT);
        assertTrue(hsts.contains("max-age="),
                "HTTPS responses must include HSTS with max-age");
    }

    private static URI configuredBaseUri() {
        String configured = System.getenv(BASE_URL_ENV);
        if (configured == null || configured.isBlank()) {
            throw new IllegalArgumentException(
                    BASE_URL_ENV + " is required");
        }
        configured = configured.trim();
        if (!configured.endsWith("/")) {
            configured += "/";
        }

        URI uri = URI.create(configured);
        assertEquals("https", uri.getScheme(),
                BASE_URL_ENV + " must use https://");
        assertFalse(uri.getHost() == null || uri.getHost().isBlank(),
                BASE_URL_ENV + " must contain a host");
        assertTrue(uri.getUserInfo() == null,
                BASE_URL_ENV + " must not contain credentials");
        assertTrue(uri.getQuery() == null && uri.getFragment() == null,
                BASE_URL_ENV + " must not contain a query or fragment");
        return uri;
    }
}
