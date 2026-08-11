# 1단계 — 저장소·빌드·의존성·배포 기준선 감사

> 후속 상태: Java 22 JspC와 반복 빌드 기준선 조치를 완료했다. 실제 변경·검증·롤백·commit 계획은 [기준선 개선 조치 결과](01-build-deployment-baseline-remediation.md)를 따른다.

## 결론

표준 Gradle WAR 빌드와 개발 배포본은 일치한다. Java 컴파일, 테스트, WAR allowlist와 재현성은 안정적이다. 가장 큰 위험은 빌드 자체가 아니라 **현재 배포 상태를 원격 Git에서 재구성할 수 없는 기준선 상태**다.

- 건강도: **83/100**
- P0: 없음
- 릴리스 판단: Git 기준선 확정 전까지 NO-GO

## 기준선

| 항목 | 결과 |
| --- | --- |
| 브랜치 | `develop` |
| HEAD | `21b06b55150c` |
| 원격 차이 | `origin/develop`보다 7커밋 앞섬 |
| 작업 트리 | staged 0, modified 35, deleted 0, untracked 4 |
| Java | Gradle toolchain/release 22 |
| 테스트 | 360개 성공 |
| WAR SHA-256 | `50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9` |
| WAR 엔트리 | 301개 |
| 개발 배포 WAR | 현재 빌드 SHA-256과 일치 |
| exploded app | 소스 webapp과 일치, 런타임 `META-INF/war-tracker`만 추가 |

빌드 정의는 [build.gradle](../../../build.gradle)의 Java 22 toolchain, dependency locking, Vertica JDBC checksum, WAR 라이브러리 allowlist와 금지 파일 검사를 사용한다.

## 빌드·WAR 검증

- `/tmp` 감사 복사본에서 `./gradlew --offline --no-daemon clean build`를 반복 실행했다.
- 두 빌드 모두 성공했고 같은 WAR 해시를 생성했다.
- Java 소스, `db.properties`, `build/`, IDE 설정, Maven metadata, `WEB-INF/classes/db/`는 WAR에 없었다.
- source, Javadoc, test 계열 JAR은 포함되지 않았다.
- 허용 라이브러리는 HikariCP, SLF4J 2, Logback, jBCrypt, JSTL 3, Vertica JDBC로 제한돼 있다.
- Vertica JDBC 로컬 JAR은 고정 SHA-256을 빌드 때 검증한다.

## 발견사항

### P1 — Git 기준선이 개발 배포본을 보존하지 못함

현재 개발 WAR는 로컬 소스와 같지만, 원격 기준선은 7커밋 뒤에 있고 39개 작업 트리 변경을 포함하지 않는다. 서버 장애나 작업 디렉터리 손실 시 원격 저장소만으로 같은 WAR를 복구할 수 없다.

최소 조치:

1. 39개 변경을 기능별로 검토한다.
2. 생성 산출물과 사용자 작업을 분리한다.
3. 승인된 단위로 commit한다.
4. 별도 승인 후 `origin/develop`에 반영한다.

### P2 — JspC Java 버전 하향

JspC는 오류 0으로 성공했지만 Tomcat에 포함된 ECJ가 Java 22를 지원하지 않아 source/target 19로 자동 하향했다. 현재 JSP scriptlet이 없어 실제 동작 위험은 낮지만, Java 22 빌드 계약과 일치하지 않는다.

최소 조치: 공유 Tomcat `lib`를 변경하지 말고 빌드 전용 Java 22 지원 ECJ/JspC 환경을 둔다.

### P2 — 외부 CDN이 재현성 경계 밖에 있음

Font Awesome과 Chart.js는 빌드 산출물에 포함되지 않고 런타임에 CDN에서 내려받는다. CDN 장애나 변경은 동일 WAR에서도 다른 화면 결과를 만들 수 있다.

최소 조치: 버전 고정 파일을 자체 호스팅하거나 SRI와 `crossorigin`을 적용한다.

### P3 — 배포 권한 절차 문서화 필요

당일 개발 로그에 WAR 권한 오류로 인한 과거 배포 실패가 있었으나 이후 정상 배포·기동됐다. 재발 방지를 위해 백업, 소유자, WAR mode, exploded app 정리 순서를 체크리스트로 고정하는 편이 안전하다.

## 운영 무영향

- 운영 `tomcat.service`: active, PID `1012286`
- 운영 WAR SHA-256: `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 운영 `/frog2/login`: HTTP 200
- 이번 감사에서 운영·개발 Tomcat을 재시작하거나 배포하지 않았다.

## 신뢰도와 다음 조건

- 신뢰도: **94%**
- 추가 최소 조건: 작업 트리 검토 후 Git 기준선을 확정하고 같은 commit에서 WAR 해시를 다시 기록한다.
