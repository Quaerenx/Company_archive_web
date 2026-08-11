# JspC Runtime Alignment Design

## Goal

Align the default Gradle JspC verification environment with the development
runtime, Apache Tomcat 10.1.57, without reading from or changing the production
Tomcat installation.

## Current state

- `tomcat-dev.service` uses `CATALINA_HOME=/opt/tomcat-dev-home/current`.
- `/opt/tomcat-dev-home/current` resolves to Apache Tomcat 10.1.57.
- Gradle JspC still defaults to `/opt/tomcat` and Jasper 10.1.41.
- An explicit `frog2JspcCatalinaHome` Gradle property already supports portable
  overrides.

## Chosen approach

Use `/opt/tomcat-dev-home/current` and Jasper `10.1.57` as the build defaults.
Keep the explicit Gradle properties as the only override mechanism, and do not
inherit a generic `CATALINA_HOME` environment variable because it may point to
the production 10.1.41 installation.

This is smaller and safer than introducing a second JspC tool configuration.
It also fails loudly when the selected Jasper version is not 10.1.57.

## Visual verification

Use the existing Firefox/WebDriver visual-regression tool at 360, 390, 768,
1024, and 1440 pixels. Authentication may come only from an existing
authenticated Firefox profile or secret environment variables already supplied
outside commands and source files. Credentials and session cookies must never
be written to the repository, command line, logs, screenshots names, or report.

The maintenance-history check covers table width, document overflow, the
table's own horizontal scrolling on narrow screens, long notes, and browser
console errors. If no safe authentication source is available, the authenticated
capture is reported as blocked rather than adding an authentication bypass.

## Git integration

After fresh full verification, keep the maintenance-history redesign and JspC
alignment in reviewable commits and push `develop` normally to
`origin/develop`. Do not force-push and do not create a pull request.

## Safety

- Do not modify or restart `/opt/tomcat`, `tomcat.service`, or port 8080.
- Do not execute DB DDL or DML.
- Do not expose credentials, cookies, CSRF tokens, or DB configuration.
- Preserve all existing working-tree changes.
