# Typography dashboard preview retirement record

## Status

Retired from the development codebase on 2026-08-07. The user chose to skip the
authenticated visual-baseline capture and explicitly requested the recommended
cleanup sequence instead. The production dashboard remains the only dashboard.

## Evidence

- The shared navigation does not link to `/dashboard-typography-preview`.
- The production dashboard does not include the preview JSP or CSS.
- The servlet performs no DAO, JDBC, DDL, or DML operation.
- The preview has one dedicated JSP, one scoped stylesheet, one servlet mapping,
  and focused contract tests.

## Removed together

- `src/main/java/com/company/controller/TypographyDashboardPreviewServlet.java`
- `src/main/webapp/WEB-INF/views/design/typography_dashboard.jsp`
- `src/main/webapp/resources/css/pages/typography_dashboard_assignees.css`
- the `TypographyDashboardPreviewServlet` declaration and mapping in `WEB-INF/web.xml`
- `TypographyDashboardPreviewServletTest.java`
- `TypographyDashboardPreviewContractTest.java`
- the preview-specific exclusions in `ambient-background.css` and
  `ambient-background.js`
- the preview-only `ApplicationEnvironment.isDevelopmentFeatureEnabled()` helper

The meeting editor preview remains because it is an active user feature. The
`main_style.css` compatibility entry point also remains because runtime access-log
evidence is insufficient to prove that its public URL is unused.

Removal does not require a database migration or any DB access.

## Verification

- Static runtime references to the retired route, JSP, CSS, class, and body scope: 0.
- Java tests: 336 passed; clean build and WAR verification passed twice.
- JspC: 44 inputs, 36 generated Java files, 61 classes, 0 errors.
- Development WAR SHA-256:
  `ef0ada52624ab93432774dd8a94f26b21f5902703486c00b7ff696af7d878c84`.
- Development login and static assets returned HTTP 200; protected GET routes
  retained the expected unauthenticated redirect to `/frog2/login`.
- Development startup log had no JSP, class-loading, or linkage errors.
- Production PID, WAR SHA-256, and login HTTP response remained unchanged.

## Rollback

The complete pre-deployment development runtime is preserved at:

`/opt/frog2-dev/backups/preview-retirement-20260807-132113`

It contains `frog2.war`, `frog2-exploded`, and `frog2-work`. Stop only
`tomcat-dev.service`, move the current development artifacts aside, restore those
three items to their original paths, and start only `tomcat-dev.service`.
