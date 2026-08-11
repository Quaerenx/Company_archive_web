# Archive Frontend Release Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Archive 디자인과 서버 계약을 유지하면서 차트·표 접근성, CSS 격리, 키보드·반응형 품질 및 45개 화면 시각 회귀 기준선을 완성한다.

**Architecture:** 기존 JSP 서버 렌더링을 접근 가능한 기본 계층으로 두고 JavaScript/Chart.js는 점진적 향상으로만 사용한다. 공통 계약은 `ui-system`과 정적 JUnit 계약 테스트에 두며 화면 전용 차이는 각 page CSS에 제한한다. 현재 소스를 개발 서버에 먼저 배포한 화면을 변경 전 기준선으로 사용하고, 동일 데이터와 viewport로 변경 후를 비교한다.

**Tech Stack:** Java 22, Gradle WAR, Jakarta Servlet 6, JSP 3.1/JSTL, JUnit 5, vanilla JavaScript, Chart.js 4.4.4, Tomcat development instance, Firefox BiDi visual capture.

## Global Constraints

- 운영 `/opt/tomcat`, 운영 WAR, 8080 서비스와 공유 DB를 변경하지 않는다.
- 기존 Archive 색상, 폰트, 카드·폼·레이아웃, URL, form name/id, 서버 응답을 유지한다.
- 외부 UI framework/CDN, 새 하드코딩 색상, 새 `!important`를 추가하지 않는다.
- 현재 94개 작업 트리 변경을 모두 사용자 작업으로 보존한다.
- commit, branch, push, PR을 생성하지 않는다.
- 인증정보, cookie, 고객사 식별자를 명령·로그·manifest·screenshot 이름에 남기지 않는다.
- production code 변경 전 관련 계약 테스트를 추가하고 예상 이유로 실패하는 것을 확인한다.

---

### Task 1: 변경 전 소스·배포·화면 기준선

**Files:**
- Modify: `src/tools/visual-regression-routes.tsv`
- Modify: `src/tools/capture-visual-regression.mjs`
- Modify: `docs/frog2-visual-regression-20260805.md`
- Create: `src/test/java/com/company/layout/VisualRegressionToolContractTest.java`

**Interfaces:**
- Consumes: 현재 7개 route TSV와 4개 viewport capture runner.
- Produces: 9개 논리 route, 5개 viewport, 상세 route runtime discovery, 민감정보 없는 capture manifest.

- [ ] **Step 1: 현재 상태를 보존한다.**
  `/tmp/frog2-frontend-source-baseline-20260811-011858/frog2`가 현재 소스의 완전한 복사본인지 파일 수와 SHA-256 표본으로 확인하고 branch, HEAD, 작업 트리 분류를 감사 보고서에 기록한다.
- [ ] **Step 2: visual tool 계약 테스트를 추가한다.**
  `VisualRegressionToolContractTest`에서 route 별칭 `login`, `dashboard`, `customers`, `customer-detail`, `maintenance-history`, `meeting`, `troubleshooting`, `file-repository`, `mypage`와 viewport `360,390,768,1024,1440`, `scrollWidth`, console error 수집, route-only screenshot 이름을 단언한다.
- [ ] **Step 3: 테스트가 현재 7 route/4 viewport 때문에 실패하는지 확인한다.**
  Run: `./gradlew test --tests com.company.layout.VisualRegressionToolContractTest`
  Expected: FAIL for missing `login`, detail routes and `390`.
- [ ] **Step 4: capture runner를 최소 확장한다.**
  공개 로그인은 직접 캡처하고, 인증 route는 인증 상태를 확인한다. 상세 route는 고객사 목록의 `.customer-detail-link`에서 URL을 메모리로 얻고 동일 고객사의 정기점검 이력 링크를 DOM에서 찾는다. 출력 이름은 논리 route와 viewport만 사용한다.
- [ ] **Step 5: 계약 테스트를 통과시킨다.**
  Run: `./gradlew test --tests com.company.layout.VisualRegressionToolContractTest`
  Expected: PASS.
- [ ] **Step 6: 현재 소스를 개발에 baseline 배포한다.**
  개발 WAR, exploded app, work를 새 timestamp 경로에 백업하고 개발 Tomcat만 재시작한다. DB write 요청은 실행하지 않는다.
- [ ] **Step 7: 변경 전 45개 화면을 캡처한다.**
  안전한 인증 profile이 없으면 로그인 화면만 캡처하고 인증 화면은 BLOCKED로 기록한다. 각 화면의 console 오류와 `documentElement.scrollWidth <= clientWidth`를 검사한다.

### Task 2: 라이선스 차트 대체 정보

**Files:**
- Modify: `src/test/java/com/company/layout/MaintenanceHistoryViewContractTest.java`
- Modify: `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- Modify: `src/main/webapp/maintenance/maintenance_history.jsp`
- Modify: `src/main/webapp/resources/js/pages/maintenance_history.js`
- Modify: `src/main/webapp/resources/css/pages/maintenance_history.css`

**Interfaces:**
- Consumes: request attribute `usageSeries` with `date`, `pct`, `usedTb`, `sizeTb`.
- Produces: `licenseUsageChartTitle`, `licenseUsageChartSummary`, `licenseUsageChartTable` and canvas ARIA linkage.

- [ ] **Step 1: failing JSP contract tests를 작성한다.**
  canvas의 `role="img"`, `aria-labelledby`, 최근 값 요약, `<details>`, `<table>`, `<caption>`, 네 개 `scope="col"`과 동일 `usageSeries` 반복을 단언한다.
- [ ] **Step 2: 현재 마크업에서 실패를 확인한다.**
  Run: `./gradlew test --tests com.company.layout.MaintenanceHistoryViewContractTest --tests com.company.layout.MaintenanceFormAssetContractTest`
  Expected: FAIL for missing alternative table and accessible chart name.
- [ ] **Step 3: 서버 렌더링 대체 정보를 구현한다.**
  최근 점과 직전 점을 JSTL로 선택해 최근 사용률·사용량·전체 용량과 가능한 경우 변화량을 문장으로 표시한다. 날짜/사용률/사용량/전체 용량 표는 `usageSeries`를 직접 반복한다.
- [ ] **Step 4: 차트 시각 구분을 보강한다.**
  사용률은 circle/실선, 사용량은 triangle/실선, 전체 용량은 rectRot/점선을 유지한다. 새 색상은 만들지 않고 기존 chart token만 사용한다.
- [ ] **Step 5: 관련 테스트와 JspC를 통과시킨다.**
  Run: `./gradlew test --tests com.company.layout.MaintenanceHistoryViewContractTest --tests com.company.layout.MaintenanceFormAssetContractTest jspcJava22`
  Expected: PASS and JspC 0 errors.

### Task 3: 데이터 표 의미와 공통 footer

**Files:**
- Modify: `src/test/java/com/company/layout/TableFooterViewContractTest.java`
- Create: `src/test/java/com/company/layout/DataTableAccessibilityContractTest.java`
- Modify: `src/main/webapp/customers/customers_list.jsp`
- Modify: `src/main/webapp/meeting/meeting_list.jsp`
- Modify: `src/main/webapp/troubleshooting/troubleshooting_list.jsp`
- Modify: `src/main/webapp/mypage/monthly_customer_response.jsp`
- Modify: `src/main/webapp/WEB-INF/views/filerepo/list.jsp`
- Modify: `src/main/webapp/mypage/mypage.jsp`
- Modify: `src/main/webapp/vm_hosts/list.jsp`
- Modify: `src/main/webapp/resources/css/ui-system.css`

**Interfaces:**
- Consumes: existing `ui-table`, `ui-table-wrap`, `tableFooter.tag` contracts.
- Produces: table-specific captions, scoped column headers, synchronized customer `aria-sort`, 44×44 pagination controls.

- [ ] **Step 1: failing table contract tests를 작성한다.**
  일곱 JSP 각각의 caption과 모든 header의 `scope="col"`, 고객사 정렬 header의 `aria-sort`, empty `colspan`, footer control `44px`를 단언한다.
- [ ] **Step 2: 누락된 caption/scope와 32px footer 때문에 실패하는지 확인한다.**
  Run: `./gradlew test --tests com.company.layout.DataTableAccessibilityContractTest --tests com.company.layout.TableFooterViewContractTest`
  Expected: FAIL with affected page path and 32px control assertions.
- [ ] **Step 3: 의미 정보만 JSP에 추가한다.**
  표 모양과 열 순서를 바꾸지 않고 `caption class="sr-only"`, `scope="col"`을 추가한다. sortable 고객사 header는 기존 `aria-sort` 계산을 유지한다.
- [ ] **Step 4: footer target을 44×44로 확대한다.**
  `ui-system.css`의 `.ui-table-pagination__control` block/inline size만 `var(--control-height-md)`로 바꾸고 기존 색상·radius·간격은 유지한다.
- [ ] **Step 5: 관련 계약 테스트와 JspC를 통과시킨다.**
  Run: `./gradlew test --tests com.company.layout.DataTableAccessibilityContractTest --tests com.company.layout.TableFooterViewContractTest jspcJava22`
  Expected: PASS and JspC 0 errors.

### Task 4: CSS 충돌과 레거시 계층 정리

**Files:**
- Modify: `src/test/java/com/company/layout/MeetingCssSplitContractTest.java`
- Modify: `src/test/java/com/company/layout/UiDesignSystemContractTest.java`
- Modify: `src/main/webapp/resources/css/pages/meeting.css`
- Modify: `src/main/webapp/resources/css/pages/meeting_view.css`
- Modify only if verified: `src/main/webapp/resources/css/pages/meeting_list_layout.css`
- Modify only if verified: affected meeting JSP class lists.

**Interfaces:**
- Consumes: `.page-meeting`, `.meeting-view`, canonical `ui-button`, `ui-form`, `ui-table` classes.
- Produces: shared meeting rules without duplicate view/comment selectors; legacy mappings retained only where markup still uses them.

- [ ] **Step 1: selector inventory를 생성한다.**
  meeting CSS selector와 JSP/JS 사용처를 교차 검색해 `active`, `compatibility`, `unused-candidate`로 분류하고 전후 수치를 기록한다.
- [ ] **Step 2: failing collision contract를 추가한다.**
  shared `meeting.css`가 view 전용 `.comments-section`, `.comment-item`, `.meeting-header`를 재정의하지 않으며 page 전용 selector가 `:where(body.page-meeting)` 또는 `.meeting-view` 범위에 있음을 단언한다.
- [ ] **Step 3: 현재 중복으로 실패하는지 확인한다.**
  Run: `./gradlew test --tests com.company.layout.MeetingCssSplitContractTest --tests com.company.layout.UiDesignSystemContractTest`
  Expected: FAIL for duplicated selectors.
- [ ] **Step 4: 대표 회의록 화면 한 개에서 최소 이동한다.**
  view/comment 전용 정의는 `meeting_view.css`에만 두고 shared 파일에는 실제 여러 meeting 화면이 함께 쓰는 규칙만 남긴다. 레거시 button class는 canonical class와 함께 사용하는 compatibility 상태를 유지한다.
- [ ] **Step 5: 관련 테스트와 정적 selector inventory를 재실행한다.**
  Expected: duplicate count decreases, unresolved dynamic candidates remain reported rather than deleted.

### Task 5: modal·keyboard·motion contracts

**Files:**
- Create: `src/test/java/com/company/layout/DialogAccessibilityContractTest.java`
- Modify only if failing: modal JSP files under `WEB-INF/includes`, `meeting`, `mypage`.
- Modify only if failing: `src/main/webapp/resources/js/ui-system.js`, `header_nav.js`, page modal scripts.

**Interfaces:**
- Consumes: `ArchiveUI.createDialogController(dialog)`.
- Produces: labelled dialogs with initial focus, focus trap, Escape, opener restore; documented inert decision.

- [ ] **Step 1: 전체 modal inventory와 failing contract를 작성한다.**
  각 modal의 `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, focus 가능한 close/control, common controller 사용을 단언한다.
- [ ] **Step 2: 현재 실패 항목을 확인한다.**
  Run: `./gradlew test --tests com.company.layout.DialogAccessibilityContractTest`
  Expected: missing title/focus contract가 있으면 해당 파일명으로 FAIL; 모두 충족하면 production code를 변경하지 않고 검증 결과만 기록한다.
- [ ] **Step 3: 실패 항목만 최소 수정한다.**
  기존 controller의 trap/Escape/opener 복귀를 재사용하고 별도 keydown 구현을 늘리지 않는다. nested modal은 허용하지 않는 계약을 추가한다.
- [ ] **Step 4: inert 결정을 기록한다.**
  dialog가 body 직계 자식이 아닌 현재 구조에서는 ancestor를 inert 처리하면 dialog도 비활성화되므로 강제 적용하지 않는다. 향후 portal 구조 전환 전까지 focus trap과 `aria-modal`을 유지한다.
- [ ] **Step 5: reduced-motion 계약과 JS 문법을 검증한다.**
  Run: `./gradlew test --tests com.company.layout.DialogAccessibilityContractTest --tests com.company.layout.UiDesignSystemContractTest jsSyntaxCheck`

### Task 6: 반응형과 스크롤 인지성

**Files:**
- Create: `src/test/java/com/company/layout/ResponsiveAccessibilityContractTest.java`
- Modify: `src/main/webapp/resources/css/ui-system.css`
- Modify only as proven: affected page CSS/JSP.

**Interfaces:**
- Consumes: `ui-table-wrap`, breakpoints 480/768/1024, `100dvh` shell.
- Produces: no document overflow; table-contained scrolling with keyboard/focus indication.

- [ ] **Step 1: responsive static contract를 작성한다.**
  핵심 CSS의 360px 대응, `overflow-x:auto`, `:focus-visible`, `100dvh`, 승인 breakpoint를 단언하고 새 576/1050/1200 breakpoint를 금지한다.
- [ ] **Step 2: 현재 필요한 항목만 실패하는지 확인한다.**
  Run: `./gradlew test --tests com.company.layout.ResponsiveAccessibilityContractTest`
- [ ] **Step 3: 공통 table scroll affordance를 최소 추가한다.**
  색상 token만 사용해 focus ring과 scrollbar 안정성을 제공한다. document 폭을 늘리는 min-width나 fixed pixel width는 추가하지 않는다.
- [ ] **Step 4: 360/390/768에서 navigation, search, table footer, chart, modal, page action과 긴 텍스트를 browser metrics로 검증한다.**
  document overflow가 재현된 페이지 CSS만 수정하고 두 화면 이상 예상치 못한 회귀가 생기면 즉시 중단한다.

### Task 7: 전체 검증과 변경 후 시각 기준선

**Files:**
- Modify: `docs/frog2-visual-regression-20260805.md`
- Create: `docs/audits/2026-08-10/06-frontend-release-readiness-remediation.md`

**Interfaces:**
- Consumes: Tasks 1–6의 source와 before baseline.
- Produces: after baseline, 검증표, rollback 경로, GO/NO-GO.

- [ ] **Step 1: 정적 검증을 실행한다.**
  Run targeted layout tests, full `./gradlew clean build`, `jspcJava22`, JavaScript syntax, CSS contract, `git diff --check`, WAR allowlist.
- [ ] **Step 2: clean build를 다시 실행한다.**
  두 WAR의 SHA-256과 entry 목록을 비교한다. 실제 차이가 있으면 배포를 중단한다.
- [ ] **Step 3: 개발 배포 백업을 새 timestamp로 생성한다.**
  현재 개발 WAR, exploded app, work를 복구 가능한 경로에 보존하고 개발 Tomcat만 재시작한다.
- [ ] **Step 4: 변경 후 45개 화면을 캡처한다.**
  before와 같은 data/session/viewport를 사용하고 console error 0, document overflow 0, focus ring, modal layer를 확인한다.
- [ ] **Step 5: before/after를 검토한다.**
  의도된 차이는 chart 대체 정보, 숨은 table semantics, 44px pagination target, 검증된 CSS 충돌 감소뿐이어야 한다. main width, navigation width, 배경, radius, 업무 레이아웃 변화는 회귀로 처리한다.
- [ ] **Step 6: Tomcat과 운영 무영향을 확인한다.**
  개발 최근 로그에서 JSP compile/ClassNotFound/NoSuchMethod/linkage 오류를 확인한다. 운영 PID, WAR SHA-256, 8080 GET이 작업 전과 동일한지 읽기 전용으로 비교한다.
- [ ] **Step 7: 최종 보고서를 작성한다.**
  변경 파일, red-green 증거, CSS/legacy 전후 수치, 45 viewport 결과, screenshot 위치, 개발 rollback 경로, 운영 무영향, 미검증 공백과 GO/NO-GO를 기록한다.
