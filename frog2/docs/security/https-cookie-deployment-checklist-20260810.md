# Archive HTTPS session-cookie verification

Status on 2026-08-10: **BLOCKED by missing Archive HTTPS endpoint**.

The current production and development Tomcat connectors are direct HTTP
listeners. Their sample TLS connectors are commented out, and no reviewed
reverse proxy or `RemoteIpValve` is active. Therefore a successful local HTTP
check cannot prove the production HTTPS cookie contract.

## Application contract already verified

- `web.xml` sets the session cookie `HttpOnly` flag.
- `META-INF/context.xml` sets `SameSite=Strict` through Tomcat's
  `Rfc6265CookieProcessor`.
- `SecurityHeadersFilter` emits HSTS only when `request.isSecure()` is true.
- Logout explicitly expires `JSESSIONID` with the application context path,
  `HttpOnly`, `SameSite=Strict`, and `Secure` only for a genuinely secure
  request. Plain HTTP development login is therefore not broken.
- `HttpsSessionCookieAuditTest` uses Java's normal certificate and hostname
  verification and inspects cookie attributes without printing cookie values.

## Required production check

Run only after a real, reviewed Archive HTTPS URL exists:

```text
FROG2_HTTPS_BASE_URL=https://<reviewed-archive-host>/<context>/ \
  ./gradlew httpsCookieAudit
```

Acceptance criteria:

1. The URL is HTTPS and has no embedded credentials, query, or fragment.
2. The certificate chain and hostname pass the default Java trust checks.
3. The login GET returns a session cookie with `HttpOnly`, `Secure`, and
   `SameSite=Strict`.
4. HSTS contains `max-age` of at least 31,536,000 seconds.
5. Test output reports only booleans/attributes and never the cookie value.

## Reviewed proxy/Tomcat design, if TLS terminates upstream

Do not make the application trust `X-Forwarded-Proto` or `X-Forwarded-For`
directly. The edge proxy must overwrite inbound forwarding headers, Tomcat must
not be reachable around that proxy, and a Tomcat `RemoteIpValve` may trust only
the proxy's exact network addresses. Never use a catch-all trusted-proxy regex.
After configuration review, verify both the external HTTPS response and a
direct HTTP development response.

Operational changes to a proxy, Connector, Valve, certificate, or firewall are
outside this repository patch and require separate approval and rollback.
