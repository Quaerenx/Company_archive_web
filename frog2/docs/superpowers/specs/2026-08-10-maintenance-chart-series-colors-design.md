# Maintenance Chart Series Colors Design

## Goal

Make the three maintenance-history license series immediately distinguishable while preserving Archive's restrained light palette and all existing chart data and behavior.

## Design

- `사용률(%)` uses the existing Archive ink-blue brand color.
- `라이선스 사용량(TB)` uses the existing success green.
- `라이선스 크기(TB)` uses the existing warning amber.
- Each dataset uses the same semantic color for its legend swatch, line, and points.
- Capacity also uses a dashed line and a distinct point shape so the chart does not rely on color alone.
- Grid, axes, tooltip, layout, data extraction, labels, units, and dual-axis behavior remain unchanged.
- Chart-specific semantic tokens alias existing palette values; no opaque color is added to the approved 18-color palette.

## Scope and Safety

- Modify only chart tokens, the maintenance-history chart configuration, its contract tests, and the asset cache version.
- Do not change JSP data, Java, authentication, authorization, or database behavior.
- Deploy only to the development Tomcat after a timestamped backup.
- Do not create commits, branches, pushes, or pull requests.

## Verification

- Focused chart and palette contracts pass after first failing against the old neutral series.
- JavaScript syntax, two consecutive clean builds, WAR validation, JspC, and `git diff --check` pass.
- An authenticated development GET confirms the three colors, legend/line/point consistency, and dashed capacity series.
- Production PID, WAR hash, and port 8080 response remain unchanged.
