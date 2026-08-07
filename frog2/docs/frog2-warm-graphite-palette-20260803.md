# frog2 Warm Graphite 팔레트 적용 보고서

작성일: 2026-08-03 KST

## 적용 내용

- Light-only 원칙과 기존 기능·DOM·레이아웃을 유지했다.
- 차가운 청회색 중립 팔레트를 따뜻한 Graphite 중립색으로 교체했다.
- 브랜드색은 Muted Slate Blue로 낮추고 활성 메뉴, 주요 버튼, 링크에만 사용한다.
- 보조 텍스트와 일반 아이콘을 같은 중립색으로 통일했다.
- 제목·카드·고객사·정기점검의 장식 아이콘에서 불필요한 브랜드색을 제거했다.
- 대시보드 경고·위험 KPI는 제목을 중립색으로 바꾸고 2px 상태 표시만 유지했다.
- 성공·경고·위험 색은 실제 상태 배지와 피드백에만 남겼다.

## 팔레트

| 역할 | 색상 |
| --- | --- |
| canvas / surface / muted surface | `#F7F7F5` / `#FFFFFF` / `#F2F2EF` |
| border / control border | `#E3E4E1` / `#8A9099` |
| heading / body / muted | `#202124` / `#3C4043` / `#6B7178` |
| brand subtle / brand / hover | `#EEF2F5` / `#3F5F78` / `#314861` |
| success subtle / success | `#EEF7F2` / `#287A55` |
| warning subtle / warning | `#FFF7E6` / `#8A5A00` |
| danger subtle / danger | `#FFF1F2` / `#C2414B` |

## 검증

- `./gradlew clean test check war`: 2회 연속 성공.
- Java 테스트: 299개, 실패·오류·건너뜀 0.
- 두 WAR SHA-256:
  `7ad6eb364dc5d8a194916b68e579fa6596ca09509bdd2a596b3df4ee011e4483`.
- JspC: JSP 35개 빌드, Java 36개와 class 61개 생성, 오류 0.
- JavaScript 23개 문법 검사 성공, `git diff --check` 성공.
- 대시보드 Light/OS Dark 10개 상태와 6개 도메인 24개 상태 검증: 실패 0.
- 라이브 로그인 8개 반응형/OS 테마 상태 검증: 실패 0.
- 본문/캔버스 9.76:1, 보조문구/캔버스 4.60:1,
  브랜드/흰색 6.73:1, 컨트롤 경계/흰색 3.22:1.
- 결과와 스크린샷: `/root/frog2-warm-graphite-validation-20260803`.

## 개발 배포와 운영 무영향

- 개발 URL: `http://192.168.40.70:18081/frog2/login`
- 개발 `tomcat-dev.service`: active/running, PID 857975.
- 개발 배포 WAR SHA-256:
  `7ad6eb364dc5d8a194916b68e579fa6596ca09509bdd2a596b3df4ee011e4483`.
- 운영 `tomcat.service`: PID 1012286로 작업 전후 동일.
- 운영 WAR SHA-256:
  `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`로 동일.
- 운영 로그인과 대시보드 비인증 응답 본문은 작업 전후 바이트 단위로 동일하다.
- DB DDL/DML과 인증 POST는 실행하지 않았다.

## 롤백

적용 직전 개발 백업:

- `/opt/frog2-dev/backups/warm-graphite-20260803-153000/frog2.war`
- `/opt/frog2-dev/backups/warm-graphite-20260803-153000/frog2-exploded`
- `/opt/frog2-dev/backups/warm-graphite-20260803-153000/frog2-work`

롤백 시 `tomcat-dev.service`만 중지하고 위 WAR와 exploded app을 복원한 뒤
개발 서비스만 시작한다. 운영 서비스와 `/opt/tomcat`은 변경하지 않는다.
