# Archive Domain Functionality Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Subagents and Git commits are intentionally disabled by the user constraints.

**Goal:** Bound maintenance history, reduce customer-detail query round trips, and lock every audited domain contract without changing shared database state, URLs, or form parameters.

**Architecture:** Keep the Servlet → DAO → JSP shape. Use a bounded `PageResult` for maintenance history and one union projection for the three customer-detail environments. Preserve already-correct domains through behavioral tests and document rather than delete uncertain legacy compatibility code.

**Tech Stack:** Java 22, Gradle WAR, Jakarta Servlet 6, JSP 3.1/JSTL, JUnit 5, Vertica SQL, JavaScript.

## Global Constraints

- Do not connect to the shared database for writes or execute DDL/DML/migrations.
- Do not modify or restart `/opt/tomcat`, its WAR, or the 8080 service.
- Preserve existing URLs, form field names, actions, redirects, and UI structure.
- Do not create commits, branches, pushes, or pull requests.
- Work in `/tmp/frog2-domain-20260811.GaU0RW` first and copy only verified files back.

---

### Task 1: Customer-detail bounded aggregate read

**Files:**
- Modify: `src/test/java/com/company/model/CustomerDetailDAOJdbcContractTest.java`
- Modify: `src/main/java/com/company/model/CustomerDetailDAO.java`
- Modify: `src/test/java/com/company/controller/CustomerControllerCompatibilityTest.java`
- Modify: `src/main/java/com/company/controller/CustomerDetailQueryService.java`

**Interfaces:**
- Consumes: `CustomerDetailDAO.getCustomerDetails(String)` and existing detail-page request attributes.
- Produces: the same `CustomerDetailSet` and `CustomerDetailQueryService.ViewData`, with one environment query and no separate active-customer query.

- [x] Add a DAO test whose fake JDBC fixture supplies prod/stg/dev rows from one result set and asserts one prepared statement, three bound customer-name parameters, stable environment mapping, and one connection close.
- [x] Run `./gradlew --offline --no-daemon test --tests com.company.model.CustomerDetailDAOJdbcContractTest` and confirm the new test fails because three statements are prepared.
- [x] Replace the three sequential detail selects with one `UNION ALL` projection carrying a `detail_environment` discriminator; require `is_deleted = 1` on production only.
- [x] Re-run the DAO test and confirm it passes.
- [x] Add a controller service test proving the production detail supplies the same `CustomerDTO` fields and that the standalone customer DAO is not called for the detail view.
- [x] Run the focused controller test and confirm it fails before changing the service.
- [x] Add a small mapper from production detail to `CustomerDTO`, preserve all request-facing fields, and retain separate EOS lookup.
- [x] Re-run both focused test classes and review the diff for unrelated customer behavior.

### Task 2: Maintenance schedule boundary contract

**Files:**
- Modify: `src/test/java/com/company/model/MaintenanceScheduleTest.java`
- Modify: `src/test/java/com/company/model/CustomerDAOMaintenanceAssignmentTest.java`
- Inspect only: `src/main/resources/db/migration/V20260804_08__set_konkuk_hospital_quarterly_schedule.sql`

**Interfaces:**
- Consumes: `MaintenanceSchedule.isDue(YearMonth)` and `CustomerDAO.getMaintenanceCustomerAssignments(YearMonth)`.
- Produces: no new production API; adds executable evidence for all approved schedule boundaries.

- [x] Add literal expected-month tests for December→March, leap February, a non-March quarterly anchor, disabled/effective range, and monthly fallback.
- [x] Add a customer-assignment assertion that the SQL filters `d.is_deleted = 1` and missing schedule columns yield `monthlyDefault()`.
- [x] Verify the Konkuk override targets exactly `건국대병원`, interval 3, anchor `2000-03-01`, without editing or running it.
- [x] Run the three schedule/assignment/migration test classes and record the result.

### Task 3: Maintenance-history pagination and formatter defect

**Files:**
- Modify: `src/test/java/com/company/model/MaintenanceRecordDAOPaginationTest.java`
- Modify: `src/main/java/com/company/model/MaintenanceRecordDAO.java`
- Modify: `src/test/java/com/company/controller/MaintenanceServletAuthorizationTest.java`
- Modify: `src/main/java/com/company/controller/MaintenanceServlet.java`
- Modify: `src/main/webapp/maintenance/maintenance_history.jsp`
- Modify: `src/test/java/com/company/layout/MaintenanceHistoryViewContractTest.java` (create if absent)

**Interfaces:**
- Consumes: `PageResult<T>`, `LicenseUsageSeriesBuilder`, `LicenseSummaryFormatter`, request `customerName`.
- Produces: `getMaintenanceRecordsByCustomer(String, int, int)`, optional request `historyPage`, and request attributes `currentPage`, `totalPages`, `totalCount`, `pageSize`.

- [x] Add a DAO test asserting `COUNT(*) OVER ()`, `ORDER BY CASE WHEN inspection_date IS NULL THEN 1 ELSE 0 END, inspection_date DESC, maintenance_id DESC`, `LIMIT 20 OFFSET 0`, and correct `PageResult` metadata.
- [x] Add an out-of-range test asserting count fallback and a rerun on the final page.
- [x] Run the DAO pagination tests and confirm both new tests fail because the bounded method does not exist.
- [x] Implement the bounded method with the injected connection provider and remove the unbounded internal call path.
- [x] Re-run the DAO tests and confirm green.
- [x] Add Servlet tests proving missing `historyPage` means 1, invalid/zero/negative values return 400 `invalid_history_page`, page items feed both chart and list, and each non-null formatted summary is set on its record.
- [x] Run the Servlet test and confirm the valid-history case fails on the current unbounded DAO call and unused summary map.
- [x] Inject `CustomerDAO` into the Servlet constructor used by tests, parse `historyPage` strictly, call the bounded DAO, expose pagination attributes, and set `record.licenseSummary` directly.
- [x] Add previous/next URLs and the shared table footer tag to the JSP without changing the existing history cards or actions.
- [x] Run the focused DAO, Servlet, formatter, layout, and JspC tasks.

### Task 4: Meeting/comment and troubleshooting regression contracts

**Files:**
- Modify: `src/test/java/com/company/model/MeetingCommentDAOPaginationTest.java`
- Modify: `src/test/java/com/company/layout/TroubleshootingViewContractTest.java`
- Modify only if a test exposes a defect: meeting/troubleshooting production files.

**Interfaces:**
- Consumes: comment `beforeCommentId`, troubleshooting `q`, `scope`, `page`, and `pageSize`.
- Produces: no new API; executable stability and parameter-preservation guarantees.

- [x] Add a literal concurrent-insert fixture proving a new larger comment ID cannot duplicate or skip the older page selected by the prior cursor.
- [x] Add JSP contract assertions that previous/next and detail-return URLs preserve `q`, `scope=content`, `page`, and `pageSize`.
- [x] Run meeting-comment, meeting authorization/form, troubleshooting request/DAO/Servlet/view tests.
- [x] Change production code only if a behavioral assertion fails for a real contract defect; otherwise record “verified, no production change.”

### Task 5: My-page, personal-host, and file-repository regression contracts

**Files:**
- Modify: `src/test/java/com/company/model/MonthlyCustomerResponseDAOContractTest.java`
- Modify or create: `src/test/java/com/company/controller/UserVmHostServletOwnershipTest.java`
- Modify: `src/test/java/com/company/filerepo/FileRepositoryServiceTest.java`
- Modify only if a test exposes a defect: corresponding production files.

**Interfaces:**
- Consumes: session principal `userId`, owner-scoped DAO methods, repository cursor.
- Produces: no new API; name-change/duplicate-name/request-userId isolation and repository cursor stability evidence.

- [x] Add fixtures with equal display names and different immutable IDs; assert monthly response filters bind only the session ID.
- [x] Add a Servlet test passing a foreign `userId` request parameter and assert personal-host reads/mutations still use only the session ID.
- [x] Add a repository test proving invalid cursors return 400 and bounded pages remain stable after an unrelated later-sorting entry.
- [x] Run focused tests and change production code only for reproduced defects.

### Task 6: Deprecation classification and contract map

**Files:**
- Create: `docs/audits/2026-08-10/04-domain-functionality-remediation.md`
- Inspect: `src/main/webapp/resources/css/main_style.css`
- Inspect: legacy migration, JSP, Servlet, button-class references, and development access logs.

**Interfaces:**
- Produces: URL→Servlet→DAO→view/error map and evidence-based immediate/log-required/compatibility-required deprecation table.

- [x] Enumerate each scoped domain route, HTTP method, action/view, DAO read/write, target JSP/JSON, redirect/flash, and 4xx/5xx behavior.
- [x] Count static references and sanitized development access-log hits for `main_style.css` and legacy routes without printing client identifiers or secrets.
- [x] Classify candidates. Do not delete `main_style.css` if log evidence is insufficient, any legacy migration, or any route requiring compatibility.
- [x] Remove an item only when both static/runtime evidence prove it unused and all focused verification passes; otherwise document it.

### Task 7: Full verification and source handoff

**Files:**
- Review every changed file and the final diff.

**Interfaces:**
- Produces: verified source patch and remediation report; no deployment or Git history changes.

- [x] Run focused domain tests, then `./gradlew --offline --no-daemon clean build` twice consecutively.
- [x] Confirm both builds include all tests, JspC zero errors, WAR allowlist success, and identical expected task coverage.
- [x] Run `node --check` for every application JavaScript file and `git diff --check`.
- [x] Review `git diff --stat`, changed paths, debug output, temporary files, and accidental secrets.
- [x] Copy only verified source/test/doc files from the isolated copy to `/opt/frog2-dev/repo/frog2` using a timestamped source backup for overwritten files.
- [x] Repeat the complete verification in the real source tree.
- [x] Perform GET-only development smoke checks and compare production PID, WAR SHA-256, and 8080 response before/after.
- [x] Report actual query/connection/DOM bounds, unexecuted DB work, remaining risks, and confidence calculated from test/code-review/logical evidence.
