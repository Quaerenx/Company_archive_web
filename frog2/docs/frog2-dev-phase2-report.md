# frog2 개발 2차 코드베이스 개선 결과

작업일: 2026-07-20
적용 범위: 개발 코드베이스와 `tomcat-dev.service`만
운영 적용: 없음

## 결과 요약

- 요청/생성자 경로의 DDL을 제거하고 migration SQL 4개로 분리했다. SQL은 실행하지 않았다.
- `MeetingRecordDAO.getMeetingRecord()`를 순수 SELECT로 만들고 조회수 증가는 별도 메서드로 분리했다.
- `frog2.env=dev`이면 다른 설정과 관계없이 DB read-only가 되며, JDBC guard가 SELECT/CTE 외 SQL을 드라이버 호출 전에 거부한다.
- Gradle 8.10.2, Java 22, 표준 `src/main/*` 구조의 재현 가능한 WAR 빌드를 복구했다.
- 레거시 `WEB-INF/lib`의 불필요·중복 JAR과 소스/Javadoc/test 산출물을 제거하고 runtime JAR을 8개로 고정했다.
- 자료실을 외부 저장소, Servlet multipart, CSRF, 확장자/MIME allowlist, active-content 차단, opaque 저장명, canonical containment, attachment-only 다운로드 구조로 변경했다.
- 새 WAR를 개발 Tomcat 18081에만 배포했다.

## 주요 변경 파일

### DB 무변경

- `src/main/java/com/company/config/ApplicationEnvironment.java`
- `src/main/java/com/company/util/ReadOnlyJdbcGuard.java`
- `src/main/java/com/company/util/DBConnection.java`
- `src/main/java/com/company/model/MaintenanceRecordDAO.java`
- `src/main/java/com/company/model/UserVmHostDAO.java`
- `src/main/java/com/company/model/HostDAO.java`
- `src/main/java/com/company/model/MeetingRecordDAO.java`
- `src/main/java/com/company/controller/MeetingServlet.java`
- `src/main/resources/db/migration/README.md`
- `src/main/resources/db/migration/V20260720_01__create_user_vm_hosts.sql`
- `src/main/resources/db/migration/V20260720_02__create_hosts.sql`
- `src/main/resources/db/migration/V20260720_03__add_hosts_row_color.sql`
- `src/main/resources/db/migration/V20260720_04__rename_license_usage_pct.sql`

### 빌드

- `.gitignore`
- `build.gradle`
- `settings.gradle`
- `gradle.lockfile`
- `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
- `libs/vertica-jdbc-23.3.0-0.jar`
- `config/README.md`
- `config/db.properties.sample`
- Java 소스: `src/main/java`
- 배포 제외 도구: `src/tools/java`
- 리소스: `src/main/resources`
- 웹 리소스: `src/main/webapp`

혼합 인코딩을 UTF-8로 정상화한 파일은 `HostDAO.java`, `MeetingCommentDAO.java`,
`MeetingRecordDAO.java`, `VerticaEosDAO.java`, `CustomerDetailDAO.java`다. 코드 토큰,
문자열과 SQL은 유지했다. `UpdatePasswords.java`는 배포 대상에서 제외된 tools source set에서
컴파일되며 도달 불가능한 catch 오류만 바로잡았다.

### 파일 자료실

- `src/main/java/com/company/filerepo/FileRepositoryConfig.java`
- `src/main/java/com/company/filerepo/FileRepositoryCsrf.java`
- `src/main/java/com/company/filerepo/FileRepositoryEntry.java`
- `src/main/java/com/company/filerepo/FileRepositoryException.java`
- `src/main/java/com/company/filerepo/FileRepositoryFilePolicy.java`
- `src/main/java/com/company/filerepo/FileRepositoryJson.java`
- `src/main/java/com/company/filerepo/FileRepositoryListing.java`
- `src/main/java/com/company/filerepo/FileRepositoryPathPolicy.java`
- `src/main/java/com/company/filerepo/FileRepositoryService.java`
- `src/main/java/com/company/controller/FileRepositoryServlet.java`
- `src/main/java/com/company/controller/FileRepositoryUploadServlet.java`
- `src/main/java/com/company/controller/FileRepositoryDownloadServlet.java`
- `src/main/java/com/company/filter/AuthFilter.java`
- `src/main/webapp/WEB-INF/web.xml`
- `src/main/webapp/WEB-INF/views/filerepo/list.jsp`
- `src/main/webapp/WEB-INF/views/filerepo/upload.jsp`

### 테스트와 운영 문서

- `src/test/java/com/company/config/ApplicationEnvironmentTest.java`
- `src/test/java/com/company/model/MeetingRecordDAOReadOnlyTest.java`
- `src/test/java/com/company/util/ReadOnlyJdbcGuardTest.java`
- `src/test/java/com/company/RuntimeClassLoadingTest.java`
- `src/test/java/com/company/filerepo/*Test.java`
- `docs/frog2-dev-phase2-rollback.md`

## 제거 또는 백업 이동

- 레거시 `WEB-INF/classes`의 `.java/.class/db.properties`: `legacy-web-inf-classes/`
- 중복 `build/classes`: `legacy-build/`
- Eclipse와 레거시 Maven/WAR metadata: `removed-metadata/`
- 직접 처리 JSP 4개: `legacy-filerepo-jsp/`
  - `filerepo_downlist.jsp`
  - `filerepo_download.jsp`
  - `filerepo_upload.jsp`
  - `filerepo_uploadProcess.jsp`
- 백업 기준 경로: `/opt/frog2-dev/backups/source-layout-20260720_184535`

1차로 제거한 source/Javadoc/test/example JAR 20개:

```text
commons-fileupload2-core-2.0.0-M2-javadoc.jar
commons-fileupload2-core-2.0.0-M2-sources.jar
commons-fileupload2-core-2.0.0-M2-test-sources.jar
commons-fileupload2-core-2.0.0-M2-tests.jar
commons-fileupload2-javax-2.0.0-M2-javadoc.jar
commons-fileupload2-javax-2.0.0-M2-sources.jar
commons-fileupload2-javax-2.0.0-M2-test-sources.jar
commons-fileupload2-javax-2.0.0-M2-tests.jar
commons-fileupload2-portlet-2.0.0-M2-javadoc.jar
commons-fileupload2-portlet-2.0.0-M2-sources.jar
commons-fileupload2-portlet-2.0.0-M2-test-sources.jar
commons-fileupload2-portlet-2.0.0-M2-tests.jar
commons-io-2.19.0-javadoc.jar
commons-io-2.19.0-sources.jar
commons-io-2.19.0-test-sources.jar
commons-io-2.19.0-tests.jar
jbcrypt-0.4-javadoc.jar
jbcrypt-0.4-sources.jar
poi-examples-5.2.3.jar
poi-javadoc-5.2.3.jar
```

2차로 레거시 `WEB-INF/lib`에서 백업 이동한 JAR 39개:

```text
HikariCP-5.1.0.jar
SparseBitSet-1.2.jar
commons-codec-1.15.jar
commons-collections4-4.4.jar
commons-compress-1.21.jar
commons-fileupload2-core-2.0.0-M2.jar
commons-fileupload2-jakarta-servlet6-2.0.0-M3.jar
commons-fileupload2-javax-2.0.0-M2.jar
commons-fileupload2-portlet-2.0.0-M2.jar
commons-io-2.11.0.jar
commons-io-2.16.1.jar
commons-io-2.19.0.jar
commons-logging-1.2.jar
commons-math3-3.6.1.jar
cos.jar
curvesapi-1.07.jar
gson-2.10.1.jar
jakarta.activation-2.0.1.jar
jakarta.el-api-5.0.0.jar
jakarta.servlet.jsp.jstl-2.0.0.jar
jakarta.servlet.jsp.jstl-3.0.1.jar
jakarta.servlet.jsp.jstl-api-2.0.0.jar
jakarta.servlet.jsp.jstl-api-3.0.0.jar
jakarta.xml.bind-api-3.0.1.jar
jbcrypt-0.4.jar
json-20230227.jar
json-simple-1.1.1.jar
log4j-api-2.18.0.jar
logback-classic-1.4.14.jar
logback-core-1.4.14.jar
poi-5.2.3.jar
poi-excelant-5.2.3.jar
poi-ooxml-5.2.3.jar
poi-ooxml-full-5.2.3.jar
poi-ooxml-lite-5.2.3.jar
poi-scratchpad-5.2.3.jar
slf4j-api-1.7.36.jar
slf4j-api-2.0.9.jar
xmlbeans-5.1.1.jar
```

위 목록에는 수동 배치본을 제거한 뒤 Gradle로 다시 공급하는 runtime 라이브러리도 포함된다.
최종 WAR에 유지한 8개는 HikariCP, JSTL API/impl, jbcrypt, SLF4J, Logback
classic/core, Vertica JDBC다. Servlet/JSP API는 Tomcat 제공 `compileOnly`다.

## 실행하지 않은 migration

- `V20260720_01__create_user_vm_hosts.sql`
- `V20260720_02__create_hosts.sql`
- `V20260720_03__add_hosts_row_color.sql`
- `V20260720_04__rename_license_usage_pct.sql`

DDL, DML, 비밀번호 migration utility는 실행하지 않았다.

## 검증

- Java 22 `clean check`: 최종 2회 연속 성공
- 단위 테스트: 24개, failures 0, errors 0
- tools source set 포함 전체 Java 컴파일 성공
- runtime class-loading test 성공
- JspC: JSP/JSPF/tag 34개, 생성 class 50개, errors 0
- `git diff --check`: 성공
- WAR allowlist task: 성공
- WAR SHA-256: `d89440526c89231717e59046efa10f35b4ac90d0e44240021d61f1a6de5e1afb`
- WAR 크기: 6,025,596 bytes (배포 전 62,862,464 bytes 대비 약 90.4% 감소)
- WAR에 source/Javadoc/test JAR, Java source, IDE/build metadata, `db.properties`,
  `WEB-INF/classes/db` 없음
- 보존 디자인 3파일은 소스와 배포본 모두 작업 전 SHA-256과 동일

## 개발 배포

- 개발 PID: `2540280`
- 개발 포트: 18081
- 개발 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- 개발 설정 drop-in: `/etc/systemd/system/tomcat-dev.service.d/20-frog2-safety.conf`
- 활성 JVM 설정: `frog2.env=dev`, `frog2.readOnly=true`, 외부 DB config,
  `/opt/frog2-dev/data/files`
- 외부 파일 저장소 권한: `tomcat:tomcat`, 0750
- login GET 200, 정적 CSS GET 200, 비인증 protected GET 302, 업로드/다운로드 API 401 JSON
- 로그인 POST, 인증된 DML 가능 요청, 실제 업로드는 실행하지 않음
- 현재 개발 저장소는 비어 있음
- Tomcat 로그: WAR 배포 719 ms, 서버 시작 758 ms, JSP/class/linkage 오류 없음

배포 백업은 `/opt/frog2-dev/backups/deploy-20260720_194005`에 있다.

## 운영 무영향

- 운영 PID: `1012286`로 작업 전후 동일
- 운영 ActiveEnterTimestamp: `2026-06-30 10:18:40 KST`
- 운영 WAR SHA-256: `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 운영 8080 login GET: 200
- 운영 login 본문 SHA-256: `18018db317d0a5b4cd00bb689e3e005ea0523f63d432e74dd47fdcc6fbd8a32a`
- 위 PID, WAR, 상태, 응답 해시는 작업 전후 동일

## 남은 위험과 3차 후보

1. 공유 DB의 감사 로그를 조회하지 않았으므로 무쓰기 보장은 코드 guard, Hikari read-only,
   단위 테스트와 미실행 명령 증거에 기반한다. DB 감사 계층에서의 독립 확인이 최종 보강점이다.
2. 안전 제약 때문에 배포 서버에서 인증된 업로드/다운로드 end-to-end 테스트를 하지 않았다.
   임시 인증 provider 또는 격리된 통합 테스트 프로필로 확인해야 한다.
3. 기존 운영 `/files` 자료는 이동하지 않았고 새 저장소로 자동 노출되지 않는다. read-only inventory,
   checksum manifest, dry-run import, 승인된 전환 순서가 별도 필요하다.
4. Tomcat 종료 시 Vertica JDBC driver 강제 deregistration 경고가 남는다. listener에서 명시적으로
   deregister하는 정리 작업을 검토한다.
5. 기존 URL 호환 mapping은 디자인 파일을 보존하기 위해 남겼다. 링크 전환과 사용 로그 확인 후
   `filerepo_*.jsp` alias를 deprecate할 수 있다.
6. `src/tools/java`의 비밀번호 migration utility는 운영 절차에서 사용 여부를 확인한 뒤 별도
   관리 저장소로 옮기거나 deprecate할 수 있다.

현재 신뢰도는 93%다. 최소 추가 검증은 격리 인증으로 업로드→목록→attachment 다운로드 1회,
그리고 공유 DB 감사 로그에서 작업 시간대 DDL/DML 0건 확인이다.

## 2026-07-21 운영 디자인 기준 보정

최초 2차 배포에서 소스의 미커밋 디자인 실험본을 보존한 결과, 운영과 달리
`linear_refinement.css`가 개발 화면에 적용됐다. 운영 배포본과 2차 배포 직전 개발
배포본은 동일한 디자인이었으므로 기능 코드와 파일 자료실 재설계는 유지하고, 아래
디자인 자산만 운영 배포본 기준으로 동기화했다.

- `WEB-INF/includes/header_nav.jspf` SHA-256:
  `ed41736a9e68ec2bcaea49f1e61658dcf720a7a141476960f639b6f91ba8aa5e`
- `login.jsp` SHA-256:
  `7eae660ace8a731ecc62051cd8ce54b0dea1417f67636573ae7c719c3b23595d`
- `resources/css/pages/customers.css` SHA-256:
  `63b4f6303c244252ac6bf79ce6159459ad15aa7e02009965436aff80a416ddee`
- 실험 CSS는 삭제하지 않고
  `/opt/frog2-dev/backups/design-sync-20260721_132134/linear_refinement.css.archived`로 이동했다.
- 보정 전 WAR, exploded app, 소스 파일과 stale JSP work cache는
  `/opt/frog2-dev/backups/design-sync-20260721_132134`에 보존했다.
- 보정 WAR SHA-256:
  `2a2bc9fc188504dbec278dc9fa432c19c3a330154b17559af880e5d6bc4bf8e1`
- 현재 개발 PID: `3241970`
- Java 22 오프라인 `clean check` 2회 연속 성공, 단위 테스트 24개 성공,
  JspC 34개 입력/50개 class/0 errors, `git diff --check` 성공
- 로그인 GET 200이며 본문 SHA-256은 운영과 같은
  `18018db317d0a5b4cd00bb689e3e005ea0523f63d432e74dd47fdcc6fbd8a32a`다.
- 실제 로그인 CSS 3개는 200, 실험 CSS는 404, 비인증 dashboard GET은
  `/frog2/login`으로 302다.
- 최초 재기동 때 기존 Jasper 산출물이 재사용돼 stale 화면이 남았다. 개발 Tomcat만
  다시 중지하고 work cache를 백업 이동한 뒤 재기동했으며 새 cache에는 실험 참조가 없다.
- 개발 로그에 `ReadOnly: true`가 확인됐고 JSP compile/class/linkage 오류는 없다.
- DB DDL/DML, 인증 POST, 업로드/다운로드 데이터 변경 요청은 실행하지 않았다.
- 운영 PID `1012286`, 운영 WAR 해시, 8080 로그인 상태와 본문 해시는 보정 전후 동일하다.

롤백 절차는 `docs/frog2-dev-design-sync-rollback.md`에 기록했다.
