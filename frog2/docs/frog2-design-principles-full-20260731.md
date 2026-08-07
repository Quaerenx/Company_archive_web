# frog2 디자인 원칙 전체 적용 기록

## 목적과 범위

- 기준 문서: `software-engineers-design-principles-ko-full.md` (2026-05 개정, 1,666줄)
- 적용 대상: frog2 개발 서버의 JSP, 공통 JavaScript, 공통/페이지 CSS
- 보존 대상: URL, 폼 필드와 요청 계약, 인증·권한·DB 동작, 기존 카드와 폼 구조
- 제외 대상: 운영 Tomcat/WAR/설정, DB DDL/DML, 신규 외부 UI 프레임워크

이 기록에서 "적용"은 문서의 예제 코드를 그대로 복사했다는 뜻이 아니다. frog2의 업무 목적에 맞는 원칙을 기존 서버 렌더링 구조에 구현했다는 뜻이다. 제품에 AI 기능이나 Figma 원본이 없어 실행할 수 없는 항목은 이유와 함께 `해당 없음`으로 기록한다.

## Part 1. 기초 원칙

### Gestalt 심리학

| 원칙 | 적용 내용 | 구현 근거 |
| --- | --- | --- |
| 근접성 | 레이블·입력·도움말은 좁게, 필드 그룹·섹션은 넓게 묶었다. | `tokens.css`의 4/8/16/24px 리듬, `ui-system.css`의 폼/상세 섹션 |
| 유사성 | 같은 기능은 공통 버튼·배지·알림·테이블·폼 상태를 사용한다. | `.ui-button`, `.ui-badge`, `.ui-alert`, `.ui-table`, `.ui-form` |
| 전경-배경 | 캔버스, 표면, 높은 표면과 overlay를 의미 토큰으로 구분했다. | `--color-background`, `--color-surface*`, `--color-overlay*`, shadow 단계 |
| 공통 영역 | 페이지 헤더, 카드, 폼, 상세, modal 경계를 공통 규칙으로 묶었다. | `content-shell`, `page-header`, `ui-form-card`, dialog controller |
| 연속성 | header와 모든 본문을 하나의 1018px 축에 맞추고 제목→상태→업무 순서를 유지했다. | `--page-content-max-width`, `.content-shell`, 각 페이지 container |
| 폐합 | 불필요한 장식은 추가하지 않고 아이콘·부분 경계·빈 상태만으로 의미를 완성했다. | 상태 아이콘+텍스트, empty state, 조용한 카드 경계 |

### 타이포그래피

- 시스템 우선 한글 글꼴을 사용한다. 외부 폰트 다운로드로 첫 렌더링을 막지 않는다.
- 본문은 `1rem/1.6`, 장문은 `1.75` 행간을 사용한다.
- 제목은 1.25 비율의 `1.25 / 1.563 / 1.953 / 2.441rem` 단계로 제한한다.
- 읽기 본문은 `52ch / 65ch / 75ch` measure를 사용하고, 데이터 표는 업무 밀도를 위해 별도 폭을 유지한다.
- 페이지 CSS의 직접 `font-size: px/rem/em` 선언을 없애고 전부 토큰을 사용한다.

### 색채 이론과 접근성

- 60% 캔버스, 30% 표면, 10% 행동/상태색이라는 역할 구조를 토큰 주석과 실제 계층에 반영했다.
- primary/secondary/success/warning/danger/info와 text/border/surface/focus를 의미 기반으로 분리했다.
- 상태는 색만 사용하지 않고 아이콘, 레이블, 배지 텍스트와 함께 전달한다.
- 라이트/다크 모드에서 일반 텍스트, muted text, link, 상태색, 강한 버튼색, 상태 메시지 쌍을 자동 대비 검사한다.
- 다크 모드는 단순 반전이 아니다. 표면 채도를 낮추고 accent 명도를 높이며 shadow와 overlay를 별도로 조정한다.

### 시각적 위계

- 각 화면의 공통 page header와 핵심 CTA를 첫 단계로 둔다.
- 제목/설명/metadata를 크기, 굵기, 색상 세 단계로 구분한다.
- 대시보드는 확인 필요·라이선스 위험·예정·완료와 월간 정기점검을 보조 VM 영역보다 먼저 배치한다.
- modal과 toast만 높은 elevation을 사용하고 일반 카드는 낮은 경계를 사용한다.

### 여백과 리듬

- 기본 간격은 8px 배수이며 2px/4px는 테두리와 레이블 같은 미세 관계에만 사용한다.
- 모든 주요 콘텐츠 폭은 `--page-content-max-width` 하나로 통일했다.
- 모바일/태블릿/데스크톱 기준은 신규 코드에서 480/768/1024px로 제한했다.
- `-1px`은 tab 경계 겹침과 스크린리더 전용 숨김 기법에서만 허용한다.

### 애니메이션

- 80/120/180/320ms와 표준 easing을 token으로 관리한다.
- `transition: all`을 제거하고 변경되는 속성만 지정한다.
- 정기점검 이력의 등장 모션은 `transform`과 `opacity`만 사용하며 항목 수에 따라 지연을 제한한다.
- `prefers-reduced-motion`에서는 CSS/JS 모션을 즉시 종료하고 smooth scroll도 사용하지 않는다.
- 오류, 검색 결과, 이미 로드된 콘텐츠에 장식용 spinner나 지연을 추가하지 않는다.

## Part 2. 인터랙션과 AI

### 인터랙션 패턴

| 패턴 | frog2 적용 |
| --- | --- |
| 직접 조작 | 카드 전체를 가짜 click target으로 만들지 않고 실제 링크·버튼을 제공한다. 표/카드 행 click은 보조 기능만 담당한다. |
| 맥락 인식 | 요청 경로로 현재 메뉴를 판단하고 상세·수정에서도 상위 메뉴를 유지한다. 목록의 검색·정렬·페이지 query를 상세/수정/복귀 링크로 전달한다. |
| 지능형 활성화 | 모바일 메뉴, 필터 상태, 댓글 수정 폼, retry와 삭제 동작은 필요한 순간에만 나타난다. 관련 없는 비활성 control을 상시 노출하지 않는다. |
| 듀얼 모드 | desktop/mobile navigation과 light/dark 환경은 같은 의미 계약을 유지하면서 표현만 바뀐다. 업무 기능 자체를 임의의 mode로 분리하지 않는다. |
| 구조화된 콘텐츠 | 목록→상태 요약→카드/행→상세의 단계로 공개하고, modal은 현재 맥락을 유지하는 보조 편집에만 사용한다. |
| 점진적 학습 | 보이는 `빠른 이동` 버튼과 Ctrl/Cmd+K를 함께 제공한다. 결과는 방향키/Home/End/Enter/Escape로 조작하며 input 편집 중 단축키는 가로채지 않는다. |

공통 dialog controller가 초기 포커스, Tab 가두기, Escape, 닫은 뒤 원래 trigger로의 포커스 복귀를 담당한다. 제출 중에는 `aria-busy`와 버튼 잠금을 적용하고, 오류는 field error/alert/toast와 재시도 동작으로 복구할 수 있게 한다.

### AI 인터페이스 패턴 — 해당 없음

frog2에는 생성형 AI 출력, citation, streaming 추론, confidence 또는 agent workflow 기능이 없다. 가짜 출처·신뢰도·처리 단계를 추가하면 오히려 문서의 정직성 원칙을 위반하므로 구현하지 않는다. 향후 AI 기능이 생기면 인용 우선, 단계 공개, 오류/불확실성 공개, 검증 checkpoint를 별도 수용 기준으로 적용한다.

## Part 3. Dieter Rams 10원칙

1. 기존 JSP 구조 안에서 native CSS/JS 기능을 사용해 점진적으로 개선했다.
2. 모든 새 요소는 탐색, 상태 전달, 오류 복구 또는 접근성 역할을 가진다.
3. 중립 표면과 절제된 accent로 일상적인 업무 화면의 피로를 줄였다.
4. 실제 링크, 명시적 label, 활성 메뉴, 상태 문구로 사용법이 드러나게 했다.
5. navigation과 장식은 뒤로 물리고 고객 데이터와 해야 할 업무를 앞으로 보냈다.
6. loading/error/empty/disabled 상태를 숨기지 않고 명시한다.
7. 유행성 gradient, glassmorphism, neon, 과도한 motion을 사용하지 않았다.
8. keyboard, focus, hover, active, disabled, loading, empty, error, dark, reduced-motion 상태를 계약에 포함했다.
9. 외부 프레임워크·폰트와 불필요한 JS를 추가하지 않고 CSS/DOM 중복을 줄였다.
10. 의미 없는 click handler, `href="#"`, 산재한 색상·폰트 값을 제거했다.

## Part 4. 구현 패턴

| 문서 항목 | 상태 | 판단 |
| --- | --- | --- |
| Container Queries | 적용 | 대시보드 정기점검 grid가 viewport가 아닌 자체 container 폭에 반응한다. |
| `:has()` | 적용+fallback | 오류가 있는 form group의 label을 강조한다. 기존 class/ARIA 기반 규칙도 남긴다. |
| `@starting-style` | 적용+fallback | toast/modal 진입을 보완하며 기존 keyframe/정적 표시가 fallback이다. |
| CSS nesting | 미적용 | 현재 CSS/JspC 빌드와 장기 브라우저 호환을 위해 평탄한 selector를 유지한다. 원칙 적용에 필수 기능이 아니다. |
| HTMX | 미적용 | 신규 runtime dependency와 서버 계약 변경 없이 현재 JSP/Servlet을 유지한다. |
| Anchor positioning | 해당 없음 | 이번 범위에 tooltip/popover처럼 anchor가 필요한 기능이 없다. |
| Scroll-driven animation | 미적용 | 업무 화면에 정보를 추가하지 않는 장식 motion이므로 의도적으로 제외한다. |
| Design tokens | 적용 | 색, 타입, 간격, radius, control, icon, shadow, motion, layout을 중앙화했다. |
| Dark mode | 적용 | OS 설정을 따르는 별도 semantic palette와 대비 계약을 추가했다. |
| Figma extraction | 해당 없음 | Figma 파일/variables가 제공되지 않았다. 현재 CSS token이 향후 handoff의 기준이다. |

## 자동 회귀 계약

`DesignPrinciplesFullCoverageTest`가 다음을 고정한다.

- 페이지 CSS의 하드코딩 hex/rgb/hsl, 직접 font size, `!important`, 비표준 breakpoint 금지
- 전체 CSS의 `transition: all` 금지와 미정의 custom property 금지
- light/dark 의미색 WCAG AA 대비
- 공통 폭, 1.25 타입 스케일, 45–75ch measure, 8px 간격, 44px control, motion token
- skip link/main landmark, command palette keyboard, modal controller, loading/error/retry 계약
- `href="#"` 금지와 native confirm의 공통 helper 단일 진입점
- container query, `:has()`, `@starting-style`, reduced-motion fallback

## 운영 안전

- 이 작업은 인증·권한·Servlet·DAO·DB schema/data 계약을 변경하지 않는다.
- 실제 DB 쓰기 요청을 디자인 검증에 사용하지 않는다.
- 운영 Tomcat, 운영 WAR, 운영 설정은 변경하거나 재시작하지 않는다.
- 개발 배포 전 WAR와 exploded app 백업을 유지하며, 문제 발생 시 해당 백업으로 개발 서버만 복원한다.

## 정량 변화와 검증 결과

- 공통 token 밖의 하드코딩 색상: 248건 → 0건
- 직접 숫자 font size: 180건 → 0건
- 전체 `!important`: 299건 → 33건(약 89% 감소), 페이지 CSS는 0건
- `transition: all`: 8건 → 0건
- 주요 본문 폭: 화면별 값 → `--page-content-max-width: 1018px` 단일 계약
- clean build, test, check, WAR 생성 2회 연속 성공
- 전체 테스트 286건 통과, 실패 0건
- JspC: JSP/JSPF/tag 43개 입력, 생성 오류 0건
- JavaScript 23개 파일 문법 검사 통과
- WAR 내용 allowlist 검사 통과, 두 clean build의 WAR SHA-256 일치
- 실제 Firefox에서 로그인 500/768/1024/1440px 가로 넘침 0, focus, card boundary, reduced motion 통과
- mock 인증 화면에서 360/390/768/1024/1440px shell 폭, mobile menu, dialog focus trap/복귀, Ctrl/Cmd+K, dark mode와 대비 검사 통과
- Firefox headless의 최소 실제 viewport가 500px여서 360/390px 실서버 확인은 정적 CSS 계약과 mock 화면으로 보완했다.

## 개발 배포와 운영 무영향 결과

- 배포 시각: 2026-07-31 23:55 KST
- 개발 WAR SHA-256: `9cd4fdb96cdec43bf51be4d65223338a5c07d81051b40b54789feec0a6d79506`
- 개발 로그인 GET: HTTP 200, 신규 asset version 확인
- 개발 비인증 dashboard GET: HTTP 302 → `/frog2/login`
- 배포 이후 `SEVERE`, `JasperException`, `ClassNotFoundException`, `NoSuchMethodError`, `LinkageError`, `OutOfMemoryError`: 0건
- 운영 PID: `1012286` 유지
- 운영 WAR SHA-256: `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88` 유지
- 운영 8080 로그인 GET: HTTP 200 유지

## 개발 롤백 자료

- 이전 개발 WAR: `/opt/frog2-dev/backups/design-principles-full-20260731_232131/frog2.war`
- 이전 개발 exploded app: `/opt/frog2-dev/backups/design-principles-full-20260731_232131/pre-deploy-live/frog2-exploded`
- 이전 개발 JSP work cache: `/opt/frog2-dev/backups/design-principles-full-20260731_232131/pre-deploy-live/frog2-work`
- 롤백 시에는 `tomcat-dev.service`만 중지하고 현재 개발 산출물을 별도 보존한 뒤 위 세 자료를 개발 경로에 복원한다. 운영 `tomcat.service`와 `/opt/tomcat`은 조작하지 않는다.
