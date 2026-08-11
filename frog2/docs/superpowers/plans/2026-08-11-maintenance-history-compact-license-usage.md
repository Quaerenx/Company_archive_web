# Maintenance History Compact License Usage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the approved A1 compact maintenance-history layout with a quiet, data-driven 14px usage ring while preserving readable numeric license data.

**Architecture:** Keep the existing URL, DAO query, pagination, and always-expanded record hierarchy. Format the existing license fields into a compact server-side display string, expose a bounded integer percentage for SVG rendering, and style the JSP with page-scoped CSS only.

**Tech Stack:** Java 22, Jakarta Servlet/JSP/JSTL, Gradle, JUnit 5, page-scoped CSS, inline SVG.

## Global Constraints

- Do not execute DB DDL or DML.
- Do not change URLs, form parameters, authorization, pagination, or DAO queries.
- Do not add dependencies, external assets, or inline CSS/JavaScript.
- The visual icon is `aria-hidden`; the adjacent percentage text remains the accessible source of truth.
- Do not commit, push, create a branch, or open a PR without separate approval.
- Deploy only to `/opt/tomcat-dev`; do not modify or restart `/opt/tomcat` or port 8080.

---

### Task 1: Compact license display contract

**Files:**
- Modify: `src/test/java/com/company/util/LicenseSummaryFormatterTest.java`
- Modify: `src/test/java/com/company/controller/MaintenanceServletAuthorizationTest.java`
- Modify: `src/main/java/com/company/util/LicenseSummaryFormatter.java`
- Modify: `src/main/java/com/company/model/MaintenanceRecordDTO.java`
- Modify: `src/main/java/com/company/controller/MaintenanceServlet.java`

**Interfaces:**
- Consumes: `licenseSizeGb`, `licenseUsageSize`, and `licenseUsagePct` from `MaintenanceRecordDTO`.
- Produces: `licenseSummary` in `used / capacity TB · percentage` form and nullable `Integer licenseUsageProgressPct` bounded to `0..100`.

- [ ] **Step 1: Write failing formatter and controller tests.**
  Assert literal outputs `3 / 12 TB · 25%`, `0.5 / 2 TB · 25%`, and `1 / 2 TB · 50%`. Assert progress values `25`, clamped `0`, clamped `100`, and `null` when no data exists. Assert the history GET prepares both summary and progress fields.
- [ ] **Step 2: Verify RED.**
  Run: `./gradlew test --tests com.company.util.LicenseSummaryFormatterTest --tests com.company.controller.MaintenanceServletAuthorizationTest`
  Expected: FAIL because the compact output and progress property do not yet exist.
- [ ] **Step 3: Implement the smallest server-side view model change.**
  Preserve input parsing and percentage fallback. Format TB values with at most two decimals and no trailing zeroes. Add a bounded rounded progress getter/setter and populate it in the existing history loop.
- [ ] **Step 4: Verify GREEN.**
  Run the same targeted test command and require zero failures.

### Task 2: A1 markup and minimalist usage ring

**Files:**
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- Modify: `src/main/webapp/maintenance/maintenance_history.jsp`
- Modify: `src/main/webapp/resources/css/pages/maintenance_history.css`

**Interfaces:**
- Consumes: `record.licenseSummary` and `record.licenseUsageProgressPct`.
- Produces: `.license-usage-line`, `.license-usage-icon`, and a 14px SVG ring whose progress circle uses `pathLength="100"`.

- [ ] **Step 1: Write a failing page contract test.**
  Require the numeric summary, an `aria-hidden="true"` icon, a non-focusable SVG, a neutral track, a progress circle, and page-scoped 14px styling. Require that the icon is conditional on a valid percentage.
- [ ] **Step 2: Verify RED.**
  Run: `./gradlew test --tests com.company.layout.MaintenanceFormAssetContractTest`
  Expected: FAIL because the approved markup and classes are absent.
- [ ] **Step 3: Implement approved A1 markup and CSS.**
  Keep the two-column facts grid, put the icon and compact numeric summary on one line, reduce excess record spacing, and turn the note box into a quiet divider section. Do not use the icon as the only status signal.
- [ ] **Step 4: Verify GREEN and JSP compilation.**
  Run: `./gradlew test --tests com.company.layout.MaintenanceFormAssetContractTest --tests com.company.layout.MaintenanceHistoryViewContractTest jspcJava22`
  Expected: PASS and JspC zero errors.

### Task 3: Regression verification and development-only deployment

**Files:**
- Verify: all changed files above
- Deploy artifact: `build/libs/frog2.war`

**Interfaces:**
- Consumes: the verified reproducible WAR.
- Produces: development-only deployment with a timestamped rollback backup.

- [ ] **Step 1: Run full verification.**
  Run `./gradlew clean build`, `./gradlew jspcJava22`, and `git diff --check`. Review `git diff` for unrelated changes and secrets.
- [ ] **Step 2: Record production invariants read-only.**
  Record production Tomcat PID, production WAR SHA-256, and port 8080 GET status without exposing credentials.
- [ ] **Step 3: Back up and deploy development only.**
  Back up the development WAR, exploded app, and work directory to a new timestamped path. Deploy the verified WAR and restart only the development Tomcat.
- [ ] **Step 4: Smoke-test read-only routes.**
  Verify the development login GET, static CSS, and unauthenticated redirect. If a safe existing authenticated session is available, inspect the maintenance-history DOM without issuing any write request.
- [ ] **Step 5: Recheck production invariants.**
  Confirm the production PID, WAR hash, and port 8080 response are unchanged.
