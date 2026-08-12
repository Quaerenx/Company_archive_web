# Maintenance History Expanded Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the expanded maintenance-history row as a full-width summary followed by a readable 70/30 note and metadata layout.

**Architecture:** Keep all existing server-rendered data and disclosure behavior. Change only the semantic JSP wrappers and scoped page CSS, protected by layout-contract tests; then refresh the asset version and deploy the verified WAR to development only.

**Tech Stack:** JSP 3.1/JSTL, scoped CSS, JUnit 5 contract tests, Gradle WAR, Tomcat 10.1.57.

## Global Constraints

- Preserve all URLs, form parameters, record values, edit behavior, and ARIA disclosure contracts.
- Add no new dependency, JavaScript behavior, database field, DDL, or DML.
- Do not modify or restart production Tomcat.
- Do not commit or push without separate approval.

---

### Task 1: Expanded-detail structure contract

**Files:**
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- Modify: `src/main/webapp/maintenance/maintenance_history.jsp`

**Interfaces:**
- Consumes: existing `MaintenanceHistoryRowView` values and `${maintenanceEditUrl}`.
- Produces: `.history-detail-summary`, `.history-detail-metrics`, `.history-detail-lower`, `.history-detail-note-section`, and `.history-detail-meta-section`.

- [x] Add a failing contract requiring the full-width summary and lower note/metadata wrappers.
- [x] Run the focused test and confirm it fails because the new hierarchy is absent.
- [x] Move existing metric, note, and metadata markup into the approved semantic wrappers without changing values or conditions.
- [x] Run the focused test and confirm it passes.

### Task 2: Readable desktop and mobile hierarchy

**Files:**
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- Modify: `src/main/webapp/resources/css/pages/maintenance_history.css`

**Interfaces:**
- Consumes: Task 1 wrapper classes.
- Produces: four-column summary metrics, 70/30 lower grid, vertical metadata rows, and single-column mobile stacking.

- [x] Add failing CSS contracts for `repeat(4, minmax(0, 1fr))`, `minmax(0, 7fr) minmax(220px, 3fr)`, non-wrapping timestamps, and mobile stacking.
- [x] Run the focused test and confirm it fails against the current three-column grid.
- [x] Replace the three-column root grid with the approved two-level hierarchy using existing tokens only.
- [x] Run focused layout and design-contract tests.

### Task 3: Cache, verification, and development deployment

**Files:**
- Modify: `src/test/java/com/company/layout/PageShellContractTest.java`
- Modify: `src/main/webapp/WEB-INF/web.xml`

**Interfaces:**
- Consumes: final JSP/CSS assets.
- Produces: a unique asset-version URL and verified development WAR.

- [x] Add a failing asset-version contract for `20260812-maintenance-detail-hierarchy-1`.
- [x] Update `frog2AssetVersion` and run the focused test.
- [x] Run full tests, JavaScript syntax, JspC, WAR allowlist, and `git diff --check` through `./gradlew clean build`.
- [x] Back up the current development WAR/exploded/work paths.
- [x] Deploy and restart only `tomcat-dev.service`.
- [x] Verify the new markup/CSS, login GET, unauthenticated redirect, Tomcat logs, rollback path, and unchanged production PID/WAR/8080 response.
