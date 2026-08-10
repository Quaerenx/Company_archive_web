# Archive runtime security policy

Status: development baseline on 2026-08-10.

## Content Security Policy

Application JSPs, tag files, and the administrator pool monitor do not contain
inline scripts, inline style blocks, event-handler attributes, or `style`
attributes. The response CSP therefore omits `unsafe-inline` from both
`script-src` and `style-src`. Existing self-hosted scripts and the two reviewed
vendor origins remain allowed.

## Session cookie

- `HttpOnly` is mandatory in `WEB-INF/web.xml`.
- `SameSite=Strict` is mandatory through the application-local Tomcat
  `CookieProcessor` in `META-INF/context.xml`.
- `Secure` is emitted by Tomcat when the request is secure. It is intentionally
  not forced for the HTTP-only development connector because doing so would
  prevent the browser from returning the development session cookie.
- A production TLS terminator must make Tomcat's request secure through a
  reviewed connector or `RemoteIpValve` configuration. The application does
  not trust `X-Forwarded-Proto` directly.
- HSTS is emitted only for requests that Tomcat reports as secure.

Production rollout remains blocked until an HTTPS response is verified to
contain `JSESSIONID` with `HttpOnly; Secure; SameSite=Strict`.

Run the read-only runtime audit against the exact Archive context root:

```bash
FROG2_HTTPS_BASE_URL=https://archive.example/frog2/ ./gradlew httpsCookieAudit
```

The audit performs only an anonymous login GET. It never logs the session
cookie value and accepts only a trusted HTTPS certificate. It also requires an
HSTS header with `max-age`.

## JDBC lifecycle

Application shutdown closes HikariCP, clears its data-source reference, and
deregisters only JDBC drivers loaded by the Archive web-application
classloader. Drivers owned by the shared Tomcat classloader are not touched.
This prevents the application-bundled Vertica driver from retaining a stopped
web-application classloader during hot redeploy.
