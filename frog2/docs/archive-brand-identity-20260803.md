# Archive 명칭 및 제품 정체성

## 공식 표기

- 제품명: `Archive`
- 제품 설명: `고객 운영 업무공간`
- 브라우저 제목: `{페이지 이름} | Archive`
- 자연어 호칭: `아카이브`

사용자 화면에서는 `ARCHIVE`, `frog2`, `게시판 시스템`, `Company Inc.`를 제품
명칭으로 사용하지 않는다.

## 적용 화면

- 공통 상단 로고와 대시보드 접근성 이름
- 공통 브라우저 제목
- 로그인 로고, 메타 설명, 제품 설명, 푸터
- 공통 푸터
- 400, 403, 404, 405, 409, 500, 503 오류 화면 제목
- WAR 표시명

## 기술 식별자

기존 주소와 런타임 계약을 깨지 않기 위해 다음 기술 식별자는 유지한다. 이 값은
사용자 화면에 제품명으로 노출하지 않는다.

- 컨텍스트 경로 `/frog2`
- `frog2.*` 시스템 속성 및 설정 키
- `frog2AssetVersion`
- `Frog2UI`, `Frog2Csrf` JavaScript 공개 객체
- Java 클래스, 로그 파일, 저장소 경로에 포함된 `frog2`

## 검증 및 배포

- 전체 Java 테스트 302개, 실패·오류·건너뜀 0
- clean build 2회 및 재현 WAR 생성 성공
- JspC: JSP 35개, 생성 Java 36개, class 61개, 오류 0
- Firefox: 390/768/1024/1440px 및 OS Light/Dark 8상태, 실패 0
- 사용자 화면의 `ARCHIVE`, `게시판 시스템`, `Company Inc.` 노출 0건
- 개발 WAR SHA-256:
  `297b4e5845182f7ff4027adb5ed08ca96d0ab25330f61d4bf863857b73fb24ad`
- 개발 백업: `/opt/frog2-dev/backups/archive-brand-20260803-160513`
- 운영 PID, WAR SHA-256, 8080 로그인 및 비인증 대시보드 응답은 변경 없음
- DB DDL/DML과 인증 POST는 실행하지 않음

개발 접속 주소는 기존과 동일하다.

`http://192.168.40.70:18081/frog2/login`
