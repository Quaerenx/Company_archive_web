# 4단계 조치 결과 — 업무 기능·도메인 구현

## 결론

고객사 상세의 핵심 조회를 4개 SQL에서 1개 SQL로 합쳤고, 정기점검
이력은 고객사 전체 기록을 한 번에 읽던 구조에서 최신 20건 단위 조회로
제한했다. 회의록·댓글, 트러블슈팅, 마이페이지·개인 호스트, 자료실은
기존 계약이 맞는지 경계 테스트를 보강했다. URL, form 파라미터, 화면
구조, 공유 DB schema와 데이터는 변경하지 않았다.

- 실제 공유 DB 접속·DDL·DML: **0건**
- Tomcat 배포·재시작: **0건**
- URL·기존 form 파라미터 변경: **0건**
- 격리 복사본 전체 테스트: **395개 성공, 실패 0**
- 격리 복사본 `clean build`: **2회 연속 성공**
- JspC: **38개 Java source / 63개 class, 오류 0**
- JavaScript 문법 검사: **26개 파일 성공**

같은 검증을 원본 저장소에서도 다시 수행했다. 두 번 생성한 WAR는 모두
`79381f1393fd500b5655d52bf97b8253bee4b0e6e0a66a3ae9e10ed941318766`으로
일치했고, 정렬한 WAR 경로 목록 hash도 두 번 모두
`6c75a2431d8bb4c792556aaafbfc33959f3e04e0283c6b7b950eae9d3033c87d`였다.
`git diff --check`도 통과했다.

## 도메인 계약 지도

| 도메인 | URL·method와 동작 | 처리 경로 | 결과·오류 계약 |
| --- | --- | --- | --- |
| 고객사 | `GET /customers`, `view=list/detail/edit/editDetail/add`; JSON `action=getDetail/getCustomersForMaintenance`; `POST action=add/update/delete/saveDetail` | `CustomersServlet` → query/command controller → `CustomerDAO`·`CustomerDetailDAO`·`VerticaEosDAO` → 고객사 JSP/JSON | 인증 필요. 검색·page 오류 400, 없는 JSON 대상 404, command 결과는 기존 session flash와 redirect 유지 |
| 정기점검 | `GET /maintenance`, `view=cards/history/add/edit`; `POST action=add/update/delete` | `MaintenanceServlet` → `CustomerDAO`·`MaintenanceRecordDAO` → 정기점검 JSP | 인증 필요. `historyPage` 생략은 1, 잘못된 값은 400 `invalid_history_page`; 쓰기 결과 flash·redirect 유지 |
| 회의록 | `GET /meeting`, `view=list/view/write/edit`; `POST action=write/update/delete` | `MeetingServlet` → `MeetingRecordDAO`·`MeetingCommentDAO` → 회의록 JSP | 인증 필요. 객체 소유권은 stable user ID 조건을 사용하며 기존 redirect 유지 |
| 댓글 | `POST /comment`, `action=add/update/delete` | `CommentServlet` → `MeetingCommentDAO` → JSON | 인증·CSRF 필요. object ID와 author user ID를 함께 검증 |
| 트러블슈팅 | `GET /troubleshooting`, `view=list/add/view/edit`; `POST action=add/update/delete` | `TroubleshootingServlet` → `TroubleshootingDAO` → JSP | 검색 `q/scope/page/pageSize` 유지. 소유권 변경은 creator user ID 조건, 기존 400·redirect 계약 유지 |
| 마이페이지 | `GET /mypage`, `action=view/editProfile/changePassword/monthlyResponse`; `POST formAction=updateProfile/updatePassword/addResponse/updateResponse/deleteResponse` | `MyPageServlet` → 사용자·정기점검·트러블슈팅·월별응대 DAO → 마이페이지 JSP | session user ID만 사용. 기존 message/flash와 redirect 유지 |
| 개인 호스트 | `GET/POST /vm-hosts`, POST save/delete | `UserVmHostServlet` → `UserVmHostDAO` → `/vm_hosts/list.jsp` | request의 `userId`는 신뢰하지 않고 session user ID만 사용 |
| 파일 자료실 | `GET /file-repository`, `GET/POST /file-repository/upload`, `GET /file-repository/download`와 기존 `/filerepo/*.jsp` alias | 파일 Servlet → `FileRepositoryService` → JSP/JSON/attachment stream | cursor·path·파일 검증 오류는 정확한 4xx/5xx; upload는 JSON, download는 attachment-only |

## 적용한 변경

### 1. 고객사 상세

- prod/stg/dev 상세를 각각 실행하던 3개 SELECT를 environment 구분자를 가진
  하나의 `UNION ALL` SELECT로 변경했다.
- prod 행에는 기존과 동일하게 `is_deleted = 1` 조건을 유지했다.
- 별도 `CustomerDAO.getCustomerByName()` 조회를 없애고 prod 상세 projection에서
  기존 `CustomerDTO` 필드를 그대로 구성한다.
- prod 상세가 없으면 없는 고객사와 동일하게 처리하는 기존 계약을 유지한다.
- EOS 조회는 서로 다른 catalog와 schema 호환 로직이므로 별도로 유지했다.

정량 결과:

- 고객사 요약+환경 상세 핵심 SQL: **4개 → 1개(75% 감소)**
- 일반적인 EOS 포함 상세 요청 SQL: **6~7개 → 3~4개**
- connection 획득: **3개 → 2개**
- 수치는 fake JDBC로 statement와 connection을 직접 계측한 결과이며 공유 DB
  지연시간을 측정한 값은 아니다.

### 2. 정기점검·대시보드

- `MaintenanceSchedule` 경계 테스트를 확장했다.
  - 12월 기준 분기 고객의 다음 해 3월
  - 윤년 2월 29일 기준월
  - 월 residue에 의한 3개월 간격
  - 비활성·적용기간 밖 schedule
  - schedule 컬럼이 없는 경우 월간 기본값
- 고객 배정 SQL이 활성 고객 `d.is_deleted = 1`만 포함하는지 고정했다.
- 건국대병원 override migration이 interval 3, anchor 3월인지 정적으로 확인했고
  migration은 실행하지 않았다.

### 3. 정기점검 이력 제한과 표시 결함

- 기존 고객사 전체 이력 무제한 SELECT를 제거했다.
- 기본 `historyPage=1`, page size 20을 적용했다.
- 정렬을 `inspection_date DESC, maintenance_id DESC`로 고정하고 null 날짜를
  뒤로 보내 동일 날짜에서도 페이지 중복·누락이 없게 했다.
- 첫 유효 페이지는 `COUNT(*) OVER()`가 포함된 bounded query 한 번으로 목록과
  총 개수를 함께 얻는다.
- 너무 큰 양수 page는 count 후 마지막 페이지로 보정한다.
- 목록과 Chart.js가 같은 최대 20개 record를 사용한다.
- 이전/다음 링크와 공통 table footer를 추가했지만 기존 URL은 유지했다.
- 계산한 라이선스 요약을 사용하지 않는 별도 Map에 넣던 실제 결함을 수정해
  `record.licenseSummary`에 설정했다. JSP가 읽는 값과 controller가 만드는 값이
  이제 일치한다.

정량 결과:

- 이력 query 반환 행: **N개(무제한) → 최대 20개**
- 이력 카드 DOM: **N개 → 최대 20개**
- 차트 point: **N개 → 최대 20개**
- 정상 page의 핵심 이력 query: **1개**
- 범위를 벗어난 page 보정 시 핵심 query: **최대 3개**

### 4. 회의록·댓글

- 새 댓글이 동시에 추가돼도 기존 cursor 다음 페이지가 밀리지 않는 fixture를
  추가했다.
- `beforeCommentId`보다 작은 stable ID 조건이 이전 페이지와 중복되지 않음을
  검증했다.
- 기존 stable author user ID와 단일 SQL 소유권 변경 경로가 통과해 production
  코드는 변경하지 않았다.

### 5. 트러블슈팅

- 검색 후 이전/다음 및 상세 복귀 경로가 `q`, `scope=content`, `page`,
  `pageSize`를 보존하는지 JSP 계약 테스트로 고정했다.
- 한 글자·100자 초과·공백 검색, 본문 검색 opt-in, owner 변경·삭제의 기존
  테스트가 통과했다.
- 본문 `%검색어%` 비용은 정확성 결함이 아니라 데이터 증가 시 성능 과제이므로
  이번 기능 단계에서 SQL을 변경하지 않았다.

### 6. 마이페이지·개인 호스트

- 표시명이 같은 두 사용자의 월별 고객 응대도 각각 immutable user ID만 SQL에
  bind하는 테스트를 추가했다.
- 개인 호스트 Servlet에 DAO 주입 지점을 추가해 request `userId`에 다른 사람
  값을 넣어도 조회·삭제 모두 session principal의 user ID만 사용함을 검증했다.
- 외부 URL, request 파라미터, 저장 동작은 변경하지 않았다.

### 7. 파일 자료실

- cursor 첫 페이지 뒤에 정렬상 더 나중인 항목이 추가돼도 다음 페이지가 기존
  경계 항목을 중복하지 않는 테스트를 추가했다.
- invalid cursor 400, path containment, symlink, MIME·크기 정책 등 기존 계약이
  함께 통과했다.
- 파일 저장 로직과 실제 개발 저장소는 변경하지 않았다.

## deprecate 분류

개발 접근 로그는 2026-07-03~2026-08-11 범위에서 URL별 횟수만 집계했다.
IP, cookie, query 원문, 사용자 정보는 읽거나 보고서에 기록하지 않았다.

| 후보 | 정적·runtime 근거 | 분류·조치 |
| --- | --- | --- |
| `resources/css/main_style.css` | JSP 정적 참조는 없지만 direct request **65회**. 현재 tokens/base compatibility entry | **호환 유지**. 삭제하지 않음 |
| 구형 `/filerepo/*.jsp` alias | 개발 접근 **4회**, 정식 `/file-repository` **122회**; web.xml·보안 계약 테스트에 명시 | **호환 유지**. 충분한 공지·관찰 기간 뒤 별도 제거 |
| `/vm_hosts/list.jsp` | 구형 직접 URL hit 0, 정식 `/vm-hosts` **35회**. 그러나 정식 Servlet의 현재 view target | **호환 유지**. URL이 아니라 내부 view라 삭제 불가 |
| `/admin/pool-status` | 개발 접근 **1회**, web.xml 관리 route | **호환 유지**. 기능·권한 검토 없이 제거하지 않음 |
| `.btn`, `.button` 계열 alias | 여러 JSP가 `ui-button`과 함께 사용 | **호환 유지**. 화면별 전환과 시각 회귀 전 제거 금지 |
| `db/legacy` migration | checksum inventory와 역사 추적 문서가 참조 | **역사 자료 유지**. active migration runner에서는 계속 제외 |
| JSP·Servlet | 모든 Servlet이 web.xml에 매핑되고, JSP는 dispatcher/include/error/view로 사용 | **즉시 제거 대상 없음** |

정적 검색만으로 확정할 수 없는 asset도 삭제하지 않았다. 따라서 이번 단계에서
제거한 production file은 **0개**다.

## 변경 파일

Production:

- `CustomerDetailQueryService.java`
- `CustomerQueryController.java`
- `MaintenanceServlet.java`
- `UserVmHostServlet.java`
- `CustomerDetailDAO.java`
- `MaintenanceRecordDAO.java`
- `maintenance/maintenance_history.jsp`

Tests:

- 고객사 상세·정기점검 schedule·이력 pagination/Servlet/JSP 계약
- 댓글 cursor·트러블슈팅 return parameter 계약
- 월별 고객 응대 stable owner·개인 호스트 session owner 계약
- 자료실 cursor 안정성 계약

Documentation:

- 구현 설계와 단계별 실행 계획
- 이 조치 결과 보고서

## 실행하지 않은 작업

- 모든 migration SQL, schema 변경, backfill
- 공유 DB SELECT/INSERT/UPDATE/DELETE/DDL
- 실제 고객 데이터 기반 query plan·latency 측정
- 인증된 쓰기 HTTP E2E
- 운영·개발 Tomcat 배포·재시작
- commit, branch, push, PR

검증된 source는 원본 저장소에 반영했지만 개발 WAR에는 배포하지 않았다.
따라서 현재 개발 배포 WAR hash
`50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9`와
새 source build hash가 다른 것은 의도된 상태다.

## 무영향·롤백 근거

- 덮어쓴 기존 파일 백업:
  `/tmp/frog2-domain-source-backup-20260811.jSbF8L/original-files.tar`
- 운영 Tomcat PID 전/후: `1012286` / `1012286`
- 개발 Tomcat PID 전/후: `3605261` / `3605261`
- 운영 WAR SHA-256 전/후:
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 개발 WAR SHA-256 전/후:
  `50837b9406a60b75ddd47fb54ec95b64333c797dacdb4a70b2465a4242a2bdb9`
- 운영·개발 로그인 GET: 200
- 비인증 dashboard GET: 운영·개발 모두 login으로 302
- 개발 정적 CSS GET: 200

## 남은 위험

1. 새 `UNION ALL` 고객사 상세 SQL은 fake JDBC 계약과 Java 컴파일로 검증했지만
   공유 Vertica에 실행하지 않았다. 격리 Vertica snapshot에서 실제 column type
   호환을 한 번 확인하면 가장 큰 검증 공백이 닫힌다.
2. 정기점검 이력 차트는 현재 페이지 20건만 보여준다. 전체 기간 분석이 업무상
   필요하면 DB write 없이 별도 기간 선택 read API를 설계해야 한다.
3. 트러블슈팅 본문 포함 검색은 데이터 증가 시 느려질 수 있다. 실제 query plan과
   검색 사용량을 확인하기 전에는 index/전문검색을 추측해 추가하지 않는다.
4. 등록·수정·삭제와 파일 upload 전체 흐름은 공유 환경 안전 제약으로 실행하지
   않았다. 격리 DB·파일 저장소가 마련되면 별도 E2E가 필요하다.

## 신뢰도

- 테스트 근거: 40/40
- 변경 diff·호환 계약 검토: 28/30
- 정적 호출 경로·업무 규칙 검토: 28/30
- 종합: **96%**

실제 Vertica에서 새 read-only SQL을 실행하지 않은 점과 격리 쓰기 E2E 공백 때문에
100%로 판단하지 않는다.
