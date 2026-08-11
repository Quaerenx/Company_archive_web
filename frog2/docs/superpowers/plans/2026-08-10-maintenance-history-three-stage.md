# Maintenance History Three-Stage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render each maintenance record as an always-expanded date, compact facts, and full-width note sequence.

**Architecture:** Preserve the existing clickable record and server data contract while replacing the loose detail divs with a semantic description list and a dedicated note-content element. Scope all presentation changes to `maintenance_history.css` and protect the structure with the existing JUnit view contract style.

**Tech Stack:** JSP/JSTL, CSS Grid/Flexbox, JUnit 5, Gradle, Tomcat 10, Firefox WebDriver

## Global Constraints

- Preserve the existing edit URL, output encoding, values, permissions, and always-expanded behavior.
- Add no JavaScript, library, dependency, color literal, or database operation.
- Deploy only to `/opt/tomcat-dev`; do not change `/opt/tomcat` or port 8080.
- Do not create commits, branches, pushes, or pull requests.

---

### Task 1: Protect the three-stage record contract

**Files:**
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- Modify: `src/test/java/com/company/layout/PageShellContractTest.java`

**Interfaces:**
- Consumes: `maintenance_history.jsp`, `maintenance_history.css`, and the global asset version.
- Produces: a contract for ordered header/facts/note stages, semantic `dl` markup, full-width notes, and no empty action row.

- [x] Add a focused test that requires `history-meta`, `history-facts`, and `history-note` in that source order.
- [x] Require `dl`/`dt`/`dd`, `note-content`, preserved `<c:out>`, and absence of the legacy detail and empty action classes.
- [x] Require compact facts, full-width wrapping notes, and narrow-screen stacking in the page CSS.
- [x] Change the expected asset version to `20260810-maintenance-history-hierarchy-1`.
- [x] Run the focused tests and confirm failure because the legacy record structure is still present.

### Task 2: Implement the three-stage hierarchy

**Files:**
- Modify: `src/main/webapp/maintenance/maintenance_history.jsp`
- Modify: `src/main/webapp/resources/css/pages/maintenance_history.css`
- Modify: `src/main/webapp/WEB-INF/web.xml`

**Interfaces:**
- Consumes: existing record fields `inspectionDate`, `inspectorName`, `createdAt`, `verticaVersion`, `licenseSummary`, and `note`.
- Produces: `.history-meta`, `.history-facts`, `.history-fact`, `.history-note`, and `.note-content` without changing navigation.

- [x] Replace legacy `.history-details` groups with a two-item semantic `.history-facts` description list.
- [x] Split the note label and escaped note value into dedicated elements, remove the decorative icon, and remove `.history-actions`.
- [x] Replace legacy detail CSS with compact fact-grid rules and a full-width wrapping note.
- [x] Keep mobile inspector alignment and stack the facts at the existing 768px breakpoint.
- [x] Set the global cache version to `20260810-maintenance-history-hierarchy-1`.
- [x] Run focused tests and `node --check resources/js/pages/maintenance_history.js` until green.

### Task 3: Verify and deploy development only

**Files:**
- Verify: `build/libs/frog2.war`
- Back up: development WAR, exploded app, and JSP work directory

**Interfaces:**
- Consumes: generated WAR.
- Produces: a recoverable development-only deployment and visual evidence.

- [x] Run `./gradlew clean test check war --offline` twice.
- [x] Run JspC, WAR-content validation, and `git diff --check`.
- [x] Record production PID, WAR hash, and port 8080 response.
- [x] Back up and deploy only to `tomcat-dev.service`.
- [x] Verify an authenticated record list at desktop and narrow width without write requests.
- [x] Recheck Tomcat logs and production invariants.
