# frog2 Carbon 아이콘 사용 원칙 적용 보고

## 결론

기존 구현은 단색과 대비 토큰은 갖추고 있었지만 아이콘 크기 체계, 아이콘 전용
조작 영역, 일부 텍스트-아이콘 색상 및 접근 가능한 이름은 Carbon 원칙과 완전히
일치하지 않았다. 해당 차이를 공통 규칙과 화면별 보완으로 정리해 개발 서버에만
배포했다.

참조: <https://carbondesignsystem.com/elements/icons/usage/>

## 적용 내용

- UI 아이콘 크기를 `16 / 20 / 24 / 32px`로 제한했다.
- 48px 이상 빈 상태 표시는 UI 아이콘이 아닌 `illustration-icon-size-*` 토큰으로
  분리했다.
- 아이콘 전용 클릭 영역과 공통 페이지 이동 영역을 최소 `44 x 44px`로 통일했다.
- 버튼, 링크, 제목에 붙는 아이콘은 `currentColor`와 중앙 정렬을 사용하게 했다.
- 아이콘 hover 시 전경색을 따로 바꾸지 않고 컨테이너 배경으로 상태를 표현했다.
- 회의록 페이지 이동, 고객사 검색어 지우기, 월간 응대 수정·삭제 아이콘에
  `aria-label` 또는 `aria-hidden` 계약을 보완했다.
- 기존 Font Awesome 아이콘 자산은 유지했다. Carbon 아이콘 라이브러리나 신규
  외부 의존성은 추가하지 않았다.

## 검증

- 집중 계약 테스트와 전체 Java 테스트: 302개, 실패·오류·건너뜀 0.
- `./gradlew clean test check war`: 2회 연속 성공.
- 재현 WAR SHA-256:
  `467c17a1db58ef88623c6d882e9e78dfa87ff8c5cd243fd348d6f3f592e7fcf5`.
- JspC: JSP 35개, 생성 Java 36개, class 61개, 오류 0.
- JavaScript 23개 문법 검사와 `git diff --check` 성공.
- Firefox 정적 검증: 390/1024px 및 OS Light/Dark 24상태, 실패 0.
  - 아이콘 전용 영역: `44 x 44px`
  - 일반 UI 아이콘: `16px`
  - 제목 아이콘: `24px`
  - 아이콘/텍스트 색상 동일
  - 중앙 정렬 오차: `0px`
- 개발 로그인 Firefox 검증: 390/768/1024/1440px 및 OS Light/Dark 8상태,
  실패 0.

## 개발 배포와 운영 무영향

- 개발 URL: `http://192.168.40.70:18081/frog2/login`
- 개발 PID: `885780`
- 개발 WAR SHA-256:
  `467c17a1db58ef88623c6d882e9e78dfa87ff8c5cd243fd348d6f3f592e7fcf5`
- 백업: `/opt/frog2-dev/backups/carbon-icons-20260803-155113`
- 운영 PID: 작업 전후 `1012286`
- 운영 WAR SHA-256: 작업 전후
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 운영 로그인 본문과 비인증 대시보드 응답은 작업 전후 동일했다.
- DB DDL/DML과 인증 POST는 실행하지 않았다.

## 특이사항

- 개발 Tomcat 종료 시 JDBC 드라이버를 Tomcat이 강제로 해제했다는 경고 1건이
  있었으나 새 기동 구간에는 JSP, 클래스 로딩, 링크 오류가 없다.
- 새 기동 직후 최초 로그인 JSP 응답은 573ms였고 이후 5회는 1.3~2.3ms였다.
- 롤백 시 위 백업의 `frog2.war`, `frog2-exploded`, `frog2-work`를 개발 경로에만
  복원한다. 운영 경로는 롤백 대상이 아니다.
