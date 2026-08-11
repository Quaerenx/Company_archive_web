# 1단계 조치 결과 — 저장소·빌드·의존성·배포 기준선

## 결론

개발 서버의 현재 소스는 Java 22 기준으로 반복 빌드할 수 있고, 같은 입력에서 바이트 단위로 동일한 WAR를 만든다. 새 Gradle 검증은 전체 JSP/tag를 Tomcat Jasper로 Java 소스로 만든 뒤 JDK 22 `javac --release 22`로 컴파일한다. 기존 ECJ가 Java 22 요청을 Java 19로 조용히 낮추던 경로는 표준 `check`에서 사용하지 않는다.

- 조치 상태: **완료**
- clean build: **2회 연속 성공**
- 단위 테스트: **361개, 실패 0**
- JspC: **생성 Java 38개, class 63개, 오류 0**
- JSP class version: **major 66(Java 22)**
- WAR SHA-256: `50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9`
- 개발 배포 WAR: 새 빌드 WAR와 동일
- 개발 exploded app: 런타임 `META-INF/war-tracker`를 제외하면 내용 차이 0
- DB 접속·배포·재시작·Git 쓰기: **수행하지 않음**

## 이번 작업의 변경 범위

### 변경한 빌드·검증 파일

1. `build.gradle`
   - 표준 `check`가 Java 22 JspC 검증을 반드시 실행하도록 연결했다.
2. `gradle/jspc-java22.gradle`
   - Tomcat Jasper 10.1.41로 JSP Java 소스만 생성한다.
   - JDK 22 toolchain과 `--release 22`로 생성 소스를 컴파일한다.
   - Tomcat/Jasper 입력이 없거나 버전이 다르면 fail-closed로 중단한다.
3. `src/test/java/com/company/buildcontract/JspcJava22BuildContractTest.java`
   - ECJ `-compile` 재도입, Java 22 release 누락, Jasper 버전 계약 누락을 정적으로 막는다.

### 변경한 문서

- 이 조치 결과 문서
- `docs/audits/2026-08-10/README.md`의 공통 검증 수치와 링크

### 변경하지 않은 범위

- 기존 업무 Java, JSP/JSPF/tag, CSS, JavaScript 및 사용자 테스트
- `gradle.lockfile`, Gradle wrapper, Vertica JDBC JAR
- 개발·운영 Tomcat 설정과 공유 `/opt/tomcat/bin`, `/opt/tomcat/lib`
- 개발·운영 WAR와 exploded app
- DB·스키마·데이터
- Git index, commit, branch, remote

작업 전 `/tmp` snapshot과 checksum dry-run으로 비교했으며 위 빌드·검증 파일을 제외한 기존 사용자 파일의 예상하지 못한 변경은 0개였다.

## 마일스톤 1 — 기준선 보존

| 항목 | 결과 |
| --- | --- |
| branch | `develop` |
| HEAD | `21b06b55150c48ed003b9387d81ce5d26c745d8a` |
| 원격 차이 | `origin/develop...develop = 0 7`, 로컬이 7커밋 앞섬 |
| staged/deleted | 0 / 0 |
| 작업 시작 기준 | 사용자 변경 modified 35개, untracked 4개 그룹 |
| 코드 조치 후 | modified 36개, 개별 untracked 18개 |
| 이번 코드 조치가 추가한 차이 | tracked 수정 1개, untracked 소스 2개 |

현재 변경 분류:

| 분류 | 주요 파일군 | 처리 |
| --- | --- | --- |
| 업무 기능·성능 | 파일 자료실 pagination 모델·서비스·화면 | 사용자 작업으로 보존 |
| UI·디자인 | 고객사, 로그인, 정기점검, 표 footer, 공통 token/CSS | 사용자 작업으로 보존 |
| 테스트 | 기존 수정 테스트 12개, 기존 신규 테스트 1개 | 사용자 작업으로 보존 |
| 빌드·설정 | `build.gradle`, 새 JspC script/test | 이번 조치 3개만 추가 |
| 문서 | 감사 7개, 설계·계획 6개 | 기존 산출물로 보존 |
| 생성 산출물 | `build/`, `.gradle/` | ignore 상태, 소스와 분리 |

중복된 `WEB-INF/classes` 소스 트리, 오래된 Java 표준 구조, 추적 중인 build 산출물은 발견하지 못했다. Java 패키지 이름을 `com/company/build`로 두면 저장소의 `**/build/` 규칙에 의해 테스트가 무시되는 충돌은 실제로 재현됐다. 새 계약 테스트는 `com/company/buildcontract`에 두어 Git 추적 대상임을 확인했다.

## 마일스톤 2 — 반복 가능한 빌드

### 고정 입력

- Gradle wrapper: 8.10.2
- wrapper JAR SHA-256: `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`
- Java toolchain/release: 22 / 22
- Vertica JDBC SHA-256: `f3fdb4eae26cebe05fba4c2427ea7b3846dfa73177f5b3ad2bb020ea28721d43`
- WAR: timestamp 미보존, reproducible order, duplicate fail
- 런타임 라이브러리: Gradle lock + WAR allowlist 이중 검증

### 최종 반복 결과

| 실행 | 결과 | 시간 | 테스트 | WAR SHA-256 |
| --- | --- | --- | --- | --- |
| clean build 1 | 성공 | 11초 | 361/361 | `50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9` |
| clean build 2 | 성공 | 11초 | 361/361 | `50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9` |

- WAR 바이트 비교: 동일
- WAR 파일 목록 비교: 동일, 301개
- timestamp 차이: 없음
- 실제 파일 차이: 없음

### WAR allowlist

`WEB-INF/lib`의 허용 JAR은 다음 8개다.

- `HikariCP-5.1.0.jar`
- `jakarta.servlet.jsp.jstl-3.0.1.jar`
- `jakarta.servlet.jsp.jstl-api-3.0.0.jar`
- `jbcrypt-0.4.jar`
- `logback-classic-1.4.14.jar`
- `logback-core-1.4.14.jar`
- `slf4j-api-2.0.9.jar`
- `vertica-jdbc-23.3.0-0.jar`

검사 결과 Java 소스, `db.properties` 변형, `build/`, IDE 설정, source/Javadoc/test JAR, `WEB-INF/classes/db/`, `META-INF/maven/`은 WAR에 없다.

`gradle.lockfile`에는 과거 `tools*Classpath` configuration 이름이 남아 있다. 현재 프로젝트에는 해당 source set이 없고 런타임·WAR 결과에는 영향이 없어 이번 최소 변경에서는 손대지 않았다. 다음 lock 재생성 때 별도 diff로 정리할 P3 후보다.

## 마일스톤 3 — JspC Java 22 정렬

### 원인

공유 Tomcat 10.1.41의 `ecj-4.27.jar`는 Java 19까지만 지원한다. 기존처럼 Jasper에 `-compile -source 22 -target 22`를 주면 실패하지 않고 Java 19로 낮춰 컴파일하므로 빌드 계약을 어긴다.

### 선택지 비교

| 선택지 | 장점 | 단점 | 결정 |
| --- | --- | --- | --- |
| Gradle JspC 전용 최신 ECJ | 한 단계 컴파일 | 새 공급망 의존성·offline cache 필요 | 보류 |
| Jasper 소스 생성 + JDK 22 별도 컴파일 | 새 라이브러리 없이 Java 22 강제, 공유 lib 불변 | 검증 task가 두 단계 | **선택** |
| 현재 Tomcat ECJ와 Java 19 계약 | 가장 단순 | 애플리케이션 Java 22 계약과 불일치 | 기각 |

선택한 구조:

1. Tomcat Jasper 10.1.41이 35개 JSP와 3개 tag에서 Java 소스 38개를 생성한다.
2. 8개 JSPF는 모두 JSP/tag 진입점에서 참조되는 것을 확인했다.
3. JDK 22 `javac --release 22`가 생성 소스를 class 63개로 컴파일한다.
4. 표본 class의 major version 66을 확인했다.
5. Jasper 버전이 10.1.41이 아니면 task가 실패한다. 시험값 0.0.0으로 실행했을 때 의도대로 실패했다.

공유 `/opt/tomcat/lib`에는 JAR을 추가·교체하지 않았다.

## 마일스톤 4 — 개발 배포 대응 관계

| 대상 | 결과 |
| --- | --- |
| 새 빌드 WAR | SHA `50837b...db9` |
| 개발 배포 WAR | SHA `50837b...db9`, 정확히 동일 |
| 개발 exploded app | `war-tracker` 제외 checksum 차이 0 |
| 배포본에만 있는 기능 파일 | 없음 |
| 소스에만 있는 기능 파일 | 없음 |
| 배포/재시작 | 수행하지 않음 |

Tomcat 경계:

- 개발: `CATALINA_HOME=/opt/tomcat`, `CATALINA_BASE=/opt/tomcat-dev`
- 개발 계정: `tomcat-dev:tomcat-dev`
- 운영 계정: `tomcat:tomcat`
- 개발 프로세스 bootstrap classpath: 공유 HOME의 `bootstrap.jar`, `tomcat-juli.jar`
- `tomcat-dev`의 운영 HOME `bin/lib/conf/webapps` 쓰기 권한: 모두 없음

가변 경로인 base, webapps, conf, logs, temp는 분리돼 있고 개발 계정은 운영 HOME을 쓸 수 없다. 다만 실행 바이너리와 공용 라이브러리는 같은 `CATALINA_HOME`을 읽으므로 `/opt/tomcat` 자체를 교체하면 두 인스턴스의 다음 기동에 함께 영향을 줄 수 있다. 이번 작업은 버전 10.1.41을 읽기 전용으로 사용했고 아무 파일도 바꾸지 않았다.

운영 불변 확인:

- `tomcat.service`: active/running, PID `1012286` 유지
- 운영 WAR SHA-256: `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88` 유지
- 운영 `/frog2/login`: HTTP 200
- 개발 `tomcat-dev.service`: active/running, PID `3605261` 유지
- 개발 `/frog2/login`: HTTP 200

## 마일스톤 5 — 권장 commit 단위

아래는 계획일 뿐이다. `git add`, commit, push는 실행하지 않았다. 위에서 아래 순서가 안전하다.

### 1. 빌드·배포 기준선

- `build.gradle`
- `gradle/jspc-java22.gradle`
- `src/test/java/com/company/buildcontract/JspcJava22BuildContractTest.java`

### 2. 데이터·보안

현재 작업 트리에 이 범주로 독립시킬 코드 변경은 없다. 빈 commit을 만들지 말고 향후 데이터·보안 조치는 별도 승인과 별도 commit으로 유지한다.

### 3. 업무 기능·성능 — 자료실 pagination

- `src/main/java/com/company/filerepo/FileRepositoryListing.java`
- `src/main/java/com/company/filerepo/FileRepositoryService.java`
- `src/main/webapp/WEB-INF/views/filerepo/list.jsp`
- `src/main/webapp/resources/css/pages/download.css`
- `src/test/java/com/company/filerepo/FileRepositoryServiceTest.java`

이 commit은 다음 UI foundation commit 뒤에 두거나 `tableFooter.tag` 사용 부분만 patch 단위로 분리해야 한다.

### 4. UI·접근성

#### 4-1. 공통 token·상세 필드·표 footer 기반

- `src/main/webapp/resources/css/tokens.css`
- `src/main/webapp/resources/css/ui-system.css`
- `src/main/webapp/WEB-INF/tags/detailField.tag`
- `src/main/webapp/WEB-INF/tags/tableFooter.tag`
- `src/test/java/com/company/layout/TableFooterViewContractTest.java`
- `src/test/java/com/company/layout/CarbonIconUsageContractTest.java`
- `src/test/java/com/company/layout/MinimalPaletteContractTest.java`

#### 4-2. 고객사 상세 정보 위계

- `src/main/webapp/customers/_detail_sections.jspf`
- `src/main/webapp/customers/customers_detail.jsp`
- `src/main/webapp/resources/css/pages/customer_detail.css`
- `src/test/java/com/company/controller/CustomerDetailViewStructureTest.java`
- `src/test/java/com/company/layout/UiDesignSystemContractTest.java`

#### 4-3. 목록 표 footer 적용

- `src/main/webapp/customers/customers_list.jsp`
- `src/main/webapp/meeting/meeting_list.jsp`
- `src/main/webapp/resources/css/pages/meeting_list_layout.css`
- `src/main/webapp/troubleshooting/troubleshooting_list.jsp`
- `src/main/webapp/mypage/monthly_customer_response.jsp`
- `src/main/webapp/mypage/mypage.jsp`
- `src/main/webapp/vm_hosts/list.jsp`
- `src/test/java/com/company/layout/CustomerPaginationViewContractTest.java`
- `src/test/java/com/company/layout/TroubleshootingViewContractTest.java`

#### 4-4. 로그인 시각·입력 개선

- `src/main/webapp/login.jsp`
- `src/main/webapp/resources/css/login_style.css`
- `src/test/java/com/company/layout/LoginViewContractTest.java`

#### 4-5. 정기점검 이력·차트·대시보드 표시

- `src/main/webapp/maintenance/maintenance_history.jsp`
- `src/main/webapp/resources/css/pages/maintenance_history.css`
- `src/main/webapp/resources/js/pages/maintenance_history.js`
- `src/main/webapp/resources/css/pages/dashboard.css`
- `src/main/webapp/WEB-INF/web.xml`
- `src/test/java/com/company/layout/MaintenanceFormAssetContractTest.java`
- `src/test/java/com/company/layout/DashboardViewContractTest.java`
- `src/test/java/com/company/layout/PageShellContractTest.java`

`tokens.css`에는 로그인과 차트 token이 함께 있으므로 4-1에 먼저 포함해야 4-4와 4-5가 파일 충돌 없이 뒤따른다.

#### 4-6. 인증된 화면 통합 smoke 계약

- `src/test/java/com/company/e2e/DevelopmentServerSmokeTest.java`

로그인·고객사 상세·표 footer가 모두 들어간 뒤 두는 것이 맞다.

### 5. 감사·설계 문서

- `docs/audits/2026-08-10/` 전체
- `docs/superpowers/plans/`의 2026-08-10 문서
- `docs/superpowers/specs/`의 2026-08-10 문서

문서는 코드 commit과 분리한다. commit 승인과 push 승인은 각각 별도로 받는다.

## 롤백 기준

이번 작업은 배포하지 않았으므로 Tomcat/WAR 롤백은 필요 없다. 소스 롤백이 승인되면 다음 3개만 정확히 되돌리면 작업 전 상태가 된다.

1. `build.gradle` 마지막의 `apply from: 'gradle/jspc-java22.gradle'` 두 줄을 제거한다.
2. `gradle/jspc-java22.gradle`을 제거한다.
3. `src/test/java/com/company/buildcontract/JspcJava22BuildContractTest.java`를 제거한다.

기존 dirty 파일에는 `git checkout`, reset, 일괄 restore를 사용하지 않는다. 작업 전 `build.gradle`은 `/tmp/frog2-baseline-20260810.VE8SbL/source/build.gradle`에 현재 세션용 snapshot으로 보존돼 있으나 `/tmp`는 영구 백업으로 간주하지 않는다.

## 남은 위험

1. **P1 — 원격 기준선 미확정**: 로컬은 `origin/develop`보다 7커밋 앞서 있고 dirty 사용자 작업이 남아 있다. 현재 WAR는 서버 로컬에서만 완전히 재현된다.
2. **P2 — 공유 CATALINA_HOME 결합**: 개발은 운영 HOME을 쓸 수 없지만 Tomcat 바이너리/lib upgrade의 영향 범위는 공유된다.
3. **P2 — 외부 CDN**: Font Awesome/Chart.js는 동일 WAR 밖의 런타임 입력이다. 완전한 화면 재현성은 자체 호스팅 또는 SRI 고정이 필요하다.
4. **P3 — lock metadata**: `gradle.lockfile`의 사용하지 않는 `tools*Classpath` 이름은 다음 lock 재생성 때 정리한다.
5. **P3 — JspC TLD scan 정보 로그**: 오류는 아니며 결과에도 영향이 없지만 불필요 JAR scan 최적화 여지가 있다.

## 최종 신뢰도

- 테스트 증거: 40/40
- diff·배포 대응 자체 검토: 29/30
- 논리·환경 점검: 29/30
- 총 신뢰도: **98%**

남은 2%는 아직 Git commit으로 고정되지 않았고 CATALINA_HOME이 물리적으로 공유된다는 운영 경계 때문이다.
