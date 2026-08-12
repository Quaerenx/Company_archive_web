# Maintenance History Compact Details Design

## Goal

Replace the always-expanded maintenance-history blocks with a compact comparison table and independently expandable details while preserving Archive's existing URLs, data, write behavior, and visual language.

## Stack adaptation

The supplied brief assumes React and TypeScript. Archive uses Java 22, Jakarta Servlet/JSP/JSTL, vanilla JavaScript, and scoped CSS. The same interaction contract is implemented with a servlet request model, a JSP table, a focused row view model, and progressive-enhancement JavaScript. No frontend library or endpoint is added.

## Data flow

- The existing `GET /maintenance?view=history&customerName=...` route remains the only history route.
- Optional `historyYear`, `historyVersion`, and `historyQuery` parameters filter the existing paginated SELECT.
- Filtering is read-only and uses fixed SQL fragments with prepared-statement parameters.
- `historyQuery` searches inspector, version, and note. Literal `%`, `_`, and the escape marker are escaped.
- Invalid years and overlong input return HTTP 400 instead of silently falling back.
- Pagination links retain active filters.
- The chart and table use the same filtered page, so visible data cannot disagree.

## Table and details

Each collapsed record is one 56-64px row with seven columns: inspection date, version, integrated license indicator, previous-month delta, inspector, one-line note summary, and detail toggle. Dates use `yyyy.MM.dd`. The license cell combines used/capacity TB, a native accessible progress element, and the percentage. Archive's existing 90% license-risk policy is reused; ordinary percentage-point changes remain neutral.

The summary is the first nonblank note line truncated to 64 Unicode code points, with `특이사항 없음` for a null or blank note. No status is inferred because the current model has no status field.

Every detail toggle is a real button with `aria-expanded` and `aria-controls`. All records start collapsed, and multiple records can be opened independently. A detail row preserves the complete note including line breaks and shows automatic license information, previous usage, delta, inspector, created time, updated time when present, and the existing edit link.

## Responsive and accessibility contract

- Desktop retains the full table and shows at least eight collapsed records in a typical viewport.
- At narrower widths, note summary and inspector columns are removed in that order; the table wrapper owns any remaining horizontal scroll.
- The document itself must not gain horizontal overflow.
- Progress information has a textual value and accessible label; risk is not communicated by color alone.
- Toggle buttons keep existing focus-visible and minimum target-size conventions.
- JavaScript controls disclosure state only. Server-rendered records and notes do not depend on chart code.

## Compatibility and safety

- The existing three-argument DAO method remains as a compatibility delegate to an empty filter.
- No DB schema, DDL, DML, API, form parameter, authentication, or authorization behavior changes.
- No status column or invented business field is introduced.
- No production dependency is added.
- Only the development deployment may be refreshed after verification; production Tomcat remains untouched.

## Verification

Focused tests cover filter validation and escaping, DAO count/list parity, servlet 400 behavior and parameter propagation, note summaries, partial license data, the 90% risk boundary, neutral delta, independent disclosure markup and script behavior, accessibility, and responsive CSS. Final checks are targeted tests, the full test suite, JavaScript syntax, JspC, two clean builds, WAR allowlist, development GET smoke, and a production read-only comparison if permitted.
