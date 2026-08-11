# Archive Domain Functionality Remediation Design

## Goal

Preserve every existing URL and form parameter while bounding maintenance history,
reducing customer-detail database round trips, and locking the already-correct domain
contracts with focused tests. Shared database data and schema are never changed.

## Scope and decisions

### Customer detail

The detail page keeps its existing `customer`, `customerDetail`,
`customerDetailStg`, `customerDetailDev`, and `verticaEosDate` request attributes.
The three environment rows are loaded with one bounded `UNION ALL` statement instead
of three sequential statements. The active production row is also used to construct
the existing `CustomerDTO`, removing the separate customer-summary query. A missing
or inactive production row remains indistinguishable from a missing customer, as in
the current controller contract. EOS lookup remains separate because it is a distinct
catalog with legacy schema compatibility logic.

### Maintenance history

`GET /maintenance?view=history&customerName=...` remains valid without new
parameters and returns the newest 20 records. Optional `historyPage` selects older
pages. The query uses `inspection_date DESC, maintenance_id DESC`, so equal dates are
stable. Invalid non-numeric, zero, or negative page values return HTTP 400; an
out-of-range positive page is clamped to the last available page. The chart and list
receive the same page items. The existing license formatter writes directly to each
record's `licenseSummary`, fixing the current controller/JSP mismatch.

### Maintenance schedule

`MaintenanceSchedule` remains the single resolver used by customer assignments,
the dashboard, and maintenance views. Tests cover January/December transitions,
leap-year February, 3/6/9/12 quarterly residue, customer-specific anchors, disabled
schedules, missing schedule fallback, inactive-customer filtering, and the approved
Konkuk University Hospital migration override. No migration is executed or edited.

### Other domains

Meeting/comment cursor pagination, troubleshooting search/pagination, my-page and
personal-host stable ownership, and file repository cursor/safety contracts already
match the audit. They receive missing boundary tests only. Production code is changed
only if a new test exposes a real defect.

### Deprecation

`main_style.css`, legacy button aliases, legacy migrations, JSPs, and servlet routes
are classified as: immediate removal, access-log evidence required, or compatibility
required. Directly addressable resources and routes are not deleted from static search
alone. No legacy migration is deleted. A production asset is removed only after clean
build, JspC, JavaScript checks, and runtime GET smoke checks.

## Error and compatibility contract

- Existing URL paths, view names, form `action` values, field names, redirects, and
  session flash keys remain unchanged.
- The new optional history parameter is `historyPage`; absence means page 1.
- Invalid `historyPage` returns HTTP 400 with code `invalid_history_page`.
- Authentication, CSRF, stable owner IDs, and read-only JDBC behavior are unchanged.
- No GET route performs DML or DDL.

## Verification design

Each behavior change follows RED → GREEN in the isolated copy. Targeted tests run
before broader tests. Final verification consists of two consecutive offline clean
builds, JspC, all unit/contract tests, JavaScript syntax checks, `git diff --check`,
WAR allowlist checks, development GET-only smoke checks, and before/after production
PID/WAR hash/8080 response comparison. No authenticated write request or database
schema audit is run.

## Explicit non-goals

- No UI redesign, new framework, database migration, index, or query-plan execution.
- No role-policy change or new Java/HTTP public behavior outside optional history
  pagination.
- No commit, branch, push, PR, Tomcat restart, or deployment.

## Self-review

The design contains no unresolved placeholders. The maintenance list and chart use one
bounded result, customer-detail data retains all existing request attributes, and all
changes remain compatible with the stated safety constraints.
