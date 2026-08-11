# Maintenance Chart Series Colors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Distinguish all three maintenance-history license chart series with Archive's existing blue, green, and amber colors plus a non-color cue for capacity.

**Architecture:** Add chart-specific semantic aliases in the shared token file, then consume those aliases in the existing Chart.js dataset configuration. Preserve the current data pipeline and chart options, and protect the result with the project's established source contracts plus browser verification.

**Tech Stack:** JSP, CSS custom properties, Chart.js 4.4.4, JavaScript, JUnit 5, Gradle, Tomcat 10

## Global Constraints

- Add no opaque palette colors, libraries, or CDN dependencies.
- Do not change Java, JSP data contracts, database behavior, or authentication.
- Deploy only to `/opt/tomcat-dev`; do not change `/opt/tomcat` or port 8080.
- Do not create commits, branches, pushes, or pull requests.

---

### Task 1: Protect the chart-series contract

**Files:**
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- Modify: `src/test/java/com/company/layout/MinimalPaletteContractTest.java`

**Interfaces:**
- Consumes: `tokens.css` semantic token declarations and `maintenance_history.js` Chart.js dataset configuration.
- Produces: a contract requiring blue usage, green used capacity, amber total capacity, matching legend/point colors, and a dashed capacity line.

- [x] Add assertions for `--color-chart-usage`, `--color-chart-used`, and `--color-chart-capacity` aliases.
- [x] Add assertions that each dataset consumes its semantic color for line, fill/legend, and point styling, and that capacity has `borderDash` and a distinct `pointStyle`.
- [x] Run `./gradlew test --tests com.company.layout.MaintenanceFormAssetContractTest --tests com.company.layout.MinimalPaletteContractTest --offline` and confirm failure because the semantic chart colors do not exist.

### Task 2: Apply the approved colors

**Files:**
- Modify: `src/main/webapp/resources/css/tokens.css`
- Modify: `src/main/webapp/resources/js/pages/maintenance_history.js`
- Modify: `src/main/webapp/WEB-INF/web.xml`

**Interfaces:**
- Consumes: existing `--palette-brand`, `--palette-success`, and `--palette-warning` values.
- Produces: `chartColors.usage`, `chartColors.used`, and `chartColors.capacity` for the three Chart.js datasets.

- [x] Alias the three chart tokens to the existing blue, green, and amber palette entries.
- [x] Resolve the aliases through the existing `cssColor()` helper.
- [x] Apply each resolved color consistently to `borderColor`, `backgroundColor`, `pointBackgroundColor`, and `pointBorderColor`.
- [x] Give the capacity series a dashed line and distinct point shape.
- [x] Change the asset cache version to `20260810-maintenance-chart-colors-1`.
- [x] Run the focused tests and JavaScript syntax check until green.

### Task 3: Verify and deploy development only

**Files:**
- Verify: `build/libs/frog2.war`
- Back up: `/opt/tomcat-dev/webapps/frog2.war`, exploded app, and JSP work directory

**Interfaces:**
- Consumes: the generated WAR.
- Produces: a verified development-only deployment with a recoverable rollback copy.

- [x] Run `./gradlew clean test check war --offline` twice.
- [x] Run JspC, WAR allowlist validation, and `git diff --check`.
- [x] Record production PID, WAR hash, and port 8080 response.
- [x] Back up and deploy only to `tomcat-dev.service`.
- [x] Verify the authenticated chart visually without submitting any write request.
- [x] Recheck Tomcat logs and production invariants.
