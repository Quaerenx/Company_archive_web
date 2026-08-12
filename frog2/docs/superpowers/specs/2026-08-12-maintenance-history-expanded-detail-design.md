# Maintenance History Expanded Detail Design

## Goal

Reorganize the expanded maintenance-history row so users can scan the
license summary, read the inspection note, and identify registration metadata
without information being scattered across three narrow columns.

## Confirmed layout

The expanded row uses two hierarchy levels.

1. A full-width license summary at the top contains four metrics in one row:
   license usage, license usage percentage, previous usage percentage, and
   previous-period delta.
2. The lower area uses a 70/30 split. The inspection note occupies the wide
   left column. Registration metadata occupies the narrow right column.

The metadata section lists inspector, created time, and updated time as
vertical label/value rows. The edit action sits below those rows. Dates must
not wrap on desktop.

## Visual language

- Keep the existing Archive colors, typography, and outer expanded-row
  background.
- Do not add nested cards or shadows.
- Use one horizontal divider between the summary and lower area and one
  vertical divider before registration metadata.
- Give the note a quiet surface background so it reads as the primary content,
  while preserving all original line breaks.
- Keep labels muted and values stronger.

## Responsive behavior

- Desktop: four summary metrics in one row, then the 70/30 note/metadata split.
- At 1024px and below: summary metrics use two columns while the lower split is
  retained where space permits.
- At 768px and below: summary, note, and metadata stack in one column; the
  vertical divider becomes a horizontal divider.

## Compatibility and safety

- Keep the existing disclosure button, IDs, ARIA attributes, edit URL, record
  values, and server contracts unchanged.
- Change only the maintenance-history JSP structure, page CSS, asset version,
  and their layout-contract tests.
- Add no dependency and perform no DB write.
- Deploy only to the development Tomcat after the full verification gate.

## Acceptance criteria

- Automatic summary metrics are visually grouped across the full width.
- The note is the dominant lower section and no longer floats in the center.
- Registration metadata is a compact vertical list and timestamps stay on one
  line on desktop.
- The edit action is aligned with the metadata section.
- Mobile stacking preserves the order summary → note → registration info.
- Existing full note text, empty-note fallback, inspector, created time,
  updated time, and edit link remain available.
