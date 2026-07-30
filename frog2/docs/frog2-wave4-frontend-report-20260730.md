# frog2 Wave 4 프론트 마감 보고서

작업일: 2026-07-30
대상: frog2 개발 서버만
운영/DB 변경: 없음

## 1. 결과

- JSP/JSPF Java scriptlet을 4개에서 0개로 줄였다.
- 회의록 write/edit 핵심 4필드와 트러블슈팅 add/edit 핵심 14필드를 각각 WEB-INF 공통 fragment로 통합했다.
- 20,848B짜리 `meeting.css`를 공통/상세/목록/폼 CSS로 분리했다.
- 정기점검 add/edit JavaScript 372줄을 공통 `maintenance_form.js` 253줄로 통합했다.
- 참조가 0이 된 `maintenance_add.js`, `maintenance_edit.js`를 제거했다.
- Chart.js CDN이 로드되지 않아도 정기점검 이력 탐색 JavaScript가 중단되지 않게 했다.
- 전체 테스트, 반복 clean build, WAR allowlist, JspC, Firefox mock/browser 검증을 통과한 WAR를 개발 Tomcat에만 배포했다.

## 2. 코드 변경

### Java/JSP 공통화

- 수정
  - `src/main/java/com/company/controller/MyPageServlet.java`
  - `src/main/webapp/mypage/mypage.jsp`
  - `src/main/webapp/mypage/change_password.jsp`
  - `src/main/webapp/mypage/edit_profile.jsp`
  - `src/main/webapp/mypage/monthly_customer_response.jsp`
  - `src/main/webapp/meeting/meeting_write.jsp`
  - `src/main/webapp/meeting/meeting_edit.jsp`
  - `src/main/webapp/troubleshooting/troubleshooting_add.jsp`
  - `src/main/webapp/troubleshooting/troubleshooting_edit.jsp`
- 추가
  - `src/main/webapp/WEB-INF/includes/_meeting_form_fields.jspf`
  - `src/main/webapp/WEB-INF/includes/_troubleshooting_form_fields.jspf`

월별 화면이 JSP에서 계산하던 `currentYear`, `currentMonth`는 `MyPageServlet` request attribute로 이동했다. 기존 AuthFilter/Servlet 인증 경계와 POST action, CSRF, ID, 삭제 버튼, 미리보기 modal은 유지했다.

폼 관련 총 소스량:

- 회의록: 271줄 → wrapper+fragment 245줄, 26줄 감소
- 트러블슈팅: 311줄 → wrapper+fragment 231줄, 80줄 감소
- 합계: 582줄 → 476줄, 106줄(18.2%) 감소

### CSS 분할

- 수정
  - `src/main/webapp/resources/css/pages/meeting.css`
  - `src/main/webapp/meeting/meeting_list.jsp`
  - `src/main/webapp/meeting/meeting_view.jsp`
  - `src/main/webapp/meeting/meeting_write.jsp`
  - `src/main/webapp/meeting/meeting_edit.jsp`
- 추가
  - `src/main/webapp/resources/css/pages/meeting_view.css`
  - `src/main/webapp/resources/css/pages/meeting_list_layout.css`
  - `src/main/webapp/resources/css/pages/meeting_form.css`

원본 규칙과 순서를 유지한 채 페이지별 chunk만 로드한다.

- 목록: 20,848B → 9,899B, 10,949B(52.5%) 감소
- 상세: 20,848B → 12,723B, 8,125B(39.0%) 감소
- 작성/수정: 20,848B → 8,839B, 12,009B(57.6%) 감소

Firefox 동일 fixture의 분할 전/후 스크린샷 SHA-256이
`9337258eeb50e64cd5b9a74b010732ee4e79821be1eeaf2c25c41b6393797569`로 완전히 같았다.

### 정기점검 JavaScript

- 수정/추가
  - `src/main/webapp/maintenance/maintenance_add.jsp`
  - `src/main/webapp/maintenance/maintenance_edit.jsp`
  - `src/main/webapp/resources/js/pages/maintenance_form.js`
  - `src/main/webapp/resources/js/pages/maintenance_history.js`
- 제거
  - `src/main/webapp/resources/js/pages/maintenance_add.js`
  - `src/main/webapp/resources/js/pages/maintenance_edit.js`

두 JS 372줄을 공통 IIFE 253줄로 통합해 119줄(32.0%) 줄였다. 다음 동작을 함께 고쳤다.

- 옵션 API 1회 호출 및 HTTP/응답 형식 검사
- 고객사/점검자 옵션 중복 제거
- 수정 화면에서 목록에 없는 기존 고객사/점검자 값 보존
- UTC가 아닌 한국 로컬 날짜를 add 기본값으로 사용
- 고정 고객 add의 옵션/상세 자동입력 순서 안정화
- 옵션 API 실패 시 임의 문자열 저장 대신 submit 차단
- add/edit 공통 길이/필수값 검증
- edit 삭제 확인 보존
- Chart.js 미로딩 시 안전하게 차트만 생략

### 테스트

- 추가
  - `JspScriptletContractTest`
  - `MaintenanceFormAssetContractTest`
  - `MeetingCssSplitContractTest`
- 수정
  - `CssLayoutStructureTest`
  - `DesignAssetConsolidationTest`
  - `MeetingFormContractTest`
  - `MyPageViewContractTest`
  - `TroubleshootingViewContractTest`

## 3. 검증

- 집중 계약 테스트: 성공
- 전체 테스트: 206 tests, 0 failures, 0 errors, 0 skipped
- `clean test check war`: 2회 연속 성공
- 두 빌드 WAR SHA-256:
  - `b28dc3964b7bbdf378585e256fb06cd82419e1f12f598fe10682c3272a019cef`
- 작업복사본과 실제 개발 저장소 WAR: byte-for-byte 동일
- JspC: 36 generated Java, 55 classes, 0 errors
- `node --check`: 변경 JavaScript 2개 성공
- `git diff --check`: 성공
- WAR allowlist: 승인 JAR 8개만 포함
- WAR 금지 항목: `.java`, `db.properties`, `build/`, source/Javadoc/test JAR 0개
- 보호 디자인 파일:
  - `header_nav.jspf`: `51d9031b706a9b9983bc0da22480569af25ef36f757cdb3550366e74febf0a8f`
  - `login.jsp`: `a38038f9e7a9e44c9006af74c7c51715e95a3b37464f9f4c2f66fbc594fe9e41`
  - `linear_refinement.css`: 계속 없음

Firefox 152 headless, DB/auth 없는 local fixture 결과:

- add: 고객/점검자 중복 0, 로컬 날짜 정상, 상세 자동입력 정상, 51자 submit 차단, page error 0, console error 0
- fixed-customer add: options/detail 순서 정상, 중복 0, page error 0, console error 0
- edit: 목록에 없는 기존 고객/점검자 보존, 삭제 취소 정상, page error 0, console error 0
- options 503 mock: submit 차단 및 사용자 오류 메시지 정상, 의도된 오류 로그 1건 외 page error 0
- Chart.js 없음: 이력 JavaScript page error 0, console error 0
- CSS 분할 전/후 동일 fixture 스크린샷: byte-for-byte 동일

검증 산출물:

- `/root/frog2-wave4-validation-20260730/meeting-form-before.png`
- `/root/frog2-wave4-validation-20260730/meeting-form-after.png`
- `/root/frog2-wave4-validation-20260730/maintenance-add-fixture.png`
- `/root/frog2-wave4-validation-20260730/maintenance-add-fixture-mobile.png`
- `/root/frog2-wave4-validation-20260730/dev-login-after.png`

## 4. 개발 배포

- 배포 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- WAR SHA-256:
  - `b28dc3964b7bbdf378585e256fb06cd82419e1f12f598fe10682c3272a019cef`
- 개발 Tomcat PID: `3514916`
- 시작 시각: `2026-07-30 13:52:21 KST`
- 개발 내부 URL: `http://127.0.0.1:18081/frog2/`
- 로그인 GET: 200
- 새 JS/CSS 5개 GET: 모두 200
- 제거한 구형 JS 2개 GET: 모두 404
- 고객사, 회의록, 트러블슈팅, 정기점검, 마이페이지 비인증 GET: 로그인으로 302
- 배포 로그의 JSP compile, `ClassNotFoundException`, `NoSuchMethodError`, linkage 오류: 0
- `frog2.env=dev`, `frog2.readOnly=true`, 개발 전용 file repo 설정 유지

## 5. 운영 및 DB 무영향

- 운영 Tomcat PID: 작업 전/후 `1012286`
- 운영 시작 시각: 작업 전/후 `2026-06-30 10:18:40 KST`
- 운영 WAR SHA-256: 작업 전/후
  - `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 운영 `server.xml` SHA-256: 작업 전/후
  - `34afc0a0f9d78660c5ded03b1654b9a24204378495928baf909b6238ac3ec47a`
- 운영 8080 로그인 GET: 작업 전/후 200
- 운영 서비스 중지/재시작/배포: 0회
- DB DDL/DML/migration/password utility: 실행 0회
- 인증 POST/PUT/PATCH/DELETE: 실행 0회
- 개발 브라우저 기능 검증은 local mock fixture만 사용했다.

## 6. 백업과 롤백

소스 사전 백업:

- `/opt/frog2-dev/backups/wave4-frontend-20260730_133017/source-before.tar.gz`
- SHA-256:
  - `b6191d7c4f2617e03d1befeb03f0c93b3313bb8aedf0062e4c96231c40d87809`

배포 사전 백업:

- `/opt/frog2-dev/backups/wave4-deploy-20260730_135145/frog2.war.before`
- `/opt/frog2-dev/backups/wave4-deploy-20260730_135145/frog2.exploded.before`
- `/opt/frog2-dev/backups/wave4-deploy-20260730_135145/frog2.work.before`

개발 배포 롤백 절차:

1. `systemctl stop tomcat-dev`
2. 현재 `/opt/tomcat-dev/webapps/frog2`와
   `/opt/tomcat-dev/work/Catalina/localhost/frog2`를 위 백업 디렉터리 아래 별도 이름으로 이동한다.
3. `frog2.war.before`를 owner/group `tomcat-dev:tomcat-dev`, mode `0640`으로
   `/opt/tomcat-dev/webapps/frog2.war`에 복원한다.
4. `frog2.exploded.before`와 `frog2.work.before`를 원래 경로에 복원한다.
5. `systemctl start tomcat-dev`
6. 18081 로그인 GET, 정적 자산, 비인증 redirect를 확인한다.

소스 롤백은 사전 tar를 별도 임시 디렉터리에 풀고 그 안의 `src/`를
`/opt/frog2-dev/repo/frog2/src/`에 checksum 기준으로 동기화한다.
운영 Tomcat은 롤백 과정에도 건드리지 않는다.

## 7. 남은 위험과 후속 후보

- 공유 DB 안전 제약 때문에 실제 로그인 후 회의록/트러블슈팅/정기점검 화면 GET은 수행하지 않았다. 이번에는 JspC, 계약 테스트, mock browser, CSS 전/후 pixel 비교로 대체했다.
- 정기점검 이력 카드와 고객 카드가 클릭 가능한 `div`다. native anchor로 바꾸려면 focus/색상/밑줄 CSS를 함께 조정하는 별도 접근성 작업이 필요하다.
- `base.css`, `components.css`, `customers.css`에는 아직 legacy/중복 규칙이 남아 있다. 실제 인증 화면별 visual regression 기반으로 별도 단계에서 분리하는 편이 안전하다.
- `main_style.css`는 runtime JSP 참조가 없지만 호환 URL 가능성을 확인할 access log 근거가 없어 제거하지 않았다.
- `resources/images/images/ollama.png`는 정적 참조가 없지만 이번 인증 없는 crawl만으로 외부 direct URL 사용까지 증명할 수 없어 제거하지 않았다.
- 기존 Git migration 상태는 정리하지 않았다. `status --porcelain=v1 -uall`은 550건이며 커밋/브랜치/push/PR은 생성하지 않았다.

현재 신뢰도: 93%

최소 추가 검증은 격리된 DB 또는 read-only snapshot 환경에서 테스트 계정으로 회의록 list/view/write GET, 트러블슈팅 add/edit GET, 정기점검 add/edit/history GET을 열고 스크린샷/console을 비교하는 것이다. submit/delete는 계속 차단해야 한다.
