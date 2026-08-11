# JspC Runtime Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Align JspC verification with development Tomcat 10.1.57, verify the authenticated maintenance-history UI when a safe session is available, and publish the verified Git baseline.

**Architecture:** Keep the existing Jasper source-generation plus Java 22 `javac` pipeline. Change only its safe defaults and build contract, then reuse the existing WebDriver capture tool and normal Git workflow.

**Tech Stack:** Gradle, Java 22, Apache Tomcat/Jasper 10.1.57, JUnit 5, Firefox WebDriver, Git.

## Global Constraints

- Do not change or restart production Tomcat, its WAR, or port 8080.
- Do not execute DB DDL or DML.
- Do not persist or print authentication secrets.
- Do not force-push or create a pull request.

---

### Task 1: Align the JspC build contract

**Files:**
- Modify: `src/test/java/com/company/buildcontract/JspcJava22BuildContractTest.java`
- Modify: `gradle/jspc-java22.gradle`

**Interfaces:**
- Consumes: `frog2JspcCatalinaHome` and `frog2JspcJasperVersion` Gradle properties.
- Produces: `jspcJava22` using `/opt/tomcat-dev-home/current` and Jasper 10.1.57 by default.

- [ ] **Step 1: Change the contract test first.**
  Require the development-only default home, version 10.1.57, and no generic
  `CATALINA_HOME` environment fallback.
- [ ] **Step 2: Run the targeted test and verify RED.**
  Run: `./gradlew test --tests com.company.buildcontract.JspcJava22BuildContractTest`
  Expected: FAIL because the build script still references 10.1.41 and `/opt/tomcat`.
- [ ] **Step 3: Apply the minimal build-script change.**
  Preserve explicit Gradle-property overrides and the existing manifest version
  check.
- [ ] **Step 4: Run the targeted test and JspC and verify GREEN.**
  Run: `./gradlew test --tests com.company.buildcontract.JspcJava22BuildContractTest jspcJava22`
  Expected: PASS; log identifies Jasper 10.1.57 under the development home.

### Task 2: Run authenticated visual verification safely

**Files:**
- Reuse: `src/tools/visual-regression.sh`
- Reuse: `src/tools/capture-visual-regression.mjs`
- Reuse: `src/tools/visual-regression-routes.tsv`

**Interfaces:**
- Consumes: an existing authenticated Firefox profile or pre-existing secret
  environment variables.
- Produces: screenshots and `capture-metrics.json` outside Git.

- [ ] **Step 1: Check for an authenticated profile without reading cookie values.**
- [ ] **Step 2: Capture the maintenance-history route at all five viewports.**
- [ ] **Step 3: Verify document `scrollWidth`, console errors, long-note rendering,
  and narrow-screen table scrolling.**
- [ ] **Step 4: If no safe auth source exists, report the capture as blocked and
  do not add an auth bypass or place credentials on the command line.**

### Task 3: Verify, commit, and push

**Files:**
- Review all maintenance-history, test, build, and plan/spec changes shown by Git.

**Interfaces:**
- Consumes: the fully verified working tree.
- Produces: reviewable commits on `develop`, normally pushed to `origin/develop`.

- [ ] **Step 1: Run full verification.**
  Run clean build twice, forced full tests, JspC, WAR allowlist, JavaScript
  syntax checks, and `git diff --check`.
- [ ] **Step 2: Verify development and production runtime invariants.**
  Confirm development health and that production PID, WAR hash, and login GET
  are unchanged.
- [ ] **Step 3: Review staged patches by concern and commit.**
  Keep the maintenance-history UI and JspC alignment reviewable.
- [ ] **Step 4: Push normally.**
  Run `git push origin develop`; do not use force and do not open a PR.
