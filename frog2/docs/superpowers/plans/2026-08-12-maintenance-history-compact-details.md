# Maintenance History Compact Details Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a compact, filterable maintenance-history table with independently expandable details without changing Archive's database or write contracts.

**Architecture:** A validated `MaintenanceHistoryFilter` owns request normalization and SQL-safe LIKE patterns. `MaintenanceRecordDAO` applies the same fixed filter predicate to the paged list and fallback count, `MaintenanceServlet` maps valid GET parameters into request attributes, and `MaintenanceHistoryRowView` supplies display-only derived values. JSP renders all content and vanilla JavaScript progressively enhances independent disclosure buttons.

**Tech Stack:** Java 22, Jakarta Servlet 6, JSP 3.1/JSTL, vanilla JavaScript, scoped CSS, JUnit 5, Gradle WAR.

## Global Constraints

- Keep `GET /maintenance?view=history` and all existing form/write contracts unchanged.
- Execute no DDL, INSERT, UPDATE, or DELETE against the shared database.
- Do not invent a maintenance status field or infer one from notes.
- Reuse the existing 90% license-risk threshold.
- Add no production dependency, branch, commit, push, or PR.
- Do not modify or restart production Tomcat.

---

### Task 1: Read-only history filter contract

**Files:**
- Create: `src/main/java/com/company/model/MaintenanceHistoryFilter.java`
- Modify: `src/main/java/com/company/model/MaintenanceRecordDAO.java`
- Modify: `src/main/java/com/company/controller/MaintenanceServlet.java`
- Test: `src/test/java/com/company/model/MaintenanceHistoryFilterTest.java`
- Test: `src/test/java/com/company/model/MaintenanceRecordDAOPaginationTest.java`
- Test: `src/test/java/com/company/controller/MaintenanceServletAuthorizationTest.java`

- [x] Write failing tests for normalization, validation, literal LIKE escaping, list/count predicate parity, servlet propagation, and invalid-filter HTTP 400.
- [x] Run focused tests and confirm expected failures.
- [x] Implement the immutable filter, prepared fixed SQL predicates, and servlet attributes.
- [x] Run focused tests and review that filter values are only bound parameters.

### Task 2: Compact row view model

**Files:**
- Modify: `src/main/java/com/company/controller/MaintenanceHistoryRowView.java`
- Test: `src/test/java/com/company/controller/MaintenanceHistoryRowViewTest.java`

- [x] Write failing tests for note summary, blank fallback, partial license values, 90% risk, previous usage, neutral delta, and stable detail IDs.
- [x] Run the focused test and confirm expected failures.
- [x] Implement the smallest display derivations and rerun the test.

### Task 3: Server-rendered compact table and detail rows

**Files:**
- Modify: `src/main/webapp/maintenance/maintenance_history.jsp`
- Modify: `src/test/java/com/company/layout/MaintenanceHistoryViewContractTest.java`
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`

- [x] Add failing layout contracts for filters, one-line dates, integrated progress, note summaries, hidden details, registration metadata, and preserved pagination filters.
- [x] Implement the filter form, compact summary row, hidden detail row, edit link, and filtered empty state.
- [x] Run layout tests and inspect output escaping.

### Task 4: Independent disclosure and responsive styling

**Files:**
- Modify: `src/main/webapp/resources/js/pages/maintenance_history.js`
- Modify: `src/main/webapp/resources/css/pages/maintenance_history.css`
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`

- [x] Add failing contracts for disclosure before chart early-return, independent targets, accessible labels, native progress, neutral delta, compact rows, and responsive column priority.
- [x] Implement event-delegated disclosure JavaScript and scoped CSS without inline styles or `!important`.
- [x] Run JavaScript syntax and focused layout tests.

### Task 5: Verification and development deployment

- [x] Run focused tests and the full unit-test suite.
- [x] Run JavaScript syntax checking, `git diff --check`, JspC, and two clean builds.
- [x] Compare WAR hashes and verify the WAR allowlist.
- [x] Back up and deploy only the development application if all gates pass.
- [x] Smoke-test development GET/static/redirect behavior after an explicitly approved deployment.
- [x] Review the final diff.
