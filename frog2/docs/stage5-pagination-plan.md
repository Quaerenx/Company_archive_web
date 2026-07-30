# Stage 5 pagination plan

현재 UI와 URL 동작은 변경하지 않는다. 이 문서는 무제한 목록의 위험과 후속 적용 순서를
고정하기 위한 계획이며, 이번 Stage 5에서는 pagination 쿼리를 배포하지 않는다.

## 대상과 우선순위

| 우선순위 | 목록 | 현재 제한 | 후속 계약 | 안정 정렬 |
|---|---|---|---|---|
| P1 | 트러블슈팅 전체·검색·작성자 목록 | 없음 | `page`, `pageSize`(기본 20, 최대 100), 기존 `query` 유지 | `occurrence_date DESC NULLS LAST, create_date DESC, id DESC` |
| P1 | 고객 목록 | 없음 | `page`, `pageSize`(기본 50, 최대 100), 기존 `filter`·`sortField`·`sortDirection` 유지 | 기존 allowlist 컬럼 뒤에 `customer_name ASC` tie-breaker |
| P1 | 고객별·담당자별 정기점검 이력 | 없음 | `page`, `pageSize`(기본 20, 최대 100), 기존 `customerName` 유지 | `inspection_date DESC, maintenance_id DESC` |
| P2 | 파일 자료실 디렉터리 목록 | 디렉터리 전체를 메모리에 적재 | `cursor`, `pageSize`(기본 50, 최대 200), 현재 경로 유지 | 디렉터리 우선, 정규화 파일명, 서버 저장 ID |
| P2 | 회의록 댓글 | 회의별 전체 | 최초 50건 + `beforeCommentId` 방식의 이전 댓글 로드 | `created_at ASC, comment_id ASC` |

## 현재 제한이 충분한 목록

- 회의록 본문 목록은 이미 20건 단위 `LIMIT/OFFSET`과 total count를 사용한다.
- 사용자 VM host는 사용자당 최대 20개 정책이 있어 별도 pagination이 필요하지 않다.
- 월별 고객 응대는 사용자·연·월 범위로 제한된다. 데이터 증가 추이를 관찰한 뒤 재평가한다.
- 대시보드 월별 정기점검은 선택 월 범위로 제한된다.
- 라이선스 사용률 시계열은 화면 chart 계약 때문에 일반 pagination을 적용하지 않는다.
  후속 작업에서는 명시적 기간과 최대 point 수를 도입하고 다운샘플링 여부를 별도로 검증한다.

## 호환 배포 순서

1. DAO에 page result(`items`, `totalCount`, `page`, `pageSize`)를 추가하고 기존 메서드는 유지한다.
2. 정렬 allowlist와 page 경계값을 단위 테스트로 고정한다.
3. JSP에 pagination 링크를 추가하되 기존 filter/search/sort 파라미터를 모두 전달한다.
4. 새 URL을 내부 링크에 먼저 적용하고 기존 URL은 첫 페이지 기본값으로 계속 수용한다.
5. access log와 응답 시간을 확인한 뒤 기존 무제한 DAO 메서드를 폐기 후보로 이동한다.

각 목록 쿼리는 항목 조회 1회와 count 1회를 상한으로 한다. 잘못된 page는 1로,
pageSize는 기본값으로 정규화한다. 검색어·정렬 파라미터는 prepared parameter와
기존 allowlist만 사용한다.

## 검증 기준

- 기존 filter/search/sort 결과와 첫 페이지의 순서가 동일하다.
- 같은 정렬값을 가진 행도 tie-breaker 때문에 페이지 사이에서 중복·누락되지 않는다.
- 빈 결과, 마지막 페이지, 범위 밖 page, 최대 pageSize 경계를 테스트한다.
- mock JDBC로 목록 1회 + count 1회 이외의 쿼리가 없음을 검증한다.
- 실제 DB 부하 테스트와 index/schema 변경은 수행하지 않는다.
