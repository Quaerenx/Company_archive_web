# frog2 개발 코드 품질 개선 최종 보고

작업일: 2026-07-21~2026-07-22
적용 범위: `/opt/frog2-dev/repo/frog2`, `tomcat-dev.service`
운영 적용: 없음
DB migration·DDL·DML 실행: 없음

## 결과 요약

- Stage 0~6을 순서대로 완료하고 각 단계에서 Java 테스트, WAR 및 JspC를 확인했다.
- 상태 변경 요청에 공통 CSRF를 적용하고 GET 로그아웃을 제거했으며 출력 인코딩,
  비밀번호 정책, 세션·오류 계약을 보강했다.
- 고객 모듈의 조회·명령·매핑·환경별 상세 책임을 분리하고 기존 URL과 공개 동작을
  characterization test로 고정했다.
- CSS 토큰·공통 컴포넌트와 페이지별 CSS/JS를 분리했다. 승인된 `header_nav` 구조
  변경은 시각 기준을 유지하면서 canonical 자료실 링크를 사용한다.
- 중복 count/목록 쿼리, 요청별 schema metadata 검사, `SELECT *`, lenient 날짜 파싱과
  라이선스·메뉴·세션 사용자 중복 구현을 줄였다.
- 정적 참조와 집계 access log로 미사용이 검증된 route, 클래스, 메서드와 위험한 CLI
  도구를 제거했다. 사용 흔적이 있는 호환 URL은 유지했다.
- 새 production dependency를 추가하지 않았고 커밋, 브랜치, push, PR을 만들지 않았다.

## Stage 0 — 기준선과 보호 대상

- 저장소: branch `develop`, HEAD `7156152f993167e94da480663d2882a4883ae420`
- 작업 전 개발 PID: `3241970`
- 작업 전 개발 WAR SHA-256:
  `2a2bc9fc188504dbec278dc9fa432c19c3a330154b17559af880e5d6bc4bf8e1`
- 운영 PID: `1012286`
- 운영 WAR SHA-256:
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 운영 `server.xml` SHA-256:
  `34afc0a0f9d78660c5ded03b1654b9a24204378495928baf909b6238ac3ec47a`
- 개발 JVM에서 `frog2.env=dev`, `frog2.readOnly=true`,
  `frog2.fileRepoRoot=/opt/frog2-dev/data/files`를 확인했다.
- 보호 파일 백업:
  `/opt/frog2-dev/backups/code-quality-20260721_144700/protected-design`

## Stage 1 — 보안과 즉시 기능 결함

주요 변경 파일:

- `src/main/java/com/company/security/CsrfToken.java`
- `src/main/java/com/company/security/CsrfFilter.java`
- `src/main/webapp/WEB-INF/includes/csrf_input.jspf`
- 상태 변경 controller와 관련 JSP, `WEB-INF/web.xml`
- `src/main/java/com/company/controller/LogoutServlet.java`
- `src/main/java/com/company/security/PasswordPolicy.java`
- `src/main/java/com/company/controller/LoginServlet.java`
- `src/main/java/com/company/controller/MyPageServlet.java`
- `src/main/java/com/company/controller/PoolMonitorServlet.java`
- `src/main/webapp/error/500.jsp`

해결 내용:

- 고객, 점검, 회의록·댓글, 마이페이지, VM host와 파일 자료실 상태 변경 요청에
  공통 CSRF 검증을 적용했다. HTML/JSON 403 계약을 분리했다.
- 로그아웃을 POST+CSRF로 전환하고 GET/HEAD/OPTIONS가 상태를 바꾸지 않게 했다.
- JSP 사용자 데이터를 컨텍스트에 맞게 인코딩하고 inline handler의 서버 데이터
  삽입을 `data-*` 기반으로 교체했다.
- 내부 예외·catalog를 사용자에게 노출하지 않으며 새 비밀번호의 null, 일치, 길이,
  현재 비밀번호 동일 여부를 서버에서 검증한다.
- 세션 timeout 정책을 하나로 통합했다. Secure 쿠키의 강제 적용은 실제 TLS·프록시
  구성이 확정되지 않아 보류했다.

## Stage 2 — 오류 계약과 응답 일관성

주요 변경 파일:

- `src/main/java/com/company/model/DataAccessException.java`
- `src/main/java/com/company/model/JdbcConnectionProvider.java`
- `src/main/java/com/company/filter/ApplicationExceptionFilter.java`
- `src/main/java/com/company/web/JsonResponse.java`
- `src/main/java/com/company/controller/FlashMessage.java`
- DAO와 Login/MyPage/Customers controller

해결 내용:

- DB 장애, 결과 없음, 입력 오류, 권한 오류와 개발 read-only 거부를 구분한다.
- 로그인 DB 장애를 자격 증명 오류로 숨기지 않고 read-only 저장 시도는 HTTP 409로
  명시한다.
- HTML/JSON status, Content-Type과 오류 body 계약을 통합했다.
- redirect 후 사라지던 월별 응대 메시지를 1회성 flash message로 교체했다.
- `printStackTrace`, `System.out`, `System.err`와 민감 객체 로그를 제거했다.

## Stage 3 — 고객 관리 구조 개선

주요 변경 파일:

- `CustomerQueryController.java`, `CustomerCommandController.java`
- `CustomerCommandService.java`, `CustomerRequestMapper.java`
- `CustomerJsonResponse.java`, `CustomerDetailQueryService.java`
- `CustomerEnvironment.java`, `CustomerDetailEnvironment.java`
- `CustomerDetailDAO.java`, `CustomersServlet.java`
- `src/main/webapp/customers/_detail_sections.jspf`

해결 내용:

- 과대화된 servlet의 목록·상세 조회, 명령, 서비스, 49개 폼 필드 매핑과 JSON 응답을
  작은 책임으로 분리했다.
- prod/stg/dev 환경 입력을 enum allowlist로 제한해 임의 테이블명 경로를 차단했다.
- 환경별 SQL·binder·row mapper 중복을 통합하고 save/update가 같은 연결과 트랜잭션을
  사용하게 했다.
- 기존 route, redirect, binder 순서와 공개 API를 compatibility test와 `javap`로
  확인했다. 실제 DB DML은 사용하지 않았다.

## Stage 4 — 디자인·레이아웃 통합

주요 변경 파일:

- `resources/css/tokens.css`, `base.css`, `components.css`, `utilities.css`
- `resources/css/pages/*.css`
- `resources/js/pages/*.js`, `resources/js/header_nav.js`
- `WEB-INF/includes/header_nav.jspf`, 공통 header/footer와 해당 JSP

해결 내용:

- 중복 `:root` 토큰과 공통 selector를 분리하고 페이지 CSS를 namespace로 제한했다.
- page CSS/JS를 `<head>` 및 페이지 슬롯으로 옮기고 header 내부 동적 CSS/favicon
  보정을 제거했다.
- 사용자가 승인한 `header_nav.jspf` 구조 변경으로 JavaScript를 외부 파일로 옮기고
  자료실 내부 링크를 `/file-repository`로 통일했다.
- 현재 header CSS SHA-256은
  `6d267e9fc4196b3987eff4a02e3db976856a7594329772cb0d839f67c9c1330a`로
  시각 기준과 같다. canonical 링크를 이전 alias로 정규화한 markup SHA-256도
  변경 전과 같은 `bc55191913b6df4c570b42d865fe25caba2ac1e90794cdb6fa2001dcbb6bf038`다.
- `login.jsp` SHA-256은 작업 전후 동일한
  `a38038f9e7a9e44c9006af74c7c51715e95a3b37464f9f4c2f66fbc594fe9e41`다.
- `linear_refinement.css`는 작업 시작 시 소스에 없었고 새로 만들거나 덮어쓰지 않았다.
  이전 실험본은 기존 디자인 보정 백업에 계속 보존된다.

## Stage 5 — 조회와 유지보수 성능

주요 변경 파일:

- `DashboardServlet.java`, `CustomerDAO.java`, `CustomerCounts.java`
- `CustomerDetailDAO.java`, `CustomerDetailSet.java`, `CustomerDetailQueryService.java`
- `SchemaCapabilityCache.java`, `UserDAO.java`, `MaintenanceRecordDAO.java`
- `StrictDateParser.java`, `LicenseSummaryFormatter.java`
- `DashboardMenuProvider.java`, `SessionUser.java`
- `docs/stage5-pagination-plan.md`

해결 내용:

- 대시보드 VM host 목록과 count를 한 번의 목록 조회로 통합했다.
- 고객 목록 total/maintenance count를 조건부 집계 한 번으로 통합했다.
- 고객 상세 prod/stg/dev 조회를 한 연결을 쓰는 명시적 조회 서비스로 묶었다.
- User/Maintenance optional-column metadata를 application-level immutable cache로 바꿨다.
- production DAO의 `SELECT *`를 필요한 명시 컬럼으로 교체했다.
- 날짜를 strict `LocalDate`/`LocalDateTime` 파서로 통일하고 라이선스 표시, 대시보드
  메뉴와 세션 사용자 확인 중복을 공통화했다.
- UI 동작을 바꾸지 않고 대형 목록의 stable sort·query cap·pagination 도입 순서를
  문서화했다. DB index와 schema는 변경하지 않았다.

## Stage 6 — 폐기와 레거시 정리

제거 완료:

- `/dashboard2` mapping과 누락 JSP 전달 분기
- `customers?view=support` 누락 JSP 전달 분기
- `HostDAO.java`, `HostDTO.java`, `VerticaEosDTO.java`
- `CustomerDetailDAO.deleteCustomerDetail()`
- `MonthlyCustomerResponseDAO.getResponseById()`
- `MaintenanceRecordDAO.getAllMaintenanceRecords()`
- 무인자 `MaintenanceRecordDAO.getMaintenanceRecordsByInspector()`
- `src/tools`의 `UpdatePasswords`, `UpdateAllPasswords`, `ConnectionPoolTest`, `QuickTest`
- Gradle tools source set 및 관련 compile/check wiring
- CSP의 미사용 `code.jquery.com` origin
- patch 임시 `.orig/.rej`와 JspC 생성 로그 디렉터리

유지 또는 보류:

- 구형 `/filerepo/*.jsp` 호환 URL: 집계 로그에서 운영 16회, 개발 3회 사용 흔적이 있어
  mapping을 유지했다. 내부 링크만 canonical `/file-repository`로 변경했다.
- `/admin/pool-status`: 운영 access log에서 1회 사용되어 유지했다.
- 문자열 인자 `getMaintenanceRecordsByInspector(String)`: 실제 호출이 있어 유지했다.
- `VerticaEosDAO`: 정적 호출 경로가 있어 DTO만 제거하고 DAO는 유지했다.

검토한 access log 범위는 운영 2026-04-26~2026-07-22, 개발 2026-07-03,
2026-07-20~2026-07-21이다. IP·사용자·원문 요청은 출력하지 않고 route별 합계만 사용했다.

## 최종 빌드와 검증

- `./gradlew clean test check war --offline`: 2회 연속 `BUILD SUCCESSFUL`
- Java 22 전체 컴파일: 성공
- 단위/계약 테스트: 115개, failures 0, errors 0, skipped 0
- JspC: errors 0
- runtime class-loading 및 WAR allowlist: 성공
- `git diff --check`: 성공
- 최종 WAR: 233 entries, 6,059,711 bytes
- 최종 WAR SHA-256:
  `6e765f6c25810f41b432cef4e363581e84dd0aaabcd06a283f2de947fb5d25bb`
- WAR runtime JAR 8개: Vertica JDBC, HikariCP, jbcrypt, SLF4J 2.0,
  Logback classic/core, JSTL API/runtime
- WAR에 Java source, `db.properties`, `build/`, IDE metadata, tools class,
  source/Javadoc/test JAR이 없음을 확인했다.
- production Java에서 `SELECT *`, `System.out`, `System.err`, `printStackTrace`,
  TODO/FIXME가 없음을 정적 확인했다.

표준 구조 이전이 아직 커밋되지 않아 Git은 기존 경로 삭제와 새 `src/` 트리를 별도
변경으로 표시한다. 이는 이번 작업에서 코드를 대량 삭제한 것이 아니라 기존 미커밋
구조 이전을 포함한 상태이며, 별도 요청이 없어 커밋하지 않았다.

## 개발 배포 결과

- 배포 백업:
  `/opt/frog2-dev/backups/code-quality-final-20260722_114325`
- 백업 파일: `frog2.war.before`, `frog2.exploded.before/`
- 개발 PID: `38597`
- 개발 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- 개발 URL(서버 로컬 검증): `http://127.0.0.1:18081/frog2/login`
- JVM: `frog2.env=dev`, `frog2.readOnly=true`,
  `frog2.fileRepoRoot=/opt/frog2-dev/data/files`
- 로그인 GET 200, header CSS/JS GET 200
- 비인증 `/frog2/dashboard` GET은 `/frog2/login`으로 302
- 로그인 응답 SHA-256:
  `18018db317d0a5b4cd00bb689e3e005ea0523f63d432e74dd47fdcc6fbd8a32a`
- 최근 개발 로그에서 `JasperException`, `ClassNotFoundException`,
  `NoSuchMethodError`, `LinkageError`, SEVERE와 thread exception 0건
- 인증 POST, 실제 업로드와 DB 쓰기 가능 경로는 호출하지 않았다.

### 로그인 CSRF 배포 캐시 보정

최초 배포 후 실제 로그인 POST가 403으로 거부되는 현상을 확인했다. 새 WAR의
`login.jsp`에는 CSRF hidden input이 있었지만, 재현 가능한 WAR와 기존 Jasper cache의
mtime이 모두 `1980-02-01`이라 Tomcat이 CSRF 코드가 없는 이전 `login_jsp.class`를
재사용한 것이 원인이었다.

- 개발 Tomcat만 중지했다.
- 기존 cache를 삭제하지 않고
  `/opt/frog2-dev/backups/code-quality-final-20260722_114325/tomcat-work.stale-before-csrf-fix`
  로 이동했다.
- 개발 Tomcat만 다시 기동했다.
- 실제 로그인 GET HTML에서 `_csrf` hidden input 1개, URL-safe 43자 token과 동일
  session에서의 token 유지를 확인했다. token 값은 출력하지 않았다.
- 인증 POST는 재실행하지 않았고 개발 치명 로그는 0건이다.

재배포 시 `/opt/tomcat-dev/work/Catalina/localhost/frog2`를 기존 배포와 함께
격리하고, 로그인 GET의 CSRF hidden input이 비어 있지 않은지 smoke check해야 한다.

롤백 절차는 `docs/frog2-dev-code-quality-rollback-20260722.md`에 실제 경로와
배포 전후 해시를 기록했다.

## 운영 무영향

- 운영 service: active, PID `1012286`로 작업 전후 동일
- 운영 시작 시각: `2026-06-30 10:18:39 KST`로 동일
- 운영 WAR SHA-256:
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`로 동일
- 운영 `server.xml` SHA-256:
  `34afc0a0f9d78660c5ded03b1654b9a24204378495928baf909b6238ac3ec47a`로 동일
- 운영 8080 login GET 200, 응답 SHA-256
  `18018db317d0a5b4cd00bb689e3e005ea0523f63d432e74dd47fdcc6fbd8a32a`로 동일
- 운영 Tomcat에 stop/start/restart/deploy 명령을 실행하지 않았다.

## DB 쓰기 0건 근거

- 작업 중 DB migration, DDL, DML, password migration 도구를 실행하지 않았다.
- DML 코드 경로는 fake/mock JDBC 또는 로컬 임시 파일 저장소 테스트만 사용했다.
- 개발 JVM의 `frog2.readOnly=true`와 `frog2.env=dev`를 배포 전후 확인했다.
- `ReadOnlyJdbcGuard`가 SELECT/CTE 외 명령을 JDBC driver 호출 전에 차단하는 테스트가
  최종 115개 테스트에 포함되어 통과했다.
- 배포 확인은 비인증 GET과 정적 자산만 사용했다.

실행하지 않은 migration:

- `V20260720_01__create_user_vm_hosts.sql`
- `V20260720_02__create_hosts.sql`
- `V20260720_03__add_hosts_row_color.sql`
- `V20260720_04__rename_license_usage_pct.sql`

`db/legacy/add_department_column.sql`과
`db/legacy/create_monthly_customer_response.sql`도 실행하지 않았다. DB 감사 로그를
독립 조회하지 않았으므로 “0건”은 실행 명령, 코드 guard, JVM 설정과 mock 검증에
근거하며 DB 감사 계층 확인은 아래의 추가 검증 항목으로 남긴다.

## 남은 위험과 다음 우선순위

1. 격리 인증 provider 또는 전용 임시 DB로 고객·점검·회의록·마이페이지의 상태 변경
   E2E와 자료실 upload→list→attachment download를 1회 검증한다.
2. 인증 화면은 세션 부재로 브라우저 screenshot·console 검증을 수행하지 않았다.
   disposable 인증 세션으로 시각 회귀와 CSP console을 확인한다.
3. 공유 DB 감사 로그를 읽기 전용으로 확인해 작업 시간대 DDL/DML 0건을 독립 입증한다.
4. `docs/stage5-pagination-plan.md` 순서대로 query cap과 pagination을 구현한다.
5. 충분한 access-log 관찰 기간 뒤 `/filerepo/*.jsp`와 `/admin/pool-status`를 다시
   deprecate 판단한다.
6. Tomcat 종료 시 Vertica JDBC driver deregistration 경고를 listener에서 명시적으로
   정리할지 검토한다.
7. 실제 TLS·프록시 종료 구성을 확인한 뒤 Secure/SameSite 쿠키 정책을 확정한다.

현재 신뢰도는 94%다. 테스트·빌드·정적 검토와 개발 배포 증거는 충분하지만,
인증된 브라우저 E2E와 독립 DB 감사 확인이 남아 있어 production-ready로 단정하지 않는다.
