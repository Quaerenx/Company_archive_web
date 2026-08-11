# 5단계 — 성능·동시성·자원 관리·관측성 감사

## 결론

현재 데이터 규모에서는 확인된 즉시 장애성 병목이 없다. 성장 시 가장 먼저 문제가 될 경로는 트러블슈팅 본문 검색, 자료실 cold scan, DB connection pool 대기, 무제한 정기점검 이력이다.

- 성능 건강도: **74/100**
- P0: 없음
- 공유 DB·운영 서버 부하 테스트는 실행하지 않음

## 요청 비용 지도

| 경로 | DB/파일 비용 | 성장 위험 |
| --- | --- | --- |
| 대시보드 | 월별 정기점검·schedule 조회와 view-model 조립 | 고객사 증가 시 여러 목록 materialization |
| 고객사 검색 | COUNT + page 목록, 6개 요약 필드 `%검색어%` | leading wildcard 전체 탐색 |
| 고객사 상세 | 약 6~7개 순차 SELECT | DB 왕복 latency |
| 트러블슈팅 검색 | COUNT + 목록, 선택 시 대형 본문 포함 | 가장 큰 DB 검색 위험 |
| 정기점검 이력 | 고객사 전체 이력 + chart data | 이력 증가에 따라 DOM·JSON 증가 |
| 파일 자료실 | cold directory scan, metadata parse, sort; warm snapshot 재사용 | 변경 직후 cache stampede·50k 상한 |

## 자료실 임시 디렉터리 측정

운영 파일을 사용하지 않고 `/tmp` fixture로 측정했다.

| 관리 엔트리 | 실제 파일 수 | cold median | warm median |
| ---: | ---: | ---: | ---: |
| 100 | 200 | 31.4ms | 0.415ms |
| 1,000 | 2,000 | 105.2ms | 0.540ms |
| 5,000 | 10,000 | 226.0ms | 0.638ms |

5,000개 관리 엔트리를 4개 thread가 동시에 cold 조회했을 때 wall time은 약 267ms이고 scan은 4회 발생했다. 이는 cache miss에 대한 single-flight가 없어 같은 디렉터리를 중복 스캔할 수 있음을 보여준다.

- 5,000개 snapshot retained heap 추정: 약 1.28MiB
- 50,000개 상한 단순 외삽: 약 12~15MiB
- 32개 directory, 총 50,000개 cache 상한은 존재함

## connection pool·thread

- HikariCP: maximum 20, minimum 5, connection timeout 30초, query timeout 30초
- Tomcat connector의 `maxThreads`가 애플리케이션 용량 계약으로 고정돼 있지 않다.
- BCrypt는 connection을 반환한 뒤 실행하므로 과거 로그인 pool 고갈 위험은 제거됐다.
- try-with-resources가 DAO connection/statement/result set 반환을 담당한다.
- Hikari shutdown과 JDBC driver deregistration은 webapp classloader 범위로 정리돼 있다.

## 동시성·캐시

- 파일 snapshot cache는 bounded지만 cold miss single-flight가 없다.
- 업로드 직후 cache invalidation은 수행되나 파일과 metadata 쓰기 사이 process crash가 발생하면 orphan 가능성이 있다.
- stable owner ID를 같은 UPDATE/DELETE 조건에 사용해 권한 확인과 변경 사이 race는 제거됐다.
- static mutable state는 주로 bounded cache와 datasource reference이며 shutdown 경로가 있다.

## 프론트엔드 성능

- 공통 인증 CSS 약 95.8KB(압축 전)와 페이지 CSS가 로드된다.
- IBM Plex Sans KR 4개 font weight 합계는 약 1.68MB이며 `font-display: swap`을 사용한다.
- 정적 자산 URL에 version이 있지만 cache header는 5분이다.
- Chart.js는 정기점검 이력에서만 로드되며 미로딩 시 차트만 생략한다.
- ambient background는 모바일·reduced-motion 조건을 고려하지만 긴 탭·다중 탭의 실제 CPU 계측은 제한적이다.
- 기존 7개 화면 × 4개 해상도 캡처에서는 viewport overflow와 console error가 없었다.

## 발견사항

### P1 — 트러블슈팅 본문 검색 성장 위험

여러 최대 65,000자 본문 컬럼에 leading-wildcard 조건을 적용하고 COUNT와 목록 조회가 반복된다. 현재 live DB 측정은 하지 않았으므로 임계치는 추정이다.

최소 조치: 기본 검색 projection 유지, 본문 검색 최소 길이 상향, query timing 계측. 규모가 커지면 전문검색/검색 인덱스를 별도 승인한다.

### P2 — 자료실 cache stampede

동일 디렉터리 cold miss에 여러 요청이 들어오면 중복 scan한다. directory key별 single-flight 또는 짧은 lock이 필요하다.

### P2 — 무제한 정기점검 이력

DB 조회, chart dataset과 DOM이 함께 증가한다. 기간 기본값과 pagination을 적용해야 한다.

### P2 — pool/thread 용량 계약 부재

Tomcat 요청 thread 수가 Hikari 20개를 크게 초과하면 connection 대기 30초가 쌓일 수 있다. 실제 동시 사용자와 query latency를 기반으로 connector queue·thread·pool을 함께 정해야 한다.

### P2 — 관측성 공백

`RequestTimingFilter`가 path/status/duration/SQL 횟수를 기록하지만 request ID, pool wait, fetched row 수, directory scan 시간, cache hit/miss가 없다. 장애 시 느린 원인을 구분하기 어렵다.

### P3 — 정적 자산 캐시·폰트

versioned asset은 장기 immutable cache로 전환 가능하다. 실제 사용 weight를 확인해 font subset 또는 weight 축소를 검토할 수 있다.

## 로드맵

### Quick win

- 파일 cache hit/miss·scan duration 계측
- 정기점검 이력 기본 기간 제한
- versioned asset 장기 cache
- 트러블슈팅 본문 검색 slow-query logging

### 중기

- 파일 cache single-flight
- customer detail query aggregation
- Hikari/Tomcat 용량 테스트와 timeout 분리
- request ID와 pool wait metric

### 구조 개선

- 50,000개 이상 자료실 외부 index
- 대규모 본문 전문검색
- 격리 DB·파일 저장소를 이용한 반복 가능한 load test

## 신뢰도와 다음 조건

- 신뢰도: **82%**
- 측정 공백: live DB query plan, 실제 사용자 동시성, 운영 filesystem latency.
- 추가 최소 조건: 복제/임시 데이터로 검색 query plan과 20/50/100 동시 요청 측정.
