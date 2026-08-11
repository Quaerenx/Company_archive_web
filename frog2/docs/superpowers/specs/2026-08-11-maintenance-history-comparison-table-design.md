# Maintenance History Comparison Table Design

## Goal

Replace the always-expanded maintenance-history cards with a dense comparison table that makes monthly Vertica version and license changes easy to scan without hiding or truncating inspection notes.

## Approved Direction

Use the approved **A comparison table with full-width note rows**.

Each inspection record is one semantic row group containing:

1. A metric row for comparison.
2. A full-width note row that grows with the complete note text.

Records remain ordered by inspection date descending. No records are merged, collapsed, or aggregated, even when more than one inspection exists in the same calendar month.

## Information Structure

The table header contains these columns:

1. `점검월`
2. `버전`
3. `사용량`
4. `전체 용량`
5. `사용률`
6. `이전 대비`
7. `점검자`

The first line of each record aligns all numeric values to fixed columns. Numeric columns use tabular numerals and right alignment.

The month cell spans both lines and shows:

- `yyyy.MM` as the primary value.
- The exact inspection day as secondary text.
- A clear link to the existing edit URL.

The inspector cell shows the inspector name and the existing registration timestamp as secondary text so the redesign does not remove currently visible information.

The second line contains `점검 메모` and the complete note. It does not use line clamping, ellipsis, disclosure controls, or collapsed content. Newlines are preserved.

## Comparison Values

License fields are normalized server-side from the existing record values. The view receives separate values for:

- Used terabytes.
- Total capacity terabytes.
- Rounded usage percentage.
- A bounded `0..100` percentage used only by the decorative ring.
- Difference from the next older record on the current page.

The column label is `이전 대비`, not `전월 대비`, because records can be missing for a month or multiple inspections can exist in one month. If either percentage is unavailable, or the older comparison record is not loaded at a pagination boundary, the value is `—`.

Positive, negative, and unchanged differences use arrow/text symbols in addition to color:

- `↑ 2%p`
- `↓ 1%p`
- `— 0%p`

The small usage ring remains decorative with `aria-hidden="true"`; the adjacent percentage text is always the source of meaning.

## Markup and Accessibility

Use a native `<table>` with:

- A descriptive `<caption>` available to assistive technology.
- `scope="col"` for column headers.
- One `<tbody>` per inspection record.
- A month `<th scope="rowgroup" rowspan="2">` that associates both lines with the same inspection.
- A metric `<tr>` followed by a note `<tr>`.
- The note cell spanning the six non-month columns.

Do not make `<tr>` or `<tbody>` behave as invalid links. The visible month/date link opens the existing edit URL and receives a clear focus-visible style and accessible name.

Missing values render as `—`. All record text continues to use escaped JSP output.

## Responsive Behavior

Desktop and tablet retain the comparison table.

At narrow widths:

- The table stays semantically intact inside a horizontally scrollable wrapper.
- A short scroll hint is visible.
- The minimum table width preserves readable metric columns.
- The note row uses the same table width and wraps naturally.
- The document itself must not gain horizontal overflow.

No JavaScript is required for responsive layout or note visibility.

## Data and Code Boundaries

Create a dedicated server-side maintenance-history row view model rather than adding more presentation-only fields to the DAO DTO. It wraps the existing `MaintenanceRecordDTO` and exposes normalized display values and comparison values.

The controller builds the row views from the already loaded paginated records. It does not issue another query and does not modify the records or database.

The existing chart, pagination, URL, authorization, edit link, DAO ordering, form parameters, and raw history data remain unchanged.

The current transitional `licenseSummary` and `licenseUsageProgressPct` presentation fields may be removed from `MaintenanceRecordDTO` only after all history JSP and tests use the dedicated view model and static search confirms no remaining consumer.

## CSS Direction

- Scope every new selector below `.maintenance-history`.
- Use the existing Archive tokens only.
- Keep metric rows white and notes on a quiet muted surface.
- Use a single stronger border between record groups and lighter internal separation.
- Do not add cards, shadows, gradients, new colors, `!important`, or a new breakpoint.
- Row hover may highlight both lines, but the month/date link remains the only navigation control.

## Error and Edge Cases

- Null inspection date: display `—` and keep the record accessible.
- Missing version or license value: display `—` in its column.
- Missing note: display `점검 메모` with `기록 없음` rather than removing the row, so every record keeps the same structure.
- Long notes: wrap fully and increase only that record group's height.
- Multiple records in one month: render separate groups in stable DAO order.
- Page boundary: show no comparison for the oldest visible record when an older record is not loaded.

## Verification

1. Add row-view unit tests for normalization, comparison direction, missing values, duplicate months, and page-boundary behavior.
2. Add JSP contract tests for caption, seven column headers, row-group header, two-row structure, complete note output, and escaped values.
3. Update the existing history layout contract tests that currently require `.history-item` cards.
4. Verify 360, 768, 1024, and 1440px behavior, including document overflow and table-wrapper scrolling.
5. Run the full unit-test suite, Java 22 JspC, clean build, WAR allowlist, and `git diff --check`.
6. Deploy only to the development Tomcat after timestamped WAR, exploded-app, and JSP-work backups.
7. Confirm development GET/static/redirect behavior and confirm production PID, WAR hash, and port 8080 response are unchanged.

## Out of Scope

- Database schema or data changes.
- New SQL queries or index changes.
- Note editing in place.
- Collapsing notes.
- Sorting or filtering controls.
- Changes to the chart or customer header.
- Operating-system, Tomcat, or production configuration changes.
