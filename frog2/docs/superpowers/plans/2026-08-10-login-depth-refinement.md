# Login Depth Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the existing minimal Archive login screen a clearer visual hierarchy and depth without adding text, controls, dependencies, or authentication changes.

**Architecture:** Add login-only semantic color and depth tokens, consume them only from `login_style.css`, and retain the current JSP structure and shared ambient animation. Protect the result with source contracts, then verify the deployed development page in Firefox at desktop, mobile, and filled-input states.

**Tech Stack:** JSP/JSTL, CSS custom properties, JUnit 5, Gradle, Tomcat 10, Firefox WebDriver

## Global Constraints

- Preserve the logo, user ID field, password field, login button, POST parameters, CSRF, session, and authentication backend.
- Do not add visible text, icons, JavaScript, external fonts, libraries, or CDN assets.
- Keep all page-level colors token-based and scope new presentation rules to `.login-page`.
- Do not access or modify the database.
- Deploy only to the development Tomcat after a timestamped backup; do not change production.
- Do not create commits, branches, pushes, or pull requests.

---

### Task 1: Define and test the visual hierarchy contract

- [x] Add failing login view assertions for a 408px shell, viewport-contained halo, translucent card, 12px card radius, login shadow, 208px logo, subdued inputs, blue-graphite action, and mobile depth.
- [x] Add a failing asset-version assertion for `20260810-login-depth-2`.
- [x] Run focused tests and confirm they fail for the current flat login styles and the initial overflowing halo.

### Task 2: Implement the token-based refinement

- [x] Add login-only semantic tokens to `tokens.css` using the existing neutral and brand palette.
- [x] Update `login_style.css` without changing login markup or behavior.
- [x] Run login, shell, token, and design-principle tests until green.

### Task 3: Verify and deploy development only

- [x] Run `clean test check war --offline` twice, JspC, WAR-content validation, and `git diff --check`.
- [x] Back up the current development WAR, exploded app, and JSP work directory.
- [x] Deploy and restart only `tomcat-dev.service`.
- [x] Verify login GET, E2E smoke, 360px and 1440px screenshots, focused/filled controls, horizontal overflow, Tomcat errors, and production invariants.
