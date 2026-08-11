# Archive Frontend Release Readiness Design

## Scope

Archive의 현재 색상, 타이포그래피, 카드·폼·업무 레이아웃을 유지하면서 차트와 표의 접근성, 키보드 조작, 반응형 안정성, CSS 충돌 가능성, 시각 회귀 검증 체계를 개선한다. 기능 URL, 폼 파라미터, 서버 응답, DB 계약은 변경하지 않는다.

## Approved baseline strategy

현재 소스와 개발 배포 WAR가 다르므로 기존 배포 화면을 변경 전 기준선으로 사용하지 않는다. 현재 작업 트리 전체를 `/tmp/frog2-frontend-source-baseline-20260811-011858/frog2`에 보존하고, 현재 소스를 개발 Tomcat에 먼저 배포해 변경 전 화면을 캡처한다. 이후 프론트엔드 변경을 적용해 같은 개발 Tomcat과 데이터로 변경 후 화면을 캡처한다. 두 배포 모두 개발 WAR, exploded app, work를 먼저 백업하고 개발 Tomcat만 재시작한다.

## Design decisions

1. 라이선스 차트는 기존 Chart.js와 색상을 유지한다. canvas에는 명확한 이름과 설명을 연결하고, 같은 `usageSeries`를 서버에서 렌더링한 접이식 표와 최근 값 요약을 제공한다. Chart.js가 실패해도 표는 남는다.
2. 데이터 표는 공통 `ui-table` 구조를 유지한다. 표별 숨은 caption, `scope="col"`, 정렬 표의 `aria-sort`, 정확한 빈 상태 `colspan`을 계약 테스트로 고정한다.
3. 공통 표 footer의 이전·다음 조작 영역만 44×44px로 확대한다. 색상·형태·페이지 표기 방식은 바꾸지 않는다.
4. CSS는 tokens/base/components/ui-system/page 순서를 유지한다. 새 색상과 `!important`를 추가하지 않는다. 회의록 중복은 실제 사용 근거가 있는 selector만 옮기거나 범위 제한하고, 레거시 class는 호환 mapping을 남긴다.
5. 모달은 기존 공통 dialog controller를 기준으로 제목, 초기 포커스, trap, Escape, opener 복귀를 검증한다. 현재 모달이 중첩된 DOM 구조이므로 배경 전체에 `inert`를 강제하지 않고, 안전한 적용 가능 여부와 미적용 이유를 문서화한다.
6. 시각 회귀 도구는 9개 route와 360/390/768/1024/1440 viewport를 지원한다. 고객사 상세와 정기점검 이력은 고객사 목록의 첫 상세 링크를 런타임에서 찾아 실제 이름을 메모리에만 두고, 파일명과 manifest에는 route 별칭만 기록한다.
7. 캡처는 인증된 기존 browser profile 또는 비밀 환경 전달이 있을 때만 수행한다. 자격증명, cookie, 고객사 식별자는 명령·로그·manifest·파일명에 기록하지 않는다.

## Verification and release decision

정적 계약 테스트의 red-green 기록, 전체 테스트, clean build 2회, JspC, JavaScript 문법, CSS 계약, WAR allowlist, 45개 viewport의 document overflow와 console 오류, 개발 Tomcat 로그, 운영 PID/WAR/8080 불변을 확인한다. 필수 항목이 하나라도 검증되지 않으면 운영 반영 판단은 NO-GO 또는 조건부 NO-GO로 보고한다. 운영 배포는 수행하지 않는다.
