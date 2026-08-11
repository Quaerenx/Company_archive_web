# Maintenance History Three-Stage Design

## Goal

Improve the readability of always-expanded maintenance records by presenting each record in a stable date, facts, and note sequence.

## Information Hierarchy

1. The first row shows the inspection date as the primary anchor and the inspector plus registration time as secondary metadata.
2. The second row groups Vertica version and license information as a compact semantic description list without stretching values to opposite edges of the record.
3. The final row shows the full-width `점검 내용 및 비고` block with a quiet label and preserved line breaks.

Records remain fully expanded and the entire record remains the existing edit link. Missing values continue to render as `-`. No accordion, JavaScript behavior, data transformation, or backend change is introduced.

## Visual Rules

- Use existing Archive typography, surface, border, spacing, and focus tokens only.
- Remove the empty action row and the decorative note icon.
- Keep record separators and hover/focus behavior.
- Let note content use the full record width, preserve whitespace, and wrap long technical strings.
- On narrow screens, align inspector metadata left and stack fact groups without horizontal overflow.

## Safety and Verification

- Preserve URL, record ID, edit navigation, output encoding, and all displayed values.
- Change only the maintenance-history JSP, page CSS, view contracts, and cache version.
- Run focused tests red then green, JavaScript syntax, two clean builds, JspC, authenticated desktop/narrow visual checks, and production invariants.
- Deploy only to the development Tomcat after backup.
- Do not create commits, branches, pushes, or pull requests.
