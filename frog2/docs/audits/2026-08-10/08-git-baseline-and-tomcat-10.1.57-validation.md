# Git 기준선 정리 및 Tomcat 10.1.57 격리 검증

- 수행일: 2026-08-11 (Asia/Seoul)
- 저장소: `/opt/frog2-dev/repo/frog2`
- 대상 브랜치: `develop`
- 검증 범위: Git 기준선 동기화, Java 22 빌드/JspC, Tomcat 10.1.57 격리 실행
- 제외 범위: 운영·개발 Tomcat 교체/재시작, 운영 배포, 인증 POST, 파일 업로드, DB DDL/DML

## 1. 결론

기존 사용자 변경 142개 파일을 다섯 개의 검토 가능한 커밋으로 분리했고, 기존 로컬 전용 일곱 커밋과 함께 `origin/develop`에 일반 push로 동기화했다. 강제 push와 PR은 사용하지 않았다.

공식 Apache Tomcat 10.1.57 배포본은 SHA-512와 PGP 서명을 확인한 뒤 `/tmp`의 격리 환경에서 검증했다. Java 22 clean build, JspC, 단위 테스트, WAR 검증과 loopback GET smoke가 모두 통과했다.

단, 실제 운영·개발 서비스가 공유하는 `/opt/tomcat`은 계속 10.1.41이다. 이번 결과는 **10.1.57로 올릴 수 있다는 호환성 검증**이며, 실행 중인 Tomcat의 보안 업데이트를 적용한 것은 아니다.

## 2. Git 기준선

### 작업 전

- 브랜치: `develop`
- HEAD: `21b06b55150c`
- `origin/develop`보다 7커밋 앞선 상태
- 수정·미추적 파일을 펼쳐 확인한 변경 대상: 142개
- 기존 사용자 변경은 삭제·되돌림 없이 보존

### 안전 백업

커밋 전에 다음 위치에 상태와 패치를 보존했다.

- `/opt/frog2-dev/backups/git-baseline-before-20260811-1010/status-before.txt`
- `/opt/frog2-dev/backups/git-baseline-before-20260811-1010/tracked-working-tree.patch`
- `/opt/frog2-dev/backups/git-baseline-before-20260811-1010/tracked-and-untracked-source.tar.gz`
- `/opt/frog2-dev/backups/git-baseline-before-20260811-1010/SHA256SUMS`

Git이 무시하는 비밀 설정과 빌드 산출물은 백업 압축 대상에서 제외했다.

### 생성한 커밋

| 순서 | 커밋 | 쉬운 설명 |
|---:|---|---|
| 1 | `c05e969 build: enforce Java 22 JSP validation` | Java와 JSP가 모두 Java 22 기준으로 검사되게 함 |
| 2 | `f173fbe refactor: harden data ownership and bounded reads` | 데이터 소유권과 무제한 조회 위험을 정리함 |
| 3 | `576c5a7 security: harden file handling and local assets` | 자료실과 자체 호스팅 자산의 보안을 보강함 |
| 4 | `a5bd876 style: improve accessible Archive views` | Archive 화면의 접근성과 공통 UI를 정리함 |
| 5 | `0db79f2 docs: record audit and remediation results` | 감사 및 개선 결과를 문서화함 |

각 커밋 전에 staged diff와 `git diff --cached --check`를 확인했다. 문서의 의미 없는 후행 공백 7건만 제거했고, 관련 없는 파일은 포맷하지 않았다.

### 원격 동기화

- 방식: `develop` → `origin/develop` 일반 push
- 결과: `ebbbc2f..0db79f2`
- force push: 사용하지 않음
- PR: 생성하지 않음

## 3. 반복 빌드 결과

| 항목 | 결과 |
|---|---|
| clean build 1차 | 성공 |
| clean build 2차 | 성공 |
| 테스트 | 408개 성공, 실패 0, 오류 0, 건너뜀 0 |
| JspC | JSP 소스 38개, 생성 class 63개, 오류 0 |
| WAR SHA-256 | `eac9827f90e8a9d7186d5fb29368caa3b95aa974294e5332e4ade77ab2342c9a` |
| 두 WAR byte 비교 | 동일 |

10.1.57 후보를 사용한 추가 clean build도 성공했고, Jasper 10.1.57로 JSP 소스를 생성한 뒤 `javac --release 22`로 컴파일했다. 후보 검증 WAR의 SHA-256도 위 기준값과 동일했다.

## 4. Tomcat 10.1.57 공급망 확인

- 공식 버전: Apache Tomcat 10.1.57, 2026-07-03 빌드
- 다운로드 출처: Apache 공식 배포 서버
- SHA-512: `2fa1866ec647d4222b07ea937a4cc266adee219a1a8870107e72fc2349248dd1e32b157fa18108451f4c6e3b162f0fdfbdd11dd7855d4e49a2f0f609004a77f3`
- PGP 서명: 유효
- 서명자: Christopher Schultz
- 서명 subkey fingerprint: `3262A061C42FC4C7BBB5C25C1CF0293FA53CA458`
- primary fingerprint: `5C3C5F3E314C866292F359A8F3AD5C94A67F707E`

참고 자료:

- [Apache Tomcat 10 다운로드](https://tomcat.apache.org/download-10)
- [Apache Tomcat 10 보안 공지](https://tomcat.apache.org/security-10.html)
- [Tomcat 10.1 changelog](https://tomcat.apache.org/tomcat-10.1-doc/changelog.html)
- [Tomcat 10.1 HTTP Connector 설정](https://tomcat.apache.org/tomcat-10.1-doc/config/http.html)

## 5. 격리 런타임 구성

| 항목 | 값 |
|---|---|
| 후보 CATALINA_HOME | `/tmp/frog2-tomcat-10.1.57-verify.nUTZBI/apache-tomcat-10.1.57` |
| 후보 CATALINA_BASE | `/tmp/frog2-tomcat-10.1.57-verify.nUTZBI/base` |
| 실행 계정 | `tomcat-dev` |
| HTTP bind | `127.0.0.1:28081` |
| 종료 포트 | `28005` |
| 애플리케이션 모드 | `frog2.env=dev`, `frog2.readOnly=true` |
| 파일 저장소 | 격리 `/tmp` 경로 |
| multipart parser 제한 | `maxPartCount=10`, `maxPartHeaderSize=512` |

`maxPartCount=10`은 CSRF·경로 필드와 최대 5개 파일을 포함하는 현재 업로드 폼을 허용하면서 Tomcat 기본값보다 범위를 줄인다. 이 값은 격리 설정에만 적용했으며 실제 `/opt/tomcat` 설정은 변경하지 않았다.

`tomcat-dev` 계정이 운영 `/opt/tomcat/webapps`와 `/opt/tomcat/conf`에 쓸 수 없음을 시작 전에 확인했다.

## 6. 런타임 검증

| 요청 | 기대 결과 | 실제 결과 |
|---|---|---|
| `GET /frog2/login` | 로그인 화면 | 200 |
| `GET /frog2/resources/css/ui-system.css` | 정적 CSS | 200 |
| `GET /frog2/dashboard` (비인증) | 로그인 이동 | 302 |
| `GET /frog2/file-repository` (비인증) | 로그인 이동 | 302 |

추가 확인:

- 로그인 응답에 CSP와 `X-Content-Type-Options: nosniff` 존재
- 애플리케이션 DB pool은 read-only로 초기화
- schema readiness 읽기 검사는 통과
- JSP compile, `ClassNotFoundException`, `NoSuchMethodError`, linkage 오류 없음
- 인증 POST, 파일 upload, 변경 요청은 실행하지 않음
- 격리 인스턴스 종료 후 28081·28005 포트 닫힘

첫 로그인 JSP 컴파일을 포함한 GET 한 건이 느린 요청 기준(약 578 ms)으로 WARN 기록됐으나 오류는 아니며, 이후 기능 검증에는 영향이 없었다.

## 7. 운영·개발 무영향 확인

| 대상 | 작업 전 | 작업 후 | 결과 |
|---|---|---|---|
| 운영 PID | `1012286` | `1012286` | 동일 |
| 개발 PID | `3903978` | `3903978` | 동일 |
| 운영 WAR SHA-256 | `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88` | 동일 | 변경 없음 |
| 개발 WAR SHA-256 | `eac9827f90e8a9d7186d5fb29368caa3b95aa974294e5332e4ade77ab2342c9a` | 동일 | 변경 없음 |
| 운영 로그인 GET | 200 | 200 | 정상 |
| 개발 로그인 GET | 200 | 200 | 정상 |

운영·개발 Tomcat 재시작과 WAR 배포는 수행하지 않았다.

## 8. 판정과 다음 단계

### 완료

- Git 변경의 검토 가능한 커밋 분리
- 원격 `develop` 기준선 동기화
- 공식 Tomcat 10.1.57 artifact 무결성·서명 확인
- Java 22 빌드/JspC 호환성 확인
- 격리 런타임 GET smoke 확인
- 운영·개발 무영향 확인

### 아직 완료되지 않은 것

- `/opt/tomcat`의 실제 10.1.41 → 10.1.57 교체
- 공유 CATALINA_HOME 구조 해소
- 개발 장기 관찰 후 운영 단계 배포

따라서 현재 판정은 **업그레이드 후보 검증 PASS, 실제 런타임 보안 조치 미적용**이다. 다음 안전한 순서는 개발 전용 CATALINA_HOME을 먼저 분리하고, 10.1.57을 개발에 배포해 관찰한 뒤 운영을 별도 승인·백업·롤백 절차로 전환하는 것이다.
