# Java 25 runtime transition

Date: 2026-09-04
Status: build/CI verification implemented; Tomcat runtime rollout not executed

The project toolchain, tests and JspC run on Java 25 with Gradle 9.1.0. Main and
precompiled JSP classes currently use `--release 22`, preserving the existing
Java 22 runtime as a rollback target while Java 25 is introduced.

## Completed compatibility gate

- Compile production and test sources with Temurin 25.
- Generate and compile every JSP with Tomcat 10.1.59 on Java 25.
- Run the complete test suite and WAR allowlist checks on Java 25.
- Keep Java 22-compatible bytecode until both Tomcat environments pass runtime
  smoke and log checks.

## Separately approved runtime rollout

1. Install a checksum-verified Java 25 LTS distribution under a versioned
   `/opt` directory without changing either service.
2. Point only `tomcat-dev.service` at the versioned JDK, restart development,
   and verify login, authenticated navigation, readiness, UTF-8 form round trips,
   JSP loading and logs.
3. Observe development for the agreed period. Restore the former `JAVA_HOME`
   immediately on linkage, driver, locale, encoding or startup regressions.
4. Repeat the same backup/change/smoke process for `tomcat.service` in a
   production maintenance window.
5. Remove the `--release 22` compatibility target only in a later release after
   the Java 22 rollback window is formally closed.

Changing the Gradle toolchain does not authorize changing a running service.
