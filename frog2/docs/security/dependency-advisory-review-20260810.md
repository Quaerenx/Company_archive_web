# Archive dependency advisory review

Review date: 2026-08-10. Only official vendor/security pages, GitHub Security
Advisory where applicable, and NVD records were considered. A version was not
classified as vulnerable without matching the deployed version and required
runtime conditions.

## Valid finding

### P1 — Apache Tomcat 10.1.41 multipart denial of service exposure

- Deployed runtime: Apache Tomcat 10.1.41 on Java 22.
- Actual path: Archive exposes authenticated Servlet multipart upload at
  `/file-repository/upload`.
- Official advisory: <https://tomcat.apache.org/security-10.html>
- Matching issues include CVE-2025-48988 and CVE-2025-48976, fixed in 10.1.42;
  10.1.41 is in the affected range. Later CVE-2025-61795 also affects through
  10.1.46 and concerns delayed multipart temporary-file cleanup.
- Current connectors do not expose the newer `maxPartCount` or
  `maxPartHeaderSize` controls because those fixes are not in 10.1.41.

Application file-count and byte limits remain useful, but container multipart
parsing occurs before application validation and cannot fully mitigate these
container issues. Upgrade production and development together to a currently
supported Tomcat 10.1.x release after a separate compatibility, backup, and
rollback review. This task did not modify shared `/opt/tomcat` binaries or
configuration.

## Condition-dependent findings not remotely reachable here

### Logback 1.4.14 configuration processing

- Official/NVD sources:
  - <https://nvd.nist.gov/vuln/detail/CVE-2026-1225>
  - <https://logback.qos.ch/news.html#1.5.25>
- The version range includes the deployed Logback, but exploitation requires
  write access to an existing Logback configuration and a useful class already
  on the classpath.
- Archive packages a fixed local `logback.xml`; no application route can write
  it, and the configuration uses only local rolling-file appenders.
- The later conditional-processing advisory CVE-2026-13006 is also not on an
  actual call path: the project has no Janino dependency and no `<if>`/
  `condition` configuration.

Result: no remote Archive finding was validated. Upgrade Logback in a separate
dependency-compatibility change rather than mixing it into this targeted patch.

## Rejected candidates

- Chart.js CVE-2020-7746 affects versions before 2.9.4. Archive now packages
  Chart.js 4.4.4, so the affected range does not match:
  <https://nvd.nist.gov/vuln/detail/CVE-2020-7746>.
- RewriteValve, PreResources/PostResources, CGI, AJP, HTTP/2, client-certificate,
  clustering, and write-enabled default-Servlet Tomcat findings were checked
  against current configuration. Those features/conditions were not active.
  They do not erase the valid multipart finding.

## No validated official match found

The deployed HikariCP 5.1.0, jBCrypt 0.4, Jakarta JSTL 3.x, Font Awesome Free
5.15.4 static CSS/fonts, and Vertica JDBC 23.3.0-0 were reviewed against
available official/vendor/NVD material and their actual use. No concrete
remotely exploitable advisory was validated from that evidence. This is not a
claim that future advisories cannot exist; repeat the review at each release.

Font Awesome and Chart.js are now self-hosted from versioned files with bundled
licenses and provenance checksums, removing runtime CDN compromise as a trust
dependency.
