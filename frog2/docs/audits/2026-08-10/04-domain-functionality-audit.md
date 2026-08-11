# 4단계 — 업무 기능·도메인 구현 감사

## 결론

주요 도메인의 URL → Servlet → Service/DAO → JSP 흐름과 업무 규칙은 현재 테스트 기준으로 일치한다. 과거의 이름 기반 소유권, 비원자적 수정·삭제, 페이지네이션 누락은 개선됐다. 남은 문제는 대용량 경로와 실제 쓰기 E2E 공백이 중심이다.

- 기능 건강도: **86/100**
- P0: 없음
- 실제 DB 쓰기나 운영 데이터 변경은 실행하지 않음

## 도메인별 건강도

| 도메인 | 점수 | 상태 |
| --- | ---: | --- |
| 고객사 | 86 | 검색·정렬·pagination 안정, 상세 조회 쿼리 수 개선 여지 |
| 정기점검·대시보드 | 85 | 월간/분기 규칙 통합, 이력 무제한 조회·차트 접근성 잔여 |
| 회의록·댓글 | 88 | stable owner mutation, 댓글 cursor pagination 적용 |
| 트러블슈팅 | 80 | 권한 구조 양호, 본문 포함 검색 비용이 큼 |
| 마이페이지·개인 호스트 | 87 | stable `userId` 소유권과 pagination 적용 |
| 파일 자료실 | 84 | 안전한 외부 저장·cursor 적용, cold scan·쓰기 E2E 잔여 |

## 업무 흐름 요약

| 도메인 | 주요 흐름 |
| --- | --- |
| 고객사 | `/customers` → CustomerServlet → CustomerDAO → 목록/상세 JSP |
| 정기점검 | `/maintenance` → MaintenanceServlet/Service → DAO → 대시보드·이력 JSP |
| 회의록·댓글 | `/meeting`, `/comment` → Servlet → DAO → 목록/상세/JSON |
| 트러블슈팅 | `/troubleshooting` → Servlet → DAO → 목록/상세/폼 |
| 마이페이지 | `/mypage` → MyPageServlet → 사용자별 DAO → JSP |
| 개인 호스트 | `/vm_hosts` → Servlet → UserVmHostDAO → JSP/JSON |
| 파일 자료실 | `/file-repository` → Servlet → FileRepositoryService → JSP/stream |

## 정기점검 업무 규칙

- 고객사별 `interval_months`와 `anchor_month`를 하나의 schedule 규칙으로 사용한다.
- 월간 고객사는 매월, 분기 고객사는 기준월로부터 3개월 간격인 달에만 대상이 된다.
- 연도 경계에서도 month residue 계산을 사용하므로 12월→다음 해 3월이 유지된다.
- dashboard와 정기점검 이력이 같은 schedule resolver를 사용한다.
- schedule table이 준비되지 않은 경우 모든 고객사를 월간으로 처리하는 안전한 호환 동작이 있다.
- 건국대병원은 검토된 override로 3·6·9·12월 대상이다.
- 완료·미진행·라이선스 위험은 기존 서버 계산값을 재사용하며 화면에서 별도 고비용 쿼리를 추가하지 않는다.

## 기능 정확성

- 고객사·회의록·트러블슈팅·마이페이지·VM host 목록에 pagination이 적용돼 있다.
- 한 글자 검색과 100자를 넘는 검색어는 DAO 진입 전에 400으로 거부한다.
- 고객사 검색은 운영 요약 필드만 사용한다.
- 트러블슈팅 본문 검색은 사용자가 `본문 포함`을 명시할 때만 수행한다.
- 회의록 댓글은 최신 50개와 stable `comment_id` cursor를 사용한다.
- 수정·삭제 권한은 stable user ID와 object ID를 같은 SQL에 넣어 원자적으로 처리한다.
- GET은 조회·화면 이동만 수행하며 쓰기 작업은 POST와 CSRF를 사용한다.

## 구조·유지보수성

개선된 부분:

- JSP scriptlet 0
- 공통 page header, detail field tag, table footer tag
- 회의록 add/edit 공통 form fragment
- 정기점검 JavaScript 중복 제거
- Servlet session principal과 오류 계약 공통화

남은 결합:

- 고객사 상세는 한 화면을 구성하기 위해 여러 DAO 조회를 순차 실행한다.
- 일부 Servlet이 validation, pagination parsing, view-model 조립을 함께 담당한다.
- 회의록 관련 CSS는 목록·상세 파일에서 동일 selector를 재정의한다.
- 파일 저장과 metadata 저장이 단일 원자 transaction은 아니다.

## 발견사항

### P1 — 실제 쓰기 흐름 미검증

등록·수정·삭제와 파일 업로드의 mock/단위 테스트는 통과하지만 격리 Tomcat+DB를 이용한 HTTP 전체 흐름은 실행하지 않았다. 공유 DB 안전 제약상 의도적인 공백이다.

### P2 — 정기점검 이력 결과가 무제한

고객사 이력이 장기간 축적되면 JSP DOM, 차트 데이터와 응답 크기가 계속 증가한다. 최근 기간 기본값과 이전 이력 pagination 또는 기간 필터가 필요하다.

### P2 — 트러블슈팅 본문 검색

본문 포함 검색은 기능적으로 명시적이지만 다수 대형 컬럼의 `%검색어%` 조건을 사용한다. 현재 정확성 결함은 아니며 데이터 증가 시 성능 결함이 된다.

### P2 — 고객사 상세의 순차 조회

상세 화면 한 번에 약 6~7개의 순차 query 경로가 남아 있다. 데이터 규모보다 DB 왕복 latency가 먼저 영향을 줄 수 있다.

### P3 — deprecate 후보

- `main_style.css`: 런타임 JSP 참조 없음, direct URL 로그 확인 후 제거 가능
- `db/legacy` SQL: 실행 대상이 아닌 역사 자료로 계속 분리
- 레거시 `.btn`, `.button` class: 공통 `ui-button` 전환 완료 후 제거
- 정적 검색만으로 확정되지 않는 이미지·URL은 로그 확인 전 유지

## 테스트

- 전체 360개 단위·계약 테스트 성공
- 도메인 권한, pagination, read-only, schedule, JSP 구조 계약 포함
- JspC 46개 입력, 오류 0
- 실제 공유 DB DDL/DML과 인증 쓰기 E2E는 실행하지 않음

## 신뢰도와 다음 조건

- 신뢰도: **89%**
- 추가 최소 조건: 격리 쓰기 E2E, 긴 정기점검 이력 fixture, 트러블슈팅 대용량 검색 fixture.
