# Maintenance History Comparison Table Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the maintenance history card list with a dense, accessible two-row comparison table that keeps every inspection note fully visible.

**Architecture:** Build immutable `MaintenanceHistoryRowView` objects from the already loaded, newest-first page of `MaintenanceRecordDTO` records. The JSP renders one semantic `<tbody>` per record, and page-scoped CSS provides the dense desktop table plus a contained horizontal scroll experience at narrow widths without adding queries or JavaScript behavior.

**Tech Stack:** Java 22, Jakarta Servlet 6, JSP 3.1/JSTL, JUnit 5, Gradle WAR, page-scoped CSS.

## Global Constraints

- Preserve the existing chart, pagination, URL, authorization, edit link, DAO ordering, form parameters, and raw history data.
- Keep complete note text visible with preserved newlines; do not clamp, collapse, merge, or aggregate records.
- Use existing Archive design tokens only; add no cards, shadows, gradients, colors, `!important`, dependencies, or breakpoints.
- Do not issue additional SQL or run DB DDL/DML.
- Modify and deploy only the development application; do not change `/opt/tomcat`, port 8080, or the production service.
- Preserve all pre-existing working-tree changes and do not commit or push.

---

### Task 1: Dedicated history row view model

**Files:**
- Create: `src/main/java/com/company/controller/MaintenanceHistoryRowView.java`
- Create: `src/test/java/com/company/controller/MaintenanceHistoryRowViewTest.java`
- Modify: `src/main/java/com/company/util/LicenseSummaryFormatter.java`
- Modify: `src/test/java/com/company/util/LicenseSummaryFormatterTest.java`

**Interfaces:**
- Consumes: `MaintenanceRecordDTO` values in DAO order, newest first.
- Produces: `MaintenanceHistoryRowView.fromRecords(List<MaintenanceRecordDTO>)`, `record`, `usedTerabytes`, `capacityTerabytes`, `usagePercentage`, `usageProgressPercentage`, `deltaLabel`, and `deltaTone` getters.

- [ ] **Step 1: Write the failing row-view tests**

  Cover separated TB values, explicit and calculated percentages, positive/negative/flat deltas, missing values, duplicate calendar months remaining separate, and the oldest visible page row returning `—` because no older record is loaded.

- [ ] **Step 2: Run the focused test and verify RED**

  Run: `./gradlew test --tests com.company.controller.MaintenanceHistoryRowViewTest`

  Expected: compilation failure because `MaintenanceHistoryRowView` and separated formatter methods do not exist.

- [ ] **Step 3: Implement the minimal formatter and immutable row view**

  Add formatter methods that reuse the existing unit parsing, and build each delta only against index `i + 1`. Use fixed safe tones `up`, `down`, `flat`, and `unavailable`; return `—` when comparison data is absent.

- [ ] **Step 4: Run focused tests and verify GREEN**

  Run: `./gradlew test --tests com.company.controller.MaintenanceHistoryRowViewTest --tests com.company.util.LicenseSummaryFormatterTest`

  Expected: PASS.

### Task 2: Controller integration and DTO cleanup

**Files:**
- Modify: `src/main/java/com/company/controller/MaintenanceServlet.java`
- Modify: `src/main/java/com/company/model/MaintenanceRecordDTO.java`
- Modify: `src/test/java/com/company/controller/MaintenanceServletAuthorizationTest.java`

**Interfaces:**
- Consumes: `MaintenanceHistoryRowView.fromRecords(records)` from Task 1.
- Produces: request attribute `historyRows`; retains `records` for compatibility and `usageSeries` for the chart.

- [ ] **Step 1: Change the controller test first**

  Assert that history forwarding exposes one `historyRows` entry with the expected split values, while the original DTO remains free of presentation mutation.

- [ ] **Step 2: Run the controller test and verify RED**

  Run: `./gradlew test --tests com.company.controller.MaintenanceServletAuthorizationTest`

  Expected: FAIL because `historyRows` is absent.

- [ ] **Step 3: Set `historyRows` without a query and remove transitional DTO presentation fields**

  Replace the DTO mutation loop with one call to `MaintenanceHistoryRowView.fromRecords(records)`. Remove `licenseSummary` and `licenseUsageProgressPct` only after `rg` confirms the new JSP and tests are the only pending consumers.

- [ ] **Step 4: Run controller and formatter tests and verify GREEN**

  Run: `./gradlew test --tests com.company.controller.MaintenanceServletAuthorizationTest --tests com.company.controller.MaintenanceHistoryRowViewTest --tests com.company.util.LicenseSummaryFormatterTest`

  Expected: PASS and no remaining DTO presentation-field references.

### Task 3: Semantic two-row JSP table

**Files:**
- Modify: `src/main/webapp/maintenance/maintenance_history.jsp`
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- Modify: `src/test/java/com/company/layout/CssLayoutStructureTest.java`

**Interfaces:**
- Consumes: request attribute `historyRows` and each row's safe getter values.
- Produces: captioned seven-column table, one `<tbody>` per record, row-group month header spanning metric and note rows, and the existing edit URL on the month/date link.

- [ ] **Step 1: Replace card assertions with failing table contract assertions**

  Require a caption, seven `scope="col"` headers, `scope="rowgroup" rowspan="2"`, a metric row, a note row with `colspan="6"`, full escaped note output, a `기록 없음` fallback, a scroll region, and absence of `.history-item` cards.

- [ ] **Step 2: Run layout tests and verify RED**

  Run: `./gradlew test --tests com.company.layout.MaintenanceFormAssetContractTest --tests com.company.layout.CssLayoutStructureTest`

  Expected: FAIL against the existing card markup.

- [ ] **Step 3: Implement the minimal semantic table**

  Iterate over `historyRows`, retain every record, use `c:out` for record text, keep the full note row always present, provide `—` fallbacks, preserve exact dates and registration timestamps, and leave the ring `aria-hidden="true"` beside numeric percentage text.

- [ ] **Step 4: Run layout tests and JspC and verify GREEN**

  Run: `./gradlew test --tests com.company.layout.MaintenanceFormAssetContractTest --tests com.company.layout.CssLayoutStructureTest jspcJava22`

  Expected: PASS and JspC zero errors.

### Task 4: Dense page-scoped styling and script cleanup

**Files:**
- Modify: `src/main/webapp/resources/css/pages/maintenance_history.css`
- Modify: `src/main/webapp/resources/js/pages/maintenance_history.js`
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`

**Interfaces:**
- Consumes: `.history-table-scroll`, `.history-comparison-table`, `.history-record-group`, `.history-metric-row`, and `.history-note-row` markup from Task 3.
- Produces: fixed metric alignment, quiet full-width note rows, strong group boundaries, tabular numerals, visible keyboard focus, and contained horizontal scrolling at the existing 768px breakpoint.

- [ ] **Step 1: Add failing style and script contract assertions**

  Require scoped table selectors, `overflow-x: auto`, a readable table `min-width`, `white-space: pre-wrap`, and no history-card animation selector.

- [ ] **Step 2: Run the layout test and verify RED**

  Run: `./gradlew test --tests com.company.layout.MaintenanceFormAssetContractTest --tests com.company.layout.CssLayoutStructureTest`

  Expected: FAIL until the page CSS and obsolete card animation are changed.

- [ ] **Step 3: Replace only obsolete card CSS and animation**

  Keep chart/header/empty-state rules intact. Style the two rows as one group, use current tokens, right-align numeric columns, preserve full note wrapping, expose a narrow-screen scroll hint, and remove the unused `.history-item` entrance animation without adding JavaScript.

- [ ] **Step 4: Verify CSS, JavaScript, JSP, and focused tests**

  Run: `./gradlew test --tests com.company.layout.MaintenanceFormAssetContractTest --tests com.company.layout.CssLayoutStructureTest jspcJava22`

  Run: `node --check src/main/webapp/resources/js/pages/maintenance_history.js`

  Expected: all PASS.

### Task 5: Full verification and development-only deployment

**Files:**
- Verify all modified files above.
- Create no source files during deployment.

**Interfaces:**
- Consumes: completed WAR from Tasks 1–4.
- Produces: a timestamped development rollback backup and a verified development deployment; production remains byte-for-byte and process-identical.

- [ ] **Step 1: Review the diff and working-tree boundaries**

  Run: `git diff --check` and inspect `git diff --` for all touched files. Confirm no DB, auth, production config, secret, unrelated formatting, or generated source changes.

- [ ] **Step 2: Run complete verification twice where required**

  Run twice: `./gradlew clean build`

  Run: `./gradlew jspcJava22 verifyWarContents`

  Run: `find src/main/webapp/resources/js -type f -name '*.js' -print0 | xargs -0 -n1 node --check`

  Expected: all tests pass, JspC zero errors, WAR allowlist passes, and JavaScript syntax checks pass.

- [ ] **Step 3: Capture deployment invariants and back up development artifacts**

  Record development and production PID, WAR SHA-256, and login GET status without printing secrets. Back up the current development WAR, exploded app, and relevant work directory to `/opt/frog2-dev/backups/maintenance-history-comparison-<timestamp>`.

- [ ] **Step 4: Deploy and restart only `tomcat-dev.service`**

  Install the verified `build/libs/frog2.war`, preserve expected owner/mode, restart only the development service, and wait for the development connector.

- [ ] **Step 5: Smoke test and confirm production invariants**

  Verify development login GET, versioned CSS, and unauthenticated history redirect; inspect recent development logs for JSP compile, `ClassNotFoundException`, `NoSuchMethodError`, and linkage failures. Confirm production PID, WAR SHA-256, and port 8080 login GET match the pre-deployment values.

---

## Self-Review

- Spec coverage: row normalization, comparison semantics, duplicate-month preservation, full notes, accessibility, responsive containment, and dev-only deployment all map to explicit tasks.
- Placeholder scan: no deferred implementation placeholders or unspecified test steps remain.
- Type consistency: `historyRows` contains `MaintenanceHistoryRowView`; JSP fields match the getters defined in Task 1.
- Scope: chart, queries, database, forms, auth, production configuration, and other pages remain unchanged.
