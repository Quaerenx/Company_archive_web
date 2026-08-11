# 3단계 개선 결과 — 인증·권한·세션·CSRF·파일 자료실 보안

## 결론

저장소와 새 빌드 WAR 기준으로 외부 CDN 의존성을 제거했고, 로그아웃
cookie 만료와 자료실 중단 업로드 복구를 보강했다. 인증·CSRF·stable
`userId` 소유권은 회귀 테스트로 다시 고정했다.

다만 현재 실행 중인 Tomcat에는 배포하지 않았다. 실제 운영 HTTPS cookie
검증과 Tomcat 10.1.41 업그레이드는 애플리케이션 저장소 밖의 조건이라
완료되지 않았다.

## 마일스톤별 결과

### 1. URL·권한 계약

- [전체 URL matrix](../../security/url-authorization-matrix-20260810.md)를
  생성했다.
- canonical/legacy URL, method, 공개·인증·관리자, 읽기·변경, CSRF,
  stable owner 조건을 기록했다.
- `SecurityRouteMatrixContractTest`가 `web.xml`의 Servlet URL 누락을
  탐지한다.
- 기존 승인 정책대로 고객사·정기점검 등 공유 조회 권한을 유지했고 새
  role을 만들지 않았다.

### 2. 외부 CDN·CSP

- Font Awesome Free 5.15.4와 Chart.js 4.4.4를 버전 고정 자체 호스팅으로
  전환했다.
- 라이선스, npm 원본 URL, 원본 archive SHA-256, 패키징 파일 SHA-256을
  함께 기록했다.
- CSP의 jsDelivr, cdnjs, Google Fonts origin을 제거했다.
- `script-src`, `style-src`, `font-src`는 자체 origin만 허용하며
  `unsafe-inline`과 `unsafe-eval`은 없다.
- 전체 JSP/JSPF/tag/CSS/JS에 외부 runtime asset URL이 다시 들어오면
  회귀 테스트가 실패한다.

### 3. 인증·session·CSRF·IDOR

- 로그인 성공 시 기존 session 무효화 후 typed `UserDTO`만 새 session에
  저장하는 기존 fixation 방어를 재검증했다.
- 로그아웃 POST는 공통 CSRF 검사, session invalidate, 현재 context path의
  `JSESSIONID` 즉시 만료를 모두 수행한다.
- HTTP 개발 요청에서는 cookie `Secure`를 강제하지 않고 HTTPS 요청에서만
  설정한다.
- 로그인 실패 메시지와 제한 메시지는 계정 존재 여부를 노출하지 않는다.
- BCrypt 전에 DB connection이 반환되는 기존 계약과 owner ID를 mutation
  SQL에 함께 넣는 IDOR 방어가 모두 통과했다.
- password, cookie, CSRF token, Authorization 값을 로그에 출력하는 호출은
  발견되지 않았다.

### 4. HTTPS cookie

- [운영 검증 체크리스트](../../security/https-cookie-deployment-checklist-20260810.md)를
  작성했다.
- 실제 Archive HTTPS listener/proxy가 없어 live 검증은 **BLOCKED**다.
- `FROG2_HTTPS_BASE_URL`을 명시해야만 audit가 실행되고, 정상 인증서 검증,
  `HttpOnly`, `Secure`, `SameSite=Strict`, HSTS를 검사한다. cookie 값은
  출력하지 않는다.
- 애플리케이션은 `X-Forwarded-Proto`를 직접 신뢰하지 않는다.

### 5. 로그인 반복 시도

- 감사 보고서의 “애플리케이션 limiter 없음”은 현재 코드와 달랐다.
- 현재 limiter는 account 5회/client 30회, 5분 window·block, 최대 10,000
  key의 bounded in-memory 구조다.
- 직접 Tomcat 단일 노드라는 현재 구조에는 이 방식을 유지하기로 했다.
  자세한 판단은 [결정 기록](../../security/login-rate-limit-decision-20260810.md)에
  있다.
- 로그인 사용자 동작과 임계값은 이번 작업에서 변경하지 않았다.

### 6. 파일 자료실

- temp data를 닫고 `force`, data atomic move, temp metadata를 닫고 `force`,
  metadata final publish 순서를 적용했다.
- 1시간 이상 된 Archive 관리 temp/불완전 pair만 다음 동일-directory 업로드
  시 숨김 quarantine으로 이동한다.
- 최근 파일은 동시 publish 가능성이 있으므로 건드리지 않는다.
- quarantine 경로가 symlink 또는 실제 디렉터리가 아니면 fail-closed한다.
- 운영/고객 파일은 이동·삭제하지 않았고 테스트는 임시 디렉터리만 사용했다.
- 상세 위협과 antivirus 부재는 [자료실 위협 모델](../../security/file-repository-threat-model-20260810.md)에
  기록했다.

### 7. 공식 advisory

- [공식 advisory 대조표](../../security/dependency-advisory-review-20260810.md)를
  작성했다.
- 유효 finding: multipart upload를 실제 사용하는 Tomcat 10.1.41은
  CVE-2025-48988/CVE-2025-48976 영향 범위이며, 이후 multipart temp cleanup
  문제(CVE-2025-61795)도 포함된다.
- 애플리케이션의 5개/10 MiB 제한은 container가 multipart를 파싱하기 전의
  문제를 완전히 막지 못한다.
- shared `/opt/tomcat` 변경은 금지돼 있으므로 현재 지원되는 Tomcat 10.1.x
  업그레이드는 별도 P1 운영 작업으로 남겼다.
- Chart.js CVE-2020-7746은 2.9.4 이전만 영향받아 4.4.4에는 해당하지 않는다.
- Logback 후보는 config write/Janino 조건이 현재 Archive 원격 경로에 없어
  유효한 원격 취약점으로 확정하지 않았다.

## 변경 파일

Runtime:

- `src/main/java/com/company/controller/LogoutServlet.java`
- `src/main/java/com/company/filerepo/FileRepositoryService.java`
- `src/main/java/com/company/filter/SecurityHeadersFilter.java`
- `src/main/webapp/includes/header.jsp`
- `src/main/webapp/maintenance/maintenance_history.jsp`
- `src/main/webapp/resources/vendor/**`

Regression tests:

- `src/test/java/com/company/controller/LogoutServletTest.java`
- `src/test/java/com/company/filerepo/FileRepositoryServiceTest.java`
- `src/test/java/com/company/filter/SecurityHeadersFilterTest.java`
- `src/test/java/com/company/layout/DesignAssetConsolidationTest.java`
- `src/test/java/com/company/layout/MinimalPaletteContractTest.java`
- `src/test/java/com/company/layout/PageShellContractTest.java`
- `src/test/java/com/company/layout/SelfHostedVendorAssetContractTest.java`
- `src/test/java/com/company/security/SecurityRouteMatrixContractTest.java`

Documents:

- `docs/security/url-authorization-matrix-20260810.md`
- `docs/security/https-cookie-deployment-checklist-20260810.md`
- `docs/security/login-rate-limit-decision-20260810.md`
- `docs/security/file-repository-threat-model-20260810.md`
- `docs/security/dependency-advisory-review-20260810.md`

## TDD와 검증 결과

- CDN/CSP: 새 계약에서 3개 실패를 확인한 뒤 통과.
- 로그아웃 cookie: 2개 실패를 확인한 뒤 HTTP/HTTPS 모두 통과.
- 자료실 orphan: stale orphan 실패와 fresh-file 보호를 확인한 뒤 통과.
- URL matrix: 문서가 없을 때 2개 실패를 확인한 뒤 통과.
- 전체 `clean build`: 383 tests, failures 0, errors 0, skipped 0.
- JspC: generated sources 38, classes 63, errors 0, Java 22 compile 성공.
- JavaScript: 앱과 자체 호스팅 vendor 전체 `node --check` 성공.
- WAR content allowlist/금지 파일 검사 성공.
- `git diff --check` 성공.
- 새 WAR SHA-256:
  `5044faa226a5829a94e78edfc0a2d22ebe396c2225725a44a78b649558bfc1fa`.

DB·서버 안전:

- DB 접속, DDL, DML, migration, 인증 POST, 공격 payload: 0건.
- 실제 운영/개발 파일 upload·이동·삭제: 0건.
- Tomcat 재시작·배포: 0건.
- 운영 PID `1012286`, 운영 WAR SHA-256
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`,
  로그인 GET 200 유지.
- 개발 PID `3605261`, 개발 WAR SHA-256
  `50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9`,
  로그인 GET 200 유지.

## 미완료 외부 조건과 릴리스 판단

1. **P1:** Tomcat 10.1.41을 지원되는 최신 10.1.x로 별도 업그레이드하고
   multipart 회귀 검증을 해야 한다.
2. **P1:** 실제 HTTPS endpoint에서 cookie/HSTS audit를 통과해야 한다.
3. **P2:** 격리 인증 환경에서 upload → list → download lifecycle을 실행해야
   한다.
4. **P2/정책:** antivirus/DLP 도입 여부와 quarantine retention/삭제 절차는
   운영 정책 승인이 필요하다.
5. 현재 새 WAR는 실행 중인 개발 Tomcat에 배포하지 않았다. 작업 트리에
   다른 사용자 변경이 함께 있어 보안 작업만 독립 배포할 수 없기 때문이다.

저장소 변경 신뢰도는 **91%**다. 테스트 증거 38/40, 변경 리뷰 27/30,
정적·논리 검토 26/30으로 평가했다. 실제 운영 릴리스 준비는 위 P1 두 건이
남아 완료로 판단하지 않는다.
