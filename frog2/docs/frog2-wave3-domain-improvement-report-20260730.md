# frog2 개발 서버 Wave 3 도메인 개선 보고서

작성일: 2026-07-30
대상: `/opt/frog2-dev/repo/frog2`, `/opt/tomcat-dev`
운영 환경: 변경 없음

## 1. 결과 요약

- 고객사, 회의록·트러블슈팅, 파일 자료실의 확인된 결함을 수정했다.
- 고객사 계약 안정 후 정기점검과 마이페이지의 우선 결함을 수정했다.
- 대시보드는 중복 COUNT 또는 N+1이 확인되지 않아 변경하지 않았다.
- DB migration, DDL, DML 및 인증된 상태 변경 요청은 실행하지 않았다.
- 전체 198개 테스트, clean build 2회, WAR allowlist 및 JspC 검증을 통과했다.
- 새 WAR는 개발 Tomcat에만 배포했다.

## 2. 적용 내용

### 고객사

- 알 수 없는 `env` 값이 운영 환경으로 자동 해석되던 fail-open 동작을 제거했다.
  - 누락·공백은 기존 호환을 위해 `prod`로 유지한다.
  - 잘못된 값은 GET/POST 모두 400으로 거부하고 DAO 쓰기를 호출하지 않는다.
- 단건 조회·수정·삭제에 활성 고객 조건 `is_deleted = 1`을 적용했다.
- 삭제 고객은 환경 상세, EOS, JSON, 편집 및 상세 저장 경로에서 차단한다.
- 명시한 환경 탭을 화면의 자동 fallback이 덮어쓰지 않게 했다.
- 고객 JSON escaping을 공통 `JsonResponse.escape()`로 통합했다.

### 회의록·트러블슈팅

- 요청 mapper를 추가해 양수 ID, 필수 문자열, strict 날짜/일시 및 실제 폼 선택값 allowlist를 DAO 호출 전에 검사한다.
- 잘못된 요청은 공통 400 오류 계약으로 응답한다.
- 회의록 update/delete의 ID 누락 시 발생하던 빈 200 응답을 제거했다.
- 회의록 페이지를 total count 기준 유효 범위로 제한하고 OFFSET 오버플로를 차단했다.
- 회의록 edit GET의 작성자 확인은 이미 읽은 DTO를 재사용한다.
  - 동일 경로 SELECT 2회에서 1회로 감소했다.
- 트러블슈팅 기본 발생일을 UTC가 아닌 브라우저 로컬 날짜로 생성한다.
- legacy/null 회의 유형도 JSP 오류 없이 표시한다.

### 파일 자료실

- metadata의 저장 크기를 엄격히 파싱하고 실제 `.data` 크기와 일치하는지 검사한다.
- metadata 입력은 8 KiB로 제한한다.
- 응답이 이미 시작된 뒤 다운로드 스트림이 끊기면 예외를 다시 전달해 부분 파일을 정상 완료처럼 처리하지 않는다.
- 실제 개발/운영 자료실 파일은 읽거나 이동하거나 삭제하지 않았다.

### 정기점검·마이페이지

- 프로필 이름만 수정하는 경로를 추가해 폼에 없는 `department`가 `NULL`로 덮이는 문제를 제거했다.
- 정기점검 history의 라이선스 그래프는 이미 읽은 record 목록에서 만든다.
  - 같은 고객의 maintenance 데이터 SELECT 2회에서 1회로 감소했다.
  - 기존 DAO API는 호환성을 위해 `@Deprecated(forRemoval = false)`로 보존했다.
- 마이페이지 최근 점검 링크를 실제 존재하는 고객별 history 화면으로 수정했다.
- 대시보드는 현재 목록 재사용과 단일 월 조회 계약이 정상이라 변경하지 않았다.

## 3. 변경 파일

### 새 파일

- `src/main/java/com/company/controller/MeetingRequestMapper.java`
- `src/main/java/com/company/controller/TroubleshootingRequestMapper.java`
- `src/main/java/com/company/util/LicenseUsageSeriesBuilder.java`
- `src/test/java/com/company/controller/DomainServletValidationTest.java`
- `src/test/java/com/company/controller/FileRepositoryDownloadServletTest.java`
- `src/test/java/com/company/controller/MeetingRequestMapperTest.java`
- `src/test/java/com/company/controller/TroubleshootingRequestMapperTest.java`
- `src/test/java/com/company/model/CustomerDAOSoftDeleteContractTest.java`
- `src/test/java/com/company/util/LicenseUsageSeriesBuilderTest.java`

### 수정 파일

- 고객사: `CustomerEnvironment`, `CustomerCommandController`, `CustomerCommandService`,
  `CustomerDetailQueryService`, `CustomerJsonResponse`, `CustomerQueryController`,
  `CustomerDAO`, `customers_detail.jsp`, `customer_detail.js` 및 관련 테스트
- 회의록·트러블슈팅: `MeetingServlet`, `TroubleshootingServlet`,
  `MeetingRecordDAO`, `meeting_view.jsp`, `troubleshooting_form.js` 및 관련 테스트
- 파일 자료실: `FileRepositoryService`, `FileRepositoryDownloadServlet` 및 관련 테스트
- 정기점검·마이페이지: `MaintenanceServlet`, `MaintenanceRecordDAO`, `UserDAO`,
  `MyPageServlet`, `mypage.jsp` 및 관련 테스트

삭제한 파일은 없다.

## 4. 검증 결과

- 집중 단위 테스트: 성공
- 전체 테스트: `198 tests, 0 failures, 0 errors, 0 skipped`
- clean build 2회: 모두 성공
- 두 빌드 WAR SHA-256:
  `7308e2200f09f0221d05e525b8378a36cda38548485b07ba7df93d1110923644`
- `git diff --check`: 성공
- JspC: 36 generated Java, 55 classes, 0 errors
- WAR allowlist: 8개 승인 JAR만 포함
- WAR에 `.java`, `db.properties`, `build/`, source/Javadoc/test JAR 없음
- 보호 디자인 파일:
  - `header_nav.jspf`: `51d9031b706a9b9983bc0da22480569af25ef36f757cdb3550366e74febf0a8f`
  - `login.jsp`: `a38038f9e7a9e44c9006af74c7c51715e95a3b37464f9f4c2f66fbc594fe9e41`
  - `linear_refinement.css`: 계속 없음

## 5. 개발 배포

- 배포 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- WAR SHA-256:
  `7308e2200f09f0221d05e525b8378a36cda38548485b07ba7df93d1110923644`
- 개발 Tomcat PID: `3498970`
- 시작 시각: `2026-07-30 13:25:03 KST`
- 개발 내부 URL: `http://127.0.0.1:18081/frog2/`
- 로그인 GET: 200
- 정적 CSS GET: 200
- 고객사, 회의록, 트러블슈팅, 정기점검, 마이페이지, 파일 자료실 비인증 GET: 로그인으로 302
- 배포 후 JSP, `ClassNotFoundException`, `NoSuchMethodError`, linkage 오류: 없음
- `frog2.env=dev`, `frog2.readOnly=true` 적용 상태 확인

## 6. 운영 무영향

- 운영 Tomcat PID: 작업 전후 `1012286`
- 운영 시작 시각: 작업 전후 `2026-06-30 10:18:40 KST`
- 운영 WAR SHA-256: 작업 전후
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 운영 `server.xml` SHA-256: 작업 전후
  `34afc0a0f9d78660c5ded03b1654b9a24204378495928baf909b6238ac3ec47a`
- 운영 8080 `/frog2/`: 작업 전후 로그인으로 302
- 운영 Tomcat 중지·재시작·배포 없음

## 7. DB 및 파일 안전

- 실행한 migration: 없음
- 실행한 DDL/DML: 없음
- 인증된 POST/PUT/PATCH/DELETE: 없음
- 배포 후 접근 로그의 검증 요청은 모두 GET이었다.
- 실제 파일 자료실 데이터 이동·삭제: 없음

## 8. 백업과 롤백

배포 전 백업:

- `/opt/frog2-dev/backups/wave3-domains-deploy-20260730_132322/frog2.war.before`
- `/opt/frog2-dev/backups/wave3-domains-deploy-20260730_132322/frog2.exploded.before`
- `/opt/frog2-dev/backups/wave3-domains-deploy-20260730_132322/frog2.work.before`
- 중지 후 이동한 원본:
  - `frog2.exploded.live-moved`
  - `frog2.work.live-moved`

롤백 절차:

1. `systemctl stop tomcat-dev`
2. 현재 `/opt/tomcat-dev/webapps/frog2`와
   `/opt/tomcat-dev/work/Catalina/localhost/frog2`를 별도 실패 백업 경로로 이동한다.
3. `frog2.war.before`를 `/opt/tomcat-dev/webapps/frog2.war`로 복원하고
   소유자 `tomcat-dev:tomcat-dev`, 모드 `0640`을 적용한다.
4. `frog2.exploded.before`와 `frog2.work.before`를 원래 경로에 복원한다.
5. `systemctl start tomcat-dev`
6. WAR 해시, 로그인 GET, 정적 자산, 비인증 redirect 및 오류 로그를 다시 확인한다.

건강한 배포 상태이므로 롤백 자체는 실행하지 않았다.

## 9. 남은 위험과 후속 후보

우선순위가 높은 후속:

1. 트러블슈팅 수정·삭제의 소유자/역할 정책 확정 후 사용자 ID 기반 권한 적용
2. 트러블슈팅·고객사 목록 pagination과 stable ordering
3. 마이페이지 YearMonth 범위 검증 및 월 조회를 날짜 범위 조건으로 전환
4. 마이페이지 JSP scriptlet 4개 제거

그 다음 후보:

- 파일 자료실 cursor pagination과 필요 시 콘텐츠 checksum
- meeting comment pagination
- 현재 호출자가 없는 `UserDAO.updateUserProfile(...)` 폐기 검토
- legacy 파일 자료실 JSP alias와 고객사 구형 add/edit 경로는 운영 접근 로그 확인 후 폐기

특이사항:

- 개발 Tomcat 정상 종료 시 Vertica JDBC driver를 Tomcat이 강제 해제했다는 기존 경고가 1회 기록됐다.
  새 프로세스 시작과 WAR 배포에는 영향이 없었고 이후 오류는 없었다.

현재 신뢰도: 94%.
남은 최소 검증은 운영 데이터 쓰기 없이 수행 가능한 실제 브라우저 로그인 후 읽기 전용 화면 이동이다.
