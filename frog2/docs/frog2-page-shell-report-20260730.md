# frog2 개발 공통 화면 shell 통합 보고서 (2026-07-30)

## 결과

- 인증 후 화면 25개가 모두 `/includes/header.jsp`와 `/includes/footer.jsp`를 정확히 한 번 사용한다.
- 자체 `<html>`, `<head>`, `<body>`를 갖던 화면은 9개에서 0개로 줄었다.
- 9개 화면에서 중복된 문서 shell 192줄을 제거하고 선언형 설정 66줄을 추가해 화면 소스는 순 126줄 감소했다.
- 로그인과 400/403/404/405/409/500/503 오류 화면은 공개·오류 전용 독립 shell로 유지했다.
- 운영 Tomcat과 DB에는 변경 요청을 실행하지 않았다.

## 변경 내용

공통 shell:

- `src/main/webapp/includes/header.jsp`
  - 기존 문서·favicon·공통 CSS·상단 메뉴 소유권을 그대로 유지
  - 기존 cascade를 보존하는 `pageCssBeforeVendor` 목록 슬롯 추가
  - 기존 탭 제목을 보존하는 `pageDocumentTitle` 선택 슬롯 추가

공통 shell로 전환한 화면:

- `src/main/webapp/dashboard.jsp`
- `src/main/webapp/customers/customers_list.jsp`
- `src/main/webapp/WEB-INF/views/filerepo/list.jsp`
- `src/main/webapp/WEB-INF/views/filerepo/upload.jsp`
- `src/main/webapp/mypage/mypage.jsp`
- `src/main/webapp/mypage/edit_profile.jsp`
- `src/main/webapp/mypage/change_password.jsp`
- `src/main/webapp/mypage/monthly_customer_response.jsp`
- `src/main/webapp/vm_hosts/list.jsp`

회귀 테스트:

- `src/test/java/com/company/layout/PageShellContractTest.java` 추가
- `DesignAssetConsolidationTest.java`, `CssLayoutStructureTest.java`의 기존 독립 head 가정을 공통 shell 계약으로 갱신

## 디자인 보존

- CSS 순서를 `core → pageCssBeforeVendor → Font Awesome → pageCss → header.css → pageCssAfterHeader`로 유지했다.
- 9개 화면의 기존 body class, 페이지 CSS 상대 순서, 페이지 JS 경로와 탭 제목을 계약 테스트로 고정했다.
- 다음 보호 파일은 변경하지 않았다.
  - `WEB-INF/includes/header_nav.jspf`: `51d9031b706a9b9983bc0da22480569af25ef36f757cdb3550366e74febf0a8f`
  - `login.jsp`: `a38038f9e7a9e44c9006af74c7c51715e95a3b37464f9f4c2f66fbc594fe9e41`
  - `linear_refinement.css`: 변경 전후 모두 없음

## 검증

- 최종 상태 clean build, 전체 테스트, WAR 검증 2회 연속 성공
- 테스트 175개: failures 0, errors 0, skipped 0
- JspC: JSP/JSPF/tag 입력 41개, 생성 class 55개, errors 0
- WAR SHA-256: `65d39fcc11c41cb6429fac6771dbefc8ef97b64da24502e51d2fb7a6b1cb5061`
- WAR 크기: 6,066,971 bytes, 항목 240개
- WAR 금지 항목과 예상 외 라이브러리 없음
- 개발 배포본과 빌드 WAR 해시 일치
- 개발 exploded JSP/JSPF/tag/CSS/JS/PNG 자산 불일치 0개
- 개발 로그인 GET 200, 정적 CSS GET 200
- 비인증 dashboard·파일 자료실 GET은 `/frog2/login`으로 302
- Tomcat 로그에 JSP 컴파일, ClassNotFound, NoSuchMethod, linkage, lifecycle 오류 없음

## 개발 배포

- 개발 URL: `http://192.168.40.70:18081/frog2/login`
- 개발 서비스: `tomcat-dev`
- 배포 후 PID: `3484118`
- 배포 후 시작 시각: `2026-07-30 13:00:06 KST`

## 백업과 롤백

소스 변경 전 백업:

- `/opt/frog2-dev/backups/page-shell-20260730_124627/source-before-v2.tar.gz`

개발 런타임 변경 전 백업:

- `/opt/frog2-dev/backups/page-shell-deploy-20260730_125828/frog2.war.before`
- `/opt/frog2-dev/backups/page-shell-deploy-20260730_125828/frog2-exploded.live-moved`
- `/opt/frog2-dev/backups/page-shell-deploy-20260730_125828/frog2-work.live-moved`

런타임 롤백은 `tomcat-dev`만 정지하고 현재 개발 배포물을 별도 보존한 뒤 위 WAR·exploded·work를 각각 원래 개발 경로로 복원하고 `tomcat-dev`만 시작한다. 운영 `/opt/tomcat`은 사용하지 않는다.

## 제한과 후속 확인

- 공유 DB 보호 원칙에 따라 로그인 POST와 인증 상태의 화면 브라우저 검증은 수행하지 않았다.
- 공통 favicon URL로 통일되며 기존 9개 화면의 query cache-buster는 제거됐다. favicon 파일 자체는 동일하다.
- 8개 화면의 페이지 JS가 footer 마크업 뒤에서 앞으로 이동했지만, 해당 JS에는 footer DOM 의존성이 없다.
- 최소 후속 검증은 mock 또는 격리 인증 세션을 사용한 9개 화면의 브라우저 스크린샷 비교다.
- 커밋, 브랜치, push, PR, staging은 만들지 않았다.
