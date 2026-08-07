# frog2 minimal dashboard pilot report

Date: 2026-08-01 KST

Status: historical dashboard pilot. The approved global Light-only rollout is
documented in `frog2-minimal-light-global-20260801.md`.

## Scope

- Existing dashboard data, card count, section order, URLs, forms, authentication,
  authorization, CSRF, redirect, and database access were not changed.
- The pilot is activated only by `body.dashboard-page.minimal-tone-pilot`.
- No DB DDL/DML, authenticated POST, production restart, commit, branch, push, or PR
  was performed.

## Adopted and excluded reference principles

Adopted:

- Linear: quiet navigation and stronger content hierarchy.
- Attio: compact operational information without changing the data model.
- Geist/Primer: semantic tokens, flat surfaces, consistent focus and controls.
- Atlassian/Carbon: neutral-first color use and status colors only for real status.

Excluded:

- Brand-specific layouts, logos, icons, and trade dress.
- Inline editing, new workflows, new data, new APIs, external UI frameworks,
  external fonts, gradients, glass effects, hover lift, and decorative animation.

## Pilot palette

Each theme has exactly 17 opaque candidate primitives.

| Role | Light | Dark |
| --- | --- | --- |
| canvas | `#F7F8FA` | `#0F141A` |
| surface | `#FFFFFF` | `#161C24` |
| surface-muted | `#F2F4F7` | `#1D2631` |
| border | `#DDE2E8` | `#364452` |
| border-strong | `#8491A3` | `#536171` |
| text-strong | `#182230` | `#F4F7FA` |
| text | `#344054` | `#DCE3EA` |
| text-muted | `#667085` | `#AAB4C0` |
| brand-subtle | `#EDF3F7` | `#203244` |
| brand | `#365C7D` | `#83B4D8` |
| brand-hover | `#294B68` | `#A0C9E5` |
| success-subtle | `#EDF8F2` | `#12291F` |
| success | `#16794A` | `#55C98B` |
| warning-subtle | `#FFF8E6` | `#2B2312` |
| warning | `#8A5A00` | `#F4C85E` |
| danger-subtle | `#FFF1F2` | `#2D181A` |
| danger | `#C9363E` | `#FF7B83` |

Semantic mappings:

- link, info, focus: brand
- secondary: text or text-muted
- disabled: text-muted plus opacity
- divider: border
- info background: brand-subtle
- overlay and allowed dropdown/modal shadows: alpha derived from a palette primitive
- dark control border: text-muted, because the fixed dark border primitives do not
  reach the required 3:1 control-boundary contrast on surface

## Visual and cascade changes

- Normal cards use surface, 1px border, 8px radius, and no shadow.
- KPI attention/risk cards remain neutral and use a labelled 2px indicator.
- Hover lift was removed; hover uses only background or border changes.
- Loading/error panels remain neutral with a labelled 2px brand/danger indicator.
- Normal typography is 400/500/600; only KPI values remain 700.
- Decorative month and VM icons use muted text color.
- Header and main content use the same 1018px max-width and gutter at all viewports.
- A solid 2px brand outline supplements the alpha focus halo.
- Form controls use a scoped stronger boundary; invalid controls keep the danger
  boundary. This explicitly resolves the higher-specificity shared form rule.
- The dashboard scope remaps legacy header aliases, overlay, and allowed shadow
  variables so shared CSS continues to load in the existing order.

## Quantitative result

| Metric | Before | Pilot effective result |
| --- | ---: | ---: |
| Light opaque colors observed in baseline CSS | 46 | 17 candidate primitives |
| Dark opaque colors observed in baseline CSS | 49 | 17 candidate primitives |
| Normal elevated surface shadows | 5 | 0 |
| Whole-card KPI status tints | 2 | 0 |
| Effective 800/900 emphasis selectors | 3 | 0 |
| Effective 700 emphasis | multiple | KPI value only |
| Dashboard color literals outside tokens | 0 | 0 |
| Dashboard `!important` | 0 | 0 |
| Dashboard `transition: all` | 0 | 0 |
| Undefined loaded custom properties | 0 | 0 |

## Verification

- `./gradlew clean test check war`: two consecutive successes.
- Java tests: 289, failures 0, errors 0, skipped 0 on both final builds.
- Reproducible WAR SHA-256 on both builds:
  `d3ebe04aa2b62f864786bed988ac74951e9c2d92bdf4fda14a55b485446fcb4c`.
- JspC: 43 JSP/JSPF/tag inputs, 36 generated Java files, 61 classes,
  0 errors.
- JavaScript: all 23 source files passed `node --check`.
- `git diff --check`: passed.
- WAR content verification: passed; no source/Javadoc/test JAR, build directory,
  or `build/classes/db.properties` match.
- Browser: exact CSS viewports 360, 390, 768, 1024, and 1440px in light and
  dark themes; horizontal overflow 0 and header/main width difference 0.
- Browser console errors: 0. Mobile menu open/Escape/focus return and modal
  open/Escape/focus return passed. Reduced-motion matched.
- Minimum measured light contrast: muted text/canvas 4.682:1,
  control/surface 3.201:1, focus/surface 7.038:1.
- Minimum measured dark contrast: danger/surface 6.864:1,
  control/surface 8.158:1, focus/surface 7.739:1.

Browser report:

- `/root/frog2-minimal-pilot-validation-20260801/pilot-browser-report.json`
- SHA-256: `6712565222629d737aa40efaa1038e8c351821fb4305cfa56718c819d46763b3`

Screenshot directories:

- Before: `/root/frog2-minimal-pilot-baseline-20260801`
- After: `/root/frog2-minimal-pilot-validation-20260801`

The old `before-dashboard-360.png` and `before-dashboard-390.png` are actually
500px wide because of the previous Firefox minimum window size. The final pilot
uses WebDriver BiDi and records true 360px and 390px CSS viewports.

## Development deployment and production invariant

- Development URL: `http://192.168.40.70:18081/frog2/`
- Development WAR changed from
  `9cd4fdb96cdec43bf51be4d65223338a5c07d81051b40b54789feec0a6d79506`
  to
  `d3ebe04aa2b62f864786bed988ac74951e9c2d92bdf4fda14a55b485446fcb4c`.
- `tomcat-dev.service` only was restarted; new PID is 34042.
- Development login GET: 200. Dashboard without authentication: 302 to login.
- Deployed tokens and dashboard CSS: 200. Startup error pattern matches: 0.
- Production PID stayed 1012286.
- Production WAR stayed
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`.
- Production login GET stayed 200 with 8697 bytes; dashboard stayed 302 to login.

## Backup and rollback

Verified pre-deployment backup:

- `/opt/frog2-dev/backups/minimal-dashboard-pilot-20260801-015155`
- 516 backed-up files plus `SHA256SUMS` (verified copies and the moved live originals)
- Manifest SHA-256:
  `f0ef5ab49b8aceb22a1e58485b37b1a3688b1f4c3c4fe28d515662e9fc3e3073`

Rollback affects development Tomcat only:

```bash
systemctl stop tomcat-dev.service
mv /opt/tomcat-dev/webapps/frog2.war \
  /opt/frog2-dev/backups/minimal-dashboard-pilot-20260801-015155/failed-pilot-frog2.war
mv /opt/tomcat-dev/webapps/frog2 \
  /opt/frog2-dev/backups/minimal-dashboard-pilot-20260801-015155/failed-pilot-exploded-frog2
mv /opt/tomcat-dev/work/Catalina/localhost/frog2 \
  /opt/frog2-dev/backups/minimal-dashboard-pilot-20260801-015155/failed-pilot-jsp-work-frog2
mv /opt/frog2-dev/backups/minimal-dashboard-pilot-20260801-015155/frog2.war-live \
  /opt/tomcat-dev/webapps/frog2.war
mv /opt/frog2-dev/backups/minimal-dashboard-pilot-20260801-015155/exploded-frog2-live \
  /opt/tomcat-dev/webapps/frog2
mv /opt/frog2-dev/backups/minimal-dashboard-pilot-20260801-015155/jsp-work-frog2-live \
  /opt/tomcat-dev/work/Catalina/localhost/frog2
systemctl start tomcat-dev.service
```

## Remaining risk and approval gate

- The authenticated live dashboard was not opened because authenticated POST and
  DB-affecting requests were forbidden. JSP compilation, static assets, a
  representative DOM fixture matching the current dashboard structure, and
  unauthenticated live GET paths were verified instead.
- Existing non-pilot pages retain the legacy palette by design.
- Global rollout requires explicit user approval and domain-by-domain regression
  verification.

대시보드 pilot 완료, 전역 확장 사용자 승인 대기
