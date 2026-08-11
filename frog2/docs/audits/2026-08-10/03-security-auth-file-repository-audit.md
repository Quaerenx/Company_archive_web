# 3단계 — 인증·권한·세션·CSRF·파일 자료실 보안 감사

## 결론

인증·CSRF·객체 소유권·파일 경로 방어는 현재 내부 업무 시스템 기준으로 양호하다. 현재 유효한 상위 위험은 외부 CDN 무결성 부재와 실제 운영 HTTPS cookie 미검증이다.

- 건강도: **80/100**
- P0: 없음
- 공격성 테스트, brute force, 인증 쓰기 요청은 실행하지 않음

## URL·권한 요약

| 영역 | 읽기 | 쓰기·변경 |
| --- | --- | --- |
| 로그인·정적 자산·오류 페이지 | 공개 | 로그인 POST만 인증 처리 |
| 대시보드·고객사·정기점검 | 모든 로그인 사용자 | 고객사는 로그인 사용자, 정기점검은 stable creator `userId` |
| 회의록·댓글·트러블슈팅 | 모든 로그인 사용자 | 생성자/작성자 stable `userId`로 수정·삭제 제한 |
| 마이페이지·월별 응대·VM host | 현재 사용자 | 현재 stable `userId`만 |
| 파일 자료실 | 모든 로그인 사용자 | 업로드 가능, 삭제 API 없음 |
| `/admin/pool-status` | 설정된 관리자 ID만 | 상태 조회 전용 |

정식 정책은 [frog2-authorization-policy-20260810.md](../../../docs/frog2-authorization-policy-20260810.md)에 기록돼 있다. 관리자 ID 설정이 비어 있으면 접근 가능한 사용자가 없는 fail-closed 방식이다.

## 인증·세션

- session principal은 typed `UserDTO`이며 JSP 파라미터나 표시명을 신뢰하지 않는다.
- 로그인 성공 시 기존 session을 그대로 사용하지 않는 fixation 방어가 적용돼 있다.
- 로그아웃은 POST·CSRF 계약을 사용하며 session을 무효화한다.
- BCrypt cost 계산 전에 DB connection을 반환한다.
- 오류 메시지는 계정 존재 여부를 과도하게 구분하지 않는다.
- `HttpOnly`와 `SameSite=Strict`가 설정돼 있다.
- HTTP 개발 환경에서는 의도적으로 `Secure`가 없고 HTTPS 요청에서만 Tomcat이 추가한다.

## CSRF·입력·출력

- 상태 변경 요청은 공통 CSRF filter로 보호한다.
- GET에서 DML·DDL을 수행하는 경로는 확인되지 않았다.
- DAO 입력은 PreparedStatement를 사용하며 ORDER BY는 허용된 필드로 제한한다.
- JSP 출력은 JSTL `c:out`과 context별 escaping을 사용한다.
- CSP는 `unsafe-inline` 없이 동작하고 inline script/style/event handler는 제거됐다.
- HTML 요청과 JSON 요청의 400/401/403/404/409/500 계약이 공통 예외 filter에서 분리된다.

## 파일 자료실 위협 모델

현재 방어:

- webroot 밖 `frog2.fileRepoRoot` 저장
- multipart 파일 크기·개수 제한
- 확장자와 MIME allowlist
- JSP/JSPX/HTML/SVG 및 active content 차단
- 사용자 파일명과 opaque 서버 저장명 분리
- canonical containment 검사
- symlink·directory traversal 방어
- 전용 Servlet download와 `Content-Disposition: attachment`
- MIME sniffing 방지 header
- cursor pagination과 bounded snapshot cache
- 테스트는 `/tmp` 저장소 사용

잔여 위험:

- 프로세스 crash 시 파일과 metadata 사이 orphan 가능성
- 첫 접근·디렉터리 변경 시 전체 scan
- 악성 문서의 콘텐츠 자체를 antivirus로 검사하지는 않음
- 실제 HTTP upload→list→download lifecycle은 격리 환경에서 미검증

## 발견사항

### P1 — 외부 CDN에 SRI가 없음

Font Awesome CSS와 Chart.js가 외부 origin에서 무결성 검증 없이 로드된다. CSP가 origin을 허용하므로 해당 CDN이 침해되면 Archive origin 안에서 코드가 실행될 수 있다.

근거:

- [includes/header.jsp](../../../src/main/webapp/includes/header.jsp)
- [maintenance_history.jsp](../../../src/main/webapp/maintenance/maintenance_history.jsp)
- [SecurityHeadersFilter.java](../../../src/main/java/com/company/filter/SecurityHeadersFilter.java)

최소 조치: 자체 호스팅을 우선하고, 불가하면 정확한 SRI hash와 `crossorigin`을 사용한다.

### P1 — 운영 HTTPS session cookie 검증 미완료

애플리케이션의 임시 로컬 TLS 검증은 통과했지만 실제 운영 proxy/terminator가 `request.isSecure()`를 올바르게 전달하는지는 확인되지 않았다. 운영 반영 전 실제 응답에서 cookie 값을 출력하지 않고 속성만 검사해야 한다.

### P2 — brute-force 제한이 애플리케이션 내부에 없음

로그인 실패를 애플리케이션 레벨에서 계정/IP별로 제한하지 않는다. 내부망 경계나 reverse proxy rate limit이 없다면 반복 시도 방어가 약하다.

### P2 — 실제 advisory 조회 공백

이번 정적 감사에서는 인터넷 advisory database를 조회하지 않았다. 버전이 오래됐다는 이유로 취약점으로 확정하지 않았으며, 배포 조건과 호출 경로를 포함한 별도 dependency advisory 검증이 필요하다.

### P3 — CSP 허용 origin 정리

현재 자체 폰트를 사용하면서 Google Fonts origin이 CSP에 남아 있다. 실제 사용하지 않는 origin은 제거할 수 있다.

## 충분해 기각한 후보

- 객체 ID만 바꿔 타인 기록을 수정하는 IDOR: stable owner ID가 같은 SQL 조건에 포함돼 기각
- 이름 변경·동명이인 소유권 우회: 표시명을 권한 키로 사용하지 않아 기각
- 자료실 path traversal: canonical containment와 opaque name으로 기각
- GET 상태 변경: 현재 경로에서 확인되지 않아 기각
- 관리자 기본 계정: 관리자 allowlist가 비면 아무도 접근하지 못해 기각

## 신뢰도와 다음 조건

- 신뢰도: **86%**
- 추가 최소 조건: 공식 advisory 조회, 운영 HTTPS cookie audit, 격리된 파일 업로드 lifecycle E2E.
