# Login Form Usability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Suppress stored username suggestions while improving field recognition, login-error association, visual balance, and mobile viewport stability without changing authentication behavior.

**Architecture:** Keep the existing JSP login POST contract and shared UI system. Make semantic HTML attribute changes in `login.jsp`, scope presentation changes to `.login-page` in `login_style.css`, and protect the user-visible contract with the existing login view and development smoke tests.

**Tech Stack:** JSP/JSTL, CSS, JUnit 5, Gradle, Tomcat 10

## Global Constraints

- Do not change the login action, method, parameter names, CSRF input, session handling, authentication backend, or password autocomplete hint.
- Do not add JavaScript or external dependencies.
- Do not change production Tomcat, production WAR, production configuration, or the shared database.
- Deploy only to the development Tomcat after preserving timestamped WAR and exploded-app backups.
- Do not create commits, branches, pushes, or pull requests.

---

### Task 1: Lock the login interaction contract

**Files:**
- Modify: `src/test/java/com/company/layout/LoginViewContractTest.java`
- Modify: `src/test/java/com/company/e2e/DevelopmentServerSmokeTest.java`

**Interfaces:**
- Consumes: rendered `login.jsp` markup and the development `/frog2/login` response
- Produces: regression checks for username suggestion suppression, persistent in-field labels, linked errors, balanced logo sizing, and dynamic viewport height

- [x] **Step 1: Write failing contract assertions**

Update `LoginViewContractTest` to require `autocomplete="off"` on the login form and username field, retain `autocomplete="current-password"` on the password field, require the inputs to precede their floating labels, require conditional `aria-describedby` links to `login-error`, and require the login CSS to use a `216px` logo cap, `24px` header gap, and `100dvh` fallback.

- [x] **Step 2: Run the focused test and verify RED**

Run: `./gradlew test --tests com.company.layout.LoginViewContractTest`

Expected: FAIL because the current form enables autocomplete, labels precede inputs, errors are not directly associated, the logo cap is `240px`, and `100dvh` is absent.

### Task 2: Implement the minimal JSP and CSS change

**Files:**
- Modify: `src/main/webapp/login.jsp`
- Modify: `src/main/webapp/resources/css/login_style.css`

**Interfaces:**
- Consumes: existing `userId`, `password`, `errorMessage`, CSRF include, and shared UI tokens
- Produces: the same login POST payload with quieter browser suggestions and persistent input identification

- [x] **Step 1: Update the login markup**

Set the form and username input to `autocomplete="off"`; retain the password's `autocomplete="current-password"`. Put each input before its label, use a single-space placeholder so the label acts as the in-field prompt, and conditionally link both fields to `login-error` through `aria-describedby`.

- [x] **Step 2: Update only login-scoped CSS**

Use relative field wrappers and floating labels that move on focus, non-empty values, or browser autofill. Reduce the logo cap to `216px`, the header gap to `24px`, retain the `100vh` fallback and add `100dvh`, and preserve reduced-motion behavior.

- [x] **Step 3: Run the focused tests and verify GREEN**

Run: `./gradlew test --tests com.company.layout.LoginViewContractTest`

Expected: PASS.

### Task 3: Verify, package, and deploy to development only

**Files:**
- Verify: all changed files and generated WAR contents
- Deploy: `/opt/tomcat-dev/webapps/frog2.war`

**Interfaces:**
- Consumes: tested source tree and current development runtime
- Produces: a timestamped development rollback backup and verified login HTML on port `18081`

- [x] **Step 1: Run full verification twice**

Run the project clean test/build/check/WAR workflow twice, followed by JspC and `git diff --check`.

- [x] **Step 2: Preserve development rollback files**

Back up the current development WAR and exploded app under `/opt/frog2-dev/backups/login-form-<timestamp>/` without touching `/opt/tomcat`.

- [x] **Step 3: Deploy and inspect the rendered login page**

Restart only the development Tomcat, verify `/frog2/login` and static assets, assert the rendered form and username field have `autocomplete="off"`, and run the development smoke tests.

- [x] **Step 4: Confirm production invariants and review the diff**

Confirm the production PID, WAR hash, and port `8080` response are unchanged; inspect the diff for unrelated changes and report remaining browser-extension limitations.
