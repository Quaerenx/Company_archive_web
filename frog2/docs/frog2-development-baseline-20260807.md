# Frog2 development baseline — 2026-08-07

## Purpose

This records the current development source, tests, tools, deployable WAR, and
rollback anchors without creating a Git commit, branch, tag, push, or PR. It does
not change the database or the production runtime.

## Git working-tree classification

- Git root: `/opt/frog2-dev/repo`
- application root: `/opt/frog2-dev/repo/frog2`
- branch: `develop`
- HEAD: `d962a1ae44040bde38e20b4ae77cba03539a4b7e`
- status entries after this document: 226
  - tracked modified: 148
  - tracked deleted: 4
  - untracked status entries: 74
  - exact untracked files: 83
- tracked diff: 152 files, 8,106 insertions, 6,154 deletions

Status entries by responsibility:

| Responsibility | Entries | Scope |
| --- | ---: | --- |
| Runtime Java | 41 | `src/main/java` |
| Runtime web | 96 | JSP, CSS, JS, images, and fonts in `src/main/webapp` |
| Runtime resources | 10 | `src/main/resources` |
| Tests | 64 | `src/test` |
| Tools | 1 | the untracked `src/tools` directory; 5 actual files |
| Documentation | 12 | `docs` |
| Build definition | 1 | `build.gradle` |
| Configuration documentation | 1 | `config/README.md` |

The four tracked deletions are intentional:

- `DashboardMenuProvider.java` and its test were retired with the unused dashboard
  quick-action provider. Runtime references remain at zero.
- `V20260720_02__create_hosts.sql` and
  `V20260720_03__add_hosts_row_color.sql` moved from the active migration directory
  to `src/main/resources/db/legacy`. Their SHA-256 values exactly match the tracked
  originals; no SQL was executed.

## Reproducibility fingerprints

| Input or artifact | Count | SHA-256 |
| --- | ---: | --- |
| Runtime source and build inputs | 214 source files | `507289b03e8a5c7f6299c5f5e2de813bc826b2c69065e520509f0456675c8391` |
| Tests | 102 files | `fa259ee9d7e7846f55e08bb6a1baf2bbf894bf97b352191b5fa5dfe1c9a8dc49` |
| Development tools | 5 files | `7b679dcac596ec0866e3b50e694558e0eef33f0049fb637ef73fb86ed47bda83` |
| Built `frog2.war` | 292 archive entries | `1116b4b7f0bdf0a0624fffa85900c2ab46212271e844625e75f8ce66c8bbfb43` |
| Deployed development WAR | current deployed artifact | `ef0ada52624ab93432774dd8a94f26b21f5902703486c00b7ff696af7d878c84` |

The runtime fingerprint covers `src/main/java`, `src/main/resources`,
`src/main/webapp`, `build.gradle`, `settings.gradle`, and `gradle.lockfile`. The
built and deployed development WAR hashes differ because the repository copy of
the IBM Plex Sans KR license had trailing whitespace normalized for Git hygiene.
A recursive extracted-content comparison found that license text to be the only
difference; application classes, JSPs, scripts, styles, images, and libraries are
identical. No redeployment was needed for this non-runtime change.

## Generated and temporary content

- `build/`, `.gradle/`, WAR files, IDE metadata, and
  `catalina.base_IS_UNDEFINED/` are ignored by the repository.
- No `.class`, `.war`, temporary patch file, `db.properties`, or application log is
  present in the Git status or packaged WAR.
- Unit-test logging now sets `catalina.base` below `build/test-catalina/<task>`.
  This prevents tests from recreating `catalina.base_IS_UNDEFINED` at the source
  root.
- The previous 496 KB generated log directory was preserved, not deleted, at
  `/opt/frog2-dev/backups/development-baseline-20260807-160700`.

## Retired experiment check

Runtime references to the retired typography dashboard route, servlet, JSP,
stylesheet, body scope, and development-only helper are zero. The meeting editor
preview remains because it is an active feature.

## Compatibility usage evidence

Production access logs were aggregated without printing request records, client
addresses, query values, or user data. The available window contains 60 daily files
from 2026-05-12 through 2026-08-07 and 9,436 GET/HEAD requests.

| Compatibility surface | Requests | Last observed | Decision |
| --- | ---: | --- | --- |
| `/resources/css/main_style.css` | 1,213 | 2026-08-06 | Keep; actively used |
| legacy `/filerepo/*` URLs | 14 | 2026-06-16 | Keep; external compatibility is active |
| `/admin/pool-status` | 1 | 2026-06-10 | Keep; actual use exists |
| legacy `/vm_hosts/*` path family | 0 | not observed | Keep for now; development access logs are unavailable and the host UI moved recently |

No compatibility route or stylesheet was removed because the evidence does not
prove safe removal. `main_style.css` is therefore an active compatibility entry
point, not dead CSS.

## Verification

- `./gradlew clean test check war --offline`: successful, 336 tests
- WAR allowlist verification: successful
- generated root log directory after the build: absent
- test log output under `build/test-catalina`: present as intended
- built and deployed WAR extracted-content comparison: only the font license
  trailing-whitespace normalization differs
- production PID, WAR, configuration, and service were not changed
- DB DDL and DML were not executed

## Rollback anchors

- Current deployed development runtime predecessor:
  `/opt/frog2-dev/backups/preview-retirement-20260807-132113`
- Preserved generated logs and source-baseline backup directory:
  `/opt/frog2-dev/backups/development-baseline-20260807-160700`

Source archive details:

- `source-tree.tar.gz`: 3,600,897 bytes,
  SHA-256 `e0e723d232c3b7ac9198d7d5128fe6b884c1840361b1805d0c8f6e64ac684394`
- `source-files.sha256`: 351 files,
  SHA-256 `9679886f0b5155609ad550d6d2dcc5621ccfd9cbe32d252440dc35f113151359`

The source snapshot is the immediate pre-commit archive. It excludes
`config/db.properties`, Git
metadata, `build/`, `.gradle/`, IDE state, application logs, and other generated
content. Restore source files only after comparing the manifest. Restore a runtime
only by stopping and starting `tomcat-dev.service`; never operate on the production
Tomcat for this rollback.

This baseline is now anchored by four approved local commits beginning with
`31b2f30`. Future source changes must compare against the current Git `HEAD`, these
fingerprints, or an explicitly approved successor commit.
