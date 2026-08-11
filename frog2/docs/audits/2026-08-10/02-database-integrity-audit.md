# 2단계 — 데이터베이스 접근 구조·데이터 무결성 감사

## 결론

요청 처리 중 DDL을 실행하던 구조는 제거됐고, 개발 환경은 설정 누락·오타 때도 쓰기를 허용하지 않는 fail-closed 구조다. 소유권 키는 안정적인 `userId`로 통일됐으며 주요 수정·삭제 권한은 단일 SQL 조건으로 처리한다.

- 건강도: **87/100**
- P0: 없음
- 현재 감사에서 DB 접속·DDL·DML을 실행하지 않음

## 정적 조사 범위

- DAO 클래스: 10개
- Java SQL 키워드 출현 위치: 67개
- 대상: Servlet, Service, DAO, listener, JDBC guard, migration SQL
- 활성 migration: 6개 파일, legacy migration: 별도 디렉터리 4개

## HTTP 요청과 DB 쓰기 가능성

| 요청 유형 | 현재 개발 동작 | 방어 장치 |
| --- | --- | --- |
| 로그인 GET/인증 전 GET | SELECT 없음 또는 정적 렌더링 | 공개 경로 제한 |
| 대시보드·목록·상세 GET | SELECT만 허용 | JDBC read-only proxy |
| 조회수·통계 조회 | 조회와 증가 분리 | 개발 read-only에서 부수 DML 차단 |
| 등록·수정·삭제 POST | 애플리케이션 권한을 통과해도 DB에서 거부 | `frog2.env=dev`, `frog2.readOnly=true`, CSRF |
| migration·schema 변경 | HTTP에서 실행되지 않음 | SQL은 버전 관리 artifact로만 보관 |

환경 판정은 [ApplicationEnvironment.java](../../../src/main/java/com/company/config/ApplicationEnvironment.java)에서 `prod` 또는 `staging`이면서 `frog2.readOnly=false`인 경우에만 쓰기를 허용한다. 누락·공백·알 수 없는 환경은 모두 read-only다.

[ReadOnlyJdbcGuard.java](../../../src/main/java/com/company/util/ReadOnlyJdbcGuard.java)는 다음을 차단한다.

- SELECT/WITH 이외 SQL
- SELECT 안에 포함된 변형 키워드
- callable statement
- updateable ResultSet
- `setReadOnly(false)`
- raw connection/statement unwrap

## 데이터 무결성

- 정기점검, 월별 고객 응대, 트러블슈팅, 회의록·댓글, 개인 호스트 소유권은 표시명이 아닌 stable `userId`를 사용한다.
- 소유자 변경·삭제는 `WHERE object_id=? AND owner_user_id=?` 형태의 한 SQL에서 권한과 변경을 함께 처리한다.
- 이름 변경·동명이인으로 다른 사용자의 기록이 섞이는 과거 위험은 현재 기준에서 제거됐다.
- BCrypt 검증은 사용자 해시 조회 후 DB connection을 반환한 다음 수행한다.
- 월별 조회는 `YEAR()/MONTH()` 함수 대신 시작일 이상·다음 달 시작일 미만의 날짜 범위를 사용한다.
- pagination은 정렬 안정성, page/size 경계, COUNT와 목록 필터를 계약 테스트로 확인한다.

## migration 상태

[migration README](../../../src/main/resources/db/migration/README.md)에 따라 애플리케이션은 SQL 파일을 자동 실행하지 않는다. 시작 시 [DatabaseSchemaReadiness.java](../../../src/main/java/com/company/model/DatabaseSchemaReadiness.java)가 metadata를 읽어 필요한 컬럼만 확인하며, 누락된 소유권 schema에서는 관련 쓰기가 fail-closed된다.

당일 선행 읽기 전용 schema audit에서 다음 활성 계약을 확인했다.

- `V20260720_01`: 개인 VM host 소유권
- `V20260720_04`: 라이선스 사용률 컬럼
- `V20260730_05`: 트러블슈팅 creator user ID
- `V20260731_06`: 정기점검·월별 고객 응대 creator user ID
- `V20260804_07`: 고객사별 월간/분기 일정
- `V20260804_08`: 검토된 건국대병원 분기 일정 override

## 발견사항

### P1 — 격리된 쓰기 E2E가 없음

mock DAO와 JDBC guard 테스트는 통과하지만 실제 Servlet → Service → DAO → DB transaction 전체를 writable 환경에서 검증하지 못했다. 공유 DB를 사용하므로 의도적으로 스킵한 상태다.

최소 조치: shared JDBC URL을 거부하는 별도 DB snapshot과 `frog2.e2e.isolated=true`가 있는 임시 Tomcat에서만 CRUD를 검증한다.

### P2 — migration 적용 이력 ledger가 없음

현재는 schema capability를 확인하지만 Flyway/Liquibase처럼 적용 버전과 checksum을 DB에 기록하지 않는다. 이미 존재하는 schema는 안전하게 확인할 수 있지만, 누가 어떤 SQL을 언제 검토·실행했는지는 운영 절차 문서에 의존한다.

최소 조치: 자동 실행 없이도 읽기 전용으로 조회할 수 있는 승인 ledger 또는 배포 기록을 둔다.

### P2 — 넓은 검색의 DB 비용

트러블슈팅의 명시적 `본문 포함` 검색은 큰 본문 컬럼에 leading-wildcard 검색을 수행한다. SQL injection 위험은 없지만 데이터가 커지면 DB 전체 탐색이 발생한다.

### P3 — legacy migration 관리

폐기된 HostDAO 관련 SQL은 `db/legacy`로 분리돼 있다. 활성 migration과 섞이지 않도록 현재 규칙을 유지하고, 자동 migration 도구를 도입할 때 legacy 디렉터리를 제외해야 한다.

## 신뢰도와 다음 조건

- 신뢰도: **88%**
- 공백: 현재 감사에서는 실제 DB metadata를 다시 조회하지 않았고 같은 날짜의 선행 read-only schema audit 결과를 사용했다.
- 추가 최소 조건: 격리 DB 쓰기 E2E와 승인된 migration ledger 확인.
