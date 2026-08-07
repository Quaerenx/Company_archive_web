# frog2 전역 Minimal Light 디자인 적용 보고서

작성일: 2026-08-01 KST

상태: 개발 서버 전역 적용 및 검증 완료. 다크 모드는 사용자 요청에 따라 제외했다.

## 적용 범위

- 로그인, 대시보드, 고객사, 정기점검, 회의록·월간 대응, 트러블슈팅,
  자료실, 마이페이지, 오류 화면에 같은 시각 언어를 적용했다.
- 기존 URL, 데이터 구조, 폼 파라미터, 인증·권한, CSRF, 서버 계약은 유지했다.
- DB DDL/DML과 인증 POST는 실행하지 않았다.
- 운영 Tomcat, 운영 WAR, 운영 설정은 변경하거나 재시작하지 않았다.
- 외부 폰트, CDN, UI 프레임워크, 신규 런타임 의존성은 추가하지 않았다.

## Light-only 원칙

- 밝은 테마의 17개 불투명 색상만 의미 기반 토큰으로 사용한다.
- `color-scheme: light`를 명시하고 다크 토큰, 다크 테마 선택자,
  `prefers-color-scheme: dark` 분기를 두지 않는다.
- OS가 다크 모드여도 애플리케이션은 같은 Light UI를 표시한다.

## 주요 변경

- 일반 카드와 패널을 흰색 표면, 1px 테두리, 8px 모서리, 그림자 없음으로 통일했다.
- 그림자는 모달, 드롭다운, 토스트, 모바일 메뉴처럼 실제로 떠 있는 레이어에만 남겼다.
- 모든 페이지 너비와 좌우 여백은 공통 `content-shell` 한 곳에서 관리한다.
- 버튼, 폼, 알림, 상태 색, 포커스 표시를 공통 토큰과 컴포넌트 규칙으로 통일했다.
- 화면별 주요 CTA는 원칙적으로 하나만 강조하고 검색·보기·수정·목록은 보조 버튼으로 정리했다.
- 표는 수평 구분선 중심으로 바꾸고 세로선과 지브라 배경을 제거했다.
- hover 이동·확대, 그라데이션, 과도한 굵기, 페이지 CSS의 `!important`를 제거했다.
- 정기점검 차트의 축·범례·격자·툴팁 색도 JavaScript 하드코딩 대신 CSS 의미 토큰을 사용한다.

## 정량 변화

| 항목 | 적용 전 | 적용 후 |
| --- | ---: | ---: |
| CSS 불투명 색상 | 94 | 17 |
| Light 계열 불투명 색상 | 47 | 17 |
| 일반 surface 그림자 정의 | 43 | 0 |
| `font-weight: 800/900` | 6 | 0 |
| `font-weight: 700` | 18 | 2 |
| 그라데이션 | 4 | 0 |
| hover 이동·확대 | 8 | 0 |
| 토큰 밖 CSS/JS 색상 | - | 0 |
| 페이지 CSS `!important` | - | 0 |
| `transition: all` | - | 0 |
| 표 세로선·지브라 규칙 | - | 0 |

## 빌드 및 정적 검증

- `./gradlew clean test check war`: 최종 소스에서 2회 연속 성공.
- Java 테스트: 291개, 실패 0, 오류 0, 건너뜀 0.
- 두 빌드의 WAR SHA-256가 동일함:
  `c713066bbafa664d7fdb2f4149a11ce07283b79af400803a5a4c9281bc4563ab`.
- JspC: 36개 Java 생성, 61개 class 생성, 오류 0.
- JavaScript: 소스 23개 모두 `node --check` 성공.
- `git diff --check`: 성공.
- WAR에는 허용된 런타임 JAR 8개만 존재하며 source/Javadoc/test JAR,
  `.java`, IDE 파일, `build/classes/db.properties`는 없다.

## 브라우저 검증

- 대시보드: 360, 390, 768, 1024, 1440px에서 Light 및 OS Dark 조건 검증.
- 나머지 6개 도메인 대표 화면: 390, 1024px에서 Light 및 OS Dark 조건 검증.
- 라이브 로그인: 390, 768, 1024, 1440px에서 Light 및 OS Dark 조건 검증.
- 모든 조건에서 `color-scheme: light`, 가로 오버플로 0, 브라우저 오류 0.
- 모바일 메뉴 열기·Escape·포커스 복귀와 모달 포커스 복귀가 정상이다.
- 측정 대비: 본문 9.845:1, 보조문구 4.682:1, 포커스 7.038:1,
  컨트롤 경계 3.201:1, 경고 5.927:1, 위험 5.146:1.
- 결과 및 스크린샷:
  `/root/frog2-minimal-light-global-validation-20260801`.

## 개발 배포 결과

- URL: `http://192.168.40.70:18081/frog2/login`
- 서비스: `tomcat-dev.service`, active/running, PID 245753.
- 배포 WAR SHA-256:
  `c713066bbafa664d7fdb2f4149a11ce07283b79af400803a5a4c9281bc4563ab`.
- 로그인 GET 200, 대시보드 및 인증 필요 화면의 비인증 GET은 로그인으로 302.
- 최종 배포 이후 `SEVERE`, JSP 컴파일, class/linkage 오류는 0건이다.

첫 전역 배포 시 복사 방식이 원본의 `root:root 600` 권한을 보존하여 개발 Tomcat이
WAR를 읽지 못한 이력이 한 차례 있었다. 즉시 `tomcat-dev:tomcat-dev 640`으로 복구했고,
최종 배포는 대상 권한을 보존하는 방식으로 다시 수행했다. 이 과정에서도 운영 서비스는
변경되지 않았다.

## 운영 무영향

- 운영 `tomcat.service`: active/running, PID 1012286로 작업 전후 동일.
- 운영 WAR SHA-256:
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`로 동일.
- 운영 로그인 GET은 200/8697 bytes, 대시보드는 로그인으로 302이며 작업 전과 동일.
- 운영 로그인과 대시보드 비인증 응답 본문은 작업 전후 바이트 단위로 동일하다.

## 백업과 롤백

원래 디자인으로 복구하는 기준 백업:

- `/opt/frog2-dev/backups/minimal-light-global-20260801-162648/frog2.war`
- `/opt/frog2-dev/backups/minimal-light-global-20260801-162648/frog2-exploded`

최종 적용 직전 단계로 복구하는 백업:

- `/opt/frog2-dev/backups/minimal-light-global-final-20260801-164900/frog2.war`
- `/opt/frog2-dev/backups/minimal-light-global-final-20260801-164900/frog2-exploded`

롤백은 `tomcat-dev.service`만 중지한 뒤 선택한 WAR와 exploded app을
`/opt/tomcat-dev/webapps`에 복원하고, WAR 소유권/권한을
`tomcat-dev:tomcat-dev 640`으로 확인한 다음 개발 서비스만 시작한다.
운영 `tomcat.service`와 `/opt/tomcat`은 어떤 경우에도 변경하지 않는다.

## 남은 제한과 위험

- 공유 운영 DB 보호를 위해 로그인 POST와 인증된 데이터 화면을 실제 계정으로 열지 않았다.
- 따라서 인증 후 실제 데이터가 결합된 모든 시각 상태는 정적 계약, JspC,
  대표 DOM 목업 및 비인증 라이브 경로로 검증했다.
- 정기점검 차트는 JavaScript 문법과 토큰 계약을 검증했지만 인증된 라이브 데이터로
  렌더링하지 않았다.

현재 신뢰도: 93%. 추가로 필요한 최소 검증은 사용자가 개발 서버에서 읽기 전용으로
로그인한 뒤 대표 7개 화면을 육안 확인하는 것이다. 데이터 변경 버튼은 사용하지 않는다.
