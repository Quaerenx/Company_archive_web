# frog2 개발 공통 기반 개선 보고서 (2026-07-30)

## 작업 범위

- 개발 소스: `/opt/frog2-dev/repo/frog2`
- 개발 런타임: `/opt/tomcat-dev`, HTTP 18081
- 운영 런타임 `/opt/tomcat`, HTTP 8080은 읽기 확인만 수행
- DB DDL, DML, migration, 로그인 POST, 인증 상태 변경 요청은 실행하지 않음
- 커밋, 브랜치, push, PR, staging은 수행하지 않음

## 적용 내용

### 세션과 인증

- 세션 principal 접근을 `SessionPrincipal`로 통합
- 잘못된 타입의 세션 user를 인증으로 인정하지 않고 제거
- 로그인과 프로필 갱신 시 password를 세션에 저장하지 않음
- 로그인 성공 시 기존 세션을 폐기하고 새 세션을 사용
- 컨트롤러 redirect를 context path 기준 절대 경로로 통일

### 보안과 오류 계약

- CSRF 안전 메서드를 GET, HEAD, OPTIONS로 제한하고 TRACE를 차단
- 정적 자원 공개 범위를 GET 또는 HEAD와 CSS, JS, PNG, favicon allowlist로 제한
- JSP, class, HTML, SVG, path parameter, encoded path, traversal 형태는 공개 정적 자원으로 보지 않음
- 정적 자원처럼 보이는 POST와 비정적 응답은 no-store 적용
- 공통 `ApplicationError`로 HTML과 JSON 오류 응답을 분기
- 오류 작성 전 미커밋 partial body를 resetBuffer로 제거
- ServletException에 감싸진 DataAccessException도 409 또는 503으로 분류
- 400과 405 공통 오류 페이지 추가
- security header는 response reset 이후에도 재적용

### DB 읽기 전용

- DatabaseMetaData, ResultSet, Statement, unwrap 경로를 통한 raw connection 우회 차단
- updateable ResultSet과 ResultSet update 계열 메서드 차단
- SELECT INTO 차단
- 모든 읽기 전용 위반 SQLState를 25006으로 통일
- 개발 JVM의 `frog2.env=dev`, `frog2.readOnly=true` 활성 상태 확인
- 실제 JDBC SQL은 실행하지 않음

### 등록 방식과 파일 자료실

- AppLifecycleListener와 PoolMonitorServlet 등록을 `web.xml` 단일 방식으로 통일
- 파일 목록과 업로드 화면의 GET 오류를 공통 HTML 또는 JSON 협상으로 통일
- canonical과 legacy 업로드 URL 모두 GET은 HTML, POST는 JSON 계약으로 통일
- 업로드 JS가 `X-CSRF-Token` header를 보내 multipart body 선파싱을 피하도록 보정
- 업로드 POST와 다운로드 API의 기존 JSON 계약은 유지

## 주요 변경 파일

- `src/main/java/com/company/security/SessionPrincipal.java`
- `src/main/java/com/company/security/CsrfFilter.java`
- `src/main/java/com/company/filter/AuthFilter.java`
- `src/main/java/com/company/filter/ApplicationExceptionFilter.java`
- `src/main/java/com/company/filter/SecurityHeadersFilter.java`
- `src/main/java/com/company/web/ApplicationError.java`
- `src/main/java/com/company/web/JsonResponse.java`
- `src/main/java/com/company/web/RequestPaths.java`
- `src/main/java/com/company/util/ReadOnlyJdbcGuard.java`
- `src/main/java/com/company/listener/AppLifecycleListener.java`
- `src/main/java/com/company/controller/PoolMonitorServlet.java`
- `src/main/java/com/company/controller/FileRepositoryServlet.java`
- `src/main/java/com/company/controller/FileRepositoryUploadServlet.java`
- 로그인 사용자 접근이 있는 9개 controller
- `src/main/webapp/WEB-INF/web.xml`
- `src/main/webapp/error/400.jsp`
- `src/main/webapp/error/405.jsp`
- `src/main/webapp/resources/js/pages/file_repository_upload.js`
- 관련 단위 및 계약 테스트

제거한 중복 구현:

- `src/main/java/com/company/controller/SessionUser.java`
- `src/main/java/com/company/filerepo/FileRepositoryCsrf.java`
- 대응 중복 테스트 2개

## 검증 결과

- Java 22 `clean test check war --offline`: 최종 2회 연속 성공
- 단위 및 계약 테스트: 171개, failures 0, errors 0, skipped 0
- JspC: 입력 41개, 생성 class 55개, errors 0
- `git diff --check`: 성공
- 작업 전 스냅샷 대비 untracked Java, test, webapp 공백 검사: 오류 없음
- WAR allowlist: 성공
- 최종 WAR SHA-256: `9a9f87fc0c0f94fc6e314d1ec86e2e2af84f4600720a6bf9fb9a01b510410907`
- WAR 안에 source, Javadoc, test JAR, Java source, db.properties, IDE 또는 build 산출물 없음
- 런타임 JAR은 HikariCP, JSTL API와 구현, jbcrypt, SLF4J, Logback, Vertica JDBC 8개
- 보존 디자인 해시:
  - `header_nav.jspf`: `51d9031b706a9b9983bc0da22480569af25ef36f757cdb3550366e74febf0a8f`
  - `login.jsp`: `a38038f9e7a9e44c9006af74c7c51715e95a3b37464f9f4c2f66fbc594fe9e41`
  - `linear_refinement.css`는 작업 전부터 없었고 새로 만들지 않음

## 개발 배포 결과

- 최종 개발 PID: `3471093`
- 최종 개발 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- 설치 WAR SHA-256은 build WAR와 동일
- 배포 720 ms, 서버 시작 760 ms
- login GET 200
- base.css GET 200
- 비인증 dashboard GET 302, Location `/frog2/login`
- 비인증 download GET 401 JSON, code `authentication_required`
- favicon GET 301, 정적 public cache 적용
- 정적처럼 보이는 JSP GET은 login 302와 no-store
- legacy upload process GET은 login 302로 HTML 계약 적용
- 배포 JS에서 `X-CSRF-Token` header 보정 확인
- JSP compile, ClassNotFoundException, NoSuchMethodError, linkage 오류 없음

백업:

- 전체 작업 전 소스: `/opt/frog2-dev/backups/common-platform-20260730-112907`
- 전체 작업 전 개발 런타임: `/opt/frog2-dev/backups/common-platform-deploy-20260730_121654`
- 최종 보정 배포 직전 런타임: `/opt/frog2-dev/backups/common-platform-deploy-20260730_123243`

## 운영 무영향

- 운영 PID `1012286` 유지
- 운영 시작 시각 `2026-06-30 10:18:40 KST` 유지
- 운영 WAR SHA-256 `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88` 유지
- 운영 server.xml SHA-256 `34afc0a0f9d78660c5ded03b1654b9a24204378495928baf909b6238ac3ec47a` 유지
- 운영 8080 login GET 200
- 운영 login 본문은 작업 전후 바이트 단위 동일

## 실행하지 않은 migration

없음. DDL, DML, 비밀번호 migration utility를 실행하지 않았다.

## 전체 작업 롤백 절차

전체 작업 전 상태로 되돌릴 때는 `/opt/frog2-dev/backups/common-platform-deploy-20260730_121654`를 사용한다.

1. `tomcat-dev.service`만 중지한다.
2. 현재 `/opt/tomcat-dev/webapps/frog2.war`, `/opt/tomcat-dev/webapps/frog2`, `/opt/tomcat-dev/work/Catalina/localhost/frog2`를 새 failed-runtime 타임스탬프 디렉터리로 이동한다.
3. `frog2.war.before`를 `/opt/tomcat-dev/webapps/frog2.war`로 복원한다.
4. `frog2-exploded.live-moved`를 `/opt/tomcat-dev/webapps/frog2`로 복원한다.
5. `frog2-work.before`를 `/opt/tomcat-dev/work/Catalina/localhost/frog2`로 복원한다.
6. WAR 소유권 `tomcat-dev:tomcat-dev`, 권한 0640과 디렉터리 소유권을 확인한다.
7. `tomcat-dev.service`만 시작하고 18081, login GET, 로그를 확인한다.
8. 운영 PID, WAR hash, 8080 login 응답이 그대로인지 다시 확인한다.

최종 보정만 되돌릴 때는 같은 절차로 `/opt/frog2-dev/backups/common-platform-deploy-20260730_123243`를 사용한다.

## 남은 위험과 최소 추가 검증

- 실제 로그인 POST와 인증 상태의 변경 요청은 DB 쓰기 금지 조건 때문에 실행하지 않음
- oversized multipart의 실제 Tomcat 413 통합 테스트는 인증 세션이 필요해 실행하지 않음. JS header 계약과 filter mock test로 검증
- 환경 설정이 모두 누락되면 read-only가 fail-open이다. 현재 개발은 명시적 dev와 true라 보호되지만, 향후 운영 배포 전 `frog2.env=prod`, `frog2.readOnly=false`를 명시하고 이후 fail-closed 전환을 별도 작업으로 검토해야 함
- SQL 키워드 guard는 부수효과 SELECT 함수와 mutable LOB를 완전 차단하지 못하므로 DB 계정 권한이 추가 방어선이어야 함
- 직접 IOException은 client disconnect와 구분이 어려워 공통 오류 변환 대상에서 제외됨
- `/admin/pool-status`는 역할 모델이 없어 모든 인증 사용자에게 열려 있음
- CSP의 `unsafe-inline` 제거는 nonce 또는 외부 스크립트 이전이 필요한 후속 작업
- 개발 종료 시 Vertica JDBC driver 강제 deregistration warning이 1회 발생함. 전체 프로세스 재시작에는 영향이 없지만 hot redeploy 정리 후보
- 공유 `CATALINA_HOME=/opt/tomcat`의 bin과 lib는 계속 읽기 전용으로만 사용해야 함

현재 신뢰도는 92퍼센트다. 남은 신뢰도 차이는 실제 인증 흐름, oversized multipart, DB 감사 로그를 의도적으로 실행 또는 조회하지 않은 데서 온다.
