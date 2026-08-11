# 6단계 개선 결과 — 프론트엔드·접근성·반응형·릴리스 준비

작업일: 2026-08-11

대상: `/opt/frog2-dev/repo/frog2` 및 개발 Tomcat `/opt/tomcat-dev`

운영 반영: 수행하지 않음

## 결론

- **개발 코드·빌드·개발 배포: 통과**
- **운영 반영 판단: NO-GO**
- NO-GO 사유는 기능 실패가 아니라 **인증된 내부 8개 화면 × 5 viewport, 총 40개 화면의 최신 시각 회귀가 미완료**이기 때문이다.
- 공개 로그인 화면 5개 viewport는 console 오류, document overflow, 시각 차이 없이 통과했다.
- 공유 DB에 DDL/DML 또는 인증 POST를 실행하지 않았고 운영 Tomcat은 변경·재시작하지 않았다.

## 작업 전 기준선과 보존 위치

- 소스 기준선: `/tmp/frog2-frontend-source-baseline-20260811-011858/frog2`
- 최초 개발 배포 기준선: `/opt/frog2-dev/backups/frontend-baseline-before-20260811-012514`
- 최종 배포 직전 백업: `/opt/frog2-dev/backups/frontend-final-before-20260811-014109`
- 최종 배포 직전 개발 WAR SHA-256: `79381f1393fd500b5655d52bf97b8253bee4b0e6e0a66a3ae9e10ed941318766`
- 최종 개발 WAR SHA-256: `eac9827f90e8a9d7186d5fb29368caa3b95aa974294e5332e4ade77ab2342c9a`

기존 작업 트리의 수정·미추적 파일은 모두 사용자 작업으로 보존했다. commit, branch, push, PR은 생성하지 않았다.

## 작업 범위 변경 파일 (35개)

문서·계획:

- `docs/audits/2026-08-10/06-frontend-release-readiness-remediation.md`
- `docs/frog2-visual-regression-20260805.md`
- `docs/superpowers/plans/2026-08-11-frontend-release-readiness.md`
- `docs/superpowers/specs/2026-08-11-frontend-release-readiness-design.md`

JSP·배포 계약:

- `src/main/webapp/WEB-INF/views/filerepo/list.jsp`
- `src/main/webapp/WEB-INF/web.xml`
- `src/main/webapp/customers/customers_list.jsp`
- `src/main/webapp/maintenance/maintenance_history.jsp`
- `src/main/webapp/meeting/meeting_list.jsp`
- `src/main/webapp/mypage/monthly_customer_response.jsp`
- `src/main/webapp/mypage/mypage.jsp`
- `src/main/webapp/troubleshooting/troubleshooting_list.jsp`
- `src/main/webapp/vm_hosts/list.jsp`

CSS·JavaScript:

- `src/main/webapp/resources/css/base.css`
- `src/main/webapp/resources/css/pages/header.css`
- `src/main/webapp/resources/css/pages/maintenance_history.css`
- `src/main/webapp/resources/css/pages/meeting.css`
- `src/main/webapp/resources/css/pages/meeting_view.css`
- `src/main/webapp/resources/css/pages/mypage.css`
- `src/main/webapp/resources/css/ui-system.css`
- `src/main/webapp/resources/js/pages/maintenance_history.js`
- `src/main/webapp/resources/js/ui-system.js`

계약 테스트:

- `src/test/java/com/company/layout/CustomerPaginationViewContractTest.java`
- `src/test/java/com/company/layout/DataTableAccessibilityContractTest.java`
- `src/test/java/com/company/layout/DialogAccessibilityContractTest.java`
- `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- `src/test/java/com/company/layout/MaintenanceHistoryViewContractTest.java`
- `src/test/java/com/company/layout/MeetingCssSplitContractTest.java`
- `src/test/java/com/company/layout/PageShellContractTest.java`
- `src/test/java/com/company/layout/ResponsiveAccessibilityContractTest.java`
- `src/test/java/com/company/layout/TableFooterViewContractTest.java`
- `src/test/java/com/company/layout/VisualRegressionToolContractTest.java`

시각 회귀 도구:

- `src/tools/capture-visual-regression.mjs`
- `src/tools/visual-regression-routes.tsv`
- `src/tools/visual-regression.sh`

## 변경 내용

### 1. 차트 접근성

- 라이선스 추이 canvas에 제목과 설명 연결을 추가했다.
- 최근 값과 직전 값의 변화를 같은 서버 데이터로 요약한다.
- JavaScript나 Chart.js가 실패해도 날짜·사용률·사용량·전체 용량을 읽을 수 있는 대체 표를 서버에서 렌더링한다.
- 색상 외 구분을 위해 사용률은 원, 사용량은 삼각형, 전체 용량은 회전 사각형과 점선을 사용한다.

주요 파일:

- `src/main/webapp/maintenance/maintenance_history.jsp`
- `src/main/webapp/resources/js/pages/maintenance_history.js`
- `src/main/webapp/resources/css/pages/maintenance_history.css`

### 2. 표 접근성

다음 7개 표에 문맥에 맞는 caption과 column scope를 보완했다.

1. 고객사
2. 회의록
3. 트러블슈팅
4. 월별 고객 응대
5. 자료실
6. 마이페이지 VM
7. 개인 호스트

고객사 정렬 표의 기존 `aria-sort` 계약은 유지했다. 공통 표 footer의 이전/다음 버튼은 32×32px에서 공통 44px control token으로 변경했다.

### 3. CSS 충돌 감소

- `meeting.css`와 `meeting_view.css`의 완전 중복 selector를 **11개에서 0개**로 줄였다.
- 상세·댓글 전용 규칙은 `.meeting-view` 범위로 제한했다.
- 새 `!important`와 새 하드코딩 색상을 추가하지 않았다.
- 인증 화면 시각 회귀 증거가 부족하므로 레거시 class를 일괄 삭제하지 않았다.

| 지표 | 변경 전 | 변경 후 |
| --- | ---: | ---: |
| meeting 완전 중복 selector | 11 | 0 |
| CSS `!important` 출현 | 34 | 34 |
| CSS raw color literal 출현 | 33 | 33 |
| `ui-button` 출현 | 121 | 121 |
| `button--primary` 출현 | 26 | 26 |
| `button--secondary` 출현 | 76 | 76 |
| `button--danger` 출현 | 10 | 10 |

### 4. modal·keyboard

- 현재 5개 dialog source가 모두 `role=dialog`, `aria-modal`, 제목 연결, 초기 focus 계약을 충족함을 테스트로 고정했다.
- 공통 dialog controller의 Tab/Shift+Tab trap, Escape, opener focus 복귀를 확인했다.
- `inert`는 적용하지 않았다. 현재 dialog가 body 직계 portal이 아니어서 ancestor에 `inert`를 주면 dialog 자체도 비활성화될 수 있기 때문이다.

### 5. 반응형과 표 스크롤

- 기존 `100vh` fallback 뒤에 `100dvh`를 적용해 모바일 주소창 변화에 대응했다.
- 실제로 넘치는 표 wrapper만 keyboard focus 가능한 labelled region으로 전환한다.
- 표가 넘치지 않으면 불필요한 `tabindex`와 region role을 제거한다.
- 공통 scrollbar와 focus-visible은 기존 token만 사용한다.

### 6. 시각 회귀 도구

- 논리 route를 7개에서 9개로 확장했다.
- viewport를 360/768/1024/1440에서 **360/390/768/1024/1440**으로 확장했다.
- 고객사 상세와 정기점검 이력 URL은 목록 DOM에서 런타임에 찾되 screenshot 이름에는 고객 식별자를 남기지 않는다.
- 각 캡처에 route, viewport, document scrollWidth, console error 수를 기록한다.
- 공개 화면과 인증 화면 profile을 분리했다.

대상 route:

1. 로그인
2. 대시보드
3. 고객사 목록
4. 고객사 상세
5. 정기점검 이력
6. 회의록
7. 트러블슈팅
8. 자료실
9. 마이페이지

## 시각 회귀 결과

### 통과

- 공개 로그인: **5/5 viewport**
- console error: **0건**
- document overflow: **0건**
- 변경 전/후 absolute error pixel: **모든 viewport 0**

기준선:

- 변경 전: `/opt/frog2-dev/visual-baselines/20260811-frontend-before`
- 변경 후: `/opt/frog2-dev/visual-baselines/20260811-frontend-after`

### 미완료

- 인증 화면: **0/40 viewport**
- 기존 Firefox profile은 개발 서버에 인증돼 있지 않았다.
- 비밀번호를 명령, 환경 덤프, manifest 또는 screenshot metadata에 남기지 않는 조건을 지키기 위해 자격증명을 주입하지 않았다.
- 따라서 대시보드·고객사 목록/상세·정기점검 이력·회의록·트러블슈팅·자료실·마이페이지의 실제 computed layout, modal layer와 console 상태는 최종 운영 승인 전에 한 번 더 확인해야 한다.

## 검증 결과

| 검증 | 결과 |
| --- | --- |
| clean build 1 | 성공 |
| clean build 2 | 성공 |
| WAR 재현성 | 두 WAR SHA-256 동일, byte compare 동일 |
| 전체 테스트 | 408 tests, 0 failures, 0 errors, 0 skipped |
| Java 22 JspC | 38 sources, 63 classes, 0 errors |
| WAR allowlist | 성공 |
| JavaScript 문법 | 27 files 성공 |
| visual shell 문법 | 성공 |
| `git diff --check` | 성공 |
| 공개 화면 console | 0 errors |
| 공개 화면 overflow | 0 |

두 clean build의 WAR SHA-256:

`eac9827f90e8a9d7186d5fb29368caa3b95aa974294e5332e4ade77ab2342c9a`

## 개발 배포 결과

- 개발 Tomcat만 중지·시작했다.
- 개발 PID: `3903978`
- 로그인 GET: HTTP 200
- `ui-system.css`: HTTP 200
- 비인증 dashboard GET: HTTP 302, `/frog2/login`으로 이동
- 최신 Catalina/application log의 JSP compile, `ClassNotFoundException`, `NoSuchMethodError`, linkage, Jasper 오류: 0건
- 최초 로그인 GET은 JSP warm-up으로 약 565ms였고 후속 요청은 약 1~3ms였다.
- 인증 POST와 공유 DB 쓰기 요청은 실행하지 않았다.

## 운영 무영향

- 운영 PID 전/후: `1012286` 유지
- 운영 WAR SHA-256 전/후: `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88` 유지
- 운영 8080 로그인 GET: HTTP 200 유지
- `/opt/tomcat`의 WAR, config, bin, lib를 변경하지 않았고 운영을 재시작하지 않았다.

## 개발 롤백 절차

다음 경로는 이번 배포 직전 상태를 완전 보존한다.

`/opt/frog2-dev/backups/frontend-final-before-20260811-014109`

복구 시 개발 Tomcat만 중지한 뒤 현재 WAR·exploded app·Jasper work를 별도 실패 보존 폴더로 이동하고, 위 폴더의 `frog2.war`, `exploded-app`, `jasper-work`를 각각 다음 위치로 복원한다.

- `/opt/tomcat-dev/webapps/frog2.war`
- `/opt/tomcat-dev/webapps/frog2`
- `/opt/tomcat-dev/work/Catalina/localhost/frog2`

소유권은 `tomcat-dev:tomcat-dev`, WAR mode는 `0640`, directory mode는 `0750`을 유지한 뒤 개발 Tomcat만 시작한다. 운영 경로는 사용하지 않는다.

## 최종 판단과 남은 최소 작업

### 개발 기준

기능 계약, 정적 접근성, 빌드, JSP 컴파일과 공개 화면에 대해서는 배포를 유지해도 된다.

### 운영 기준

현재는 **NO-GO**다. 아래 한 가지가 완료되면 다시 GO를 판단할 수 있다.

1. 비밀값을 저장하지 않는 인증 profile을 준비한다.
2. 내부 8 route × 5 viewport를 동일 데이터로 캡처한다.
3. console error 0, document overflow 0, 업무 레이아웃·modal·focus 회귀 없음을 확인한다.

현재 신뢰도:

- 코드·테스트·빌드: 96%
- 공개 화면: 95%
- 인증 내부 화면 시각 회귀: 55%
- 종합: 84%

종합 신뢰도가 85% 미만이므로 이 문서는 작업을 운영 릴리스 완료로 선언하지 않는다.
