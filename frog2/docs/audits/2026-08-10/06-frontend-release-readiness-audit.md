# 6단계 — 프론트엔드·접근성·반응형·최종 릴리스 준비 감사

## 결론

공통 shell, 폭, 색상 token, 키보드 navigation과 반응형 기반은 안정적이다. 현재 릴리스 위험은 디자인 방향이 아니라 **현재 변경분의 시각 회귀 공백**, **차트·테이블 접근성**, **CSS 호환 계층**, **외부 CDN 무결성**이다.

- UI·접근성 건강도: **80/100**
- 전체 통합 건강도: **82/100**
- P0: 없음
- 운영 릴리스 판단: NO-GO

## 공통 화면 구조

- 인증 화면 25개가 공통 header/footer를 정확히 한 번 사용한다.
- 인증 화면 모두 공통 `pageHeader.tag`와 `.content-shell`을 사용한다.
- header와 main content 폭은 `--page-content-max-width: 1018px` 하나로 통일돼 있다.
- CSS 순서는 tokens → base → components → ui-system → utilities → ambient → header → page CSS다.
- 로그인과 오류 페이지를 제외한 인증 화면에는 skip link가 있다.

주요 근거:

- [includes/header.jsp](../../../src/main/webapp/includes/header.jsp)
- [core_styles.jspf](../../../src/main/webapp/WEB-INF/includes/core_styles.jspf)
- [pageHeader.tag](../../../src/main/webapp/WEB-INF/tags/pageHeader.tag)
- [tokens.css](../../../src/main/webapp/resources/css/tokens.css)

## CSS 수치

| 항목 | 결과 |
| --- | ---: |
| CSS 파일 | 33개 |
| CSS 줄 수 | 8,711줄 |
| CSS 크기 | 218,188 bytes |
| 공통 인증 CSS | 약 95,836 bytes, 압축 전 |
| 하드코딩 색상 | token 파일 밖 0건 |
| `!important` 텍스트 출현 | 34건 |
| 실제 `!important` 선언 | 32건, utility 28 + reduced-motion 4 |
| 주요 breakpoint | 480/768/1024px |
| 단발성 breakpoint | 576/1050/1200px 등 4종 |

페이지 규칙의 `!important` 남용은 없다. 다만 공통 `ui-button`과 레거시 `.btn`, `.button`, 도메인 전용 버튼이 공존한다. `ui-system.css`도 이를 명시적인 호환 계층으로 설명하고 있다.

## 색상·대비

주요 조합 측정:

| 조합 | 대비 |
| --- | ---: |
| strong text / surface | 14.64:1 |
| body text / surface | 7.46:1 |
| muted text / surface | 4.86:1 |
| brand / surface | 6.28:1 |
| success text / success background | 4.69:1 |
| warning text / warning background | 5.52:1 |
| danger text / danger background | 4.66:1 |

일반 텍스트 AA 기준을 만족한다. 차트는 blue/green/amber와 실선/점선·서로 다른 point shape를 함께 사용해 색상만으로 구분하지 않는다.

## 접근성

양호:

- navigation button의 `aria-expanded`, `aria-controls`, `aria-current`
- hover뿐 아니라 Enter/Space/Escape와 focus 이동
- mobile menu 외부 클릭·Escape·선택 후 닫기
- 공통 focus-visible
- modal focus trap·Escape·opener focus 복귀
- 공통 form error의 `aria-invalid`, `aria-describedby`, `role=alert`
- 로그인 floating label, current-password autocomplete, `100dvh`
- dashboard 완료 상태의 텍스트 대체 정보
- `prefers-reduced-motion`

잔여:

- 라이선스 chart canvas에 접근 가능한 제목·요약·표가 없음
- 표 7개 중 6개 caption 없음
- 표 7개 중 5개 `th scope="col"` 없음
- 공통 table pagination control 32×32px로 프로젝트 44px touch 기준 미달
- modal 배경에 `inert` 미적용
- horizontal table은 viewport overflow를 막지만 360px에서 사용자가 옆으로 스크롤해야 함

## 반응형·시각 회귀

기존 기준선:

- dashboard, customers, maintenance, meeting, troubleshooting, file repository, mypage
- 360, 768, 1024, 1440px
- 총 28장
- 당시 console error 0, document viewport overflow 0

현재 공백:

- 기준선 생성 뒤 로그인·고객사 상세·정기점검 이력 등 UI가 변경됐다.
- route manifest에 로그인, 고객사 상세, 정기점검 이력이 없다.
- 현재 변경분을 반영한 인증 Firefox profile이 없어 비밀값을 파일·명령에 남기지 않는 조건으로 재캡처하지 않았다.
- 따라서 기존 28장은 역사적 기준선이지 현재 39개 작업 트리 변경의 최종 증거가 아니다.

## 발견사항

### P1 — 현재 UI 변경분 화면 회귀 미검증

최소 조치: 로그인, 대시보드, 고객사 목록·상세, 정기점검 이력, 회의록, 트러블슈팅, 자료실, 마이페이지 9개 화면을 4개 폭으로 재캡처한다.

### P1 — chart 접근성

`maintenance_history.jsp`의 canvas 데이터가 screen reader에서 제공되지 않는다. 시각적으로 숨긴 데이터 table 또는 날짜·값 요약을 canvas와 연결해야 한다.

### P1 — 외부 CDN 무결성

Chart.js와 Font Awesome에 SRI가 없다. 자체 호스팅을 권장한다.

### P2 — CSS cascade·scope

`meeting.css`와 `meeting_view.css`가 `.meeting-header`, `.meeting-title` 등을 로딩 순서로 다시 정의한다. `customers.css` 등 일부 page CSS도 `body.page-*` 범위 없이 전역 selector를 사용한다. 이후 공통 class가 추가되면 의도하지 않은 화면까지 영향을 받을 수 있다.

### P2 — 표 접근성·터치 영역

caption/scope를 보완하고 32px pagination을 최소 44px로 늘려야 한다.

### P2 — JspC Java 19 fallback

JSP compile은 성공하지만 Java 22 계약과 도구가 일치하지 않는다.

### P2 — 정적 자산 정책

versioned asset도 5분 cache이고 자체 font 4개가 약 1.68MB다. 장기 immutable cache와 실제 사용 weight 점검이 필요하다.

### P3 — 구조 정리

- header의 페이지별 중복 max-width selector 제거
- 단발성 breakpoint를 480/768/1024 중심으로 통합
- modal open 시 background `inert` 검토
- `main_style.css` direct URL 로그 확인 후 제거
- unused Google Fonts CSP origin 제거

## 최종 통합 우선순위

### 1주

1. 39개 변경의 Git 기준선 확정
2. 9개 화면 × 4개 폭 재캡처
3. chart 접근 가능한 대체 정보
4. table caption/scope와 44px pagination
5. CDN 자체 호스팅 또는 SRI

### 1개월

1. page CSS를 `body.page-*`로 범위 제한
2. alert → button → form → table 순으로 레거시 class 제거
3. meeting CSS selector 통합
4. 빌드 전용 Java 22 JspC 환경
5. 검색·파일 cache·무제한 이력 성능 개선
6. request ID·pool wait·scan time 관측성

### 중기

1. 격리 DB·파일 저장소 쓰기 E2E
2. 실제 운영 HTTPS cookie audit
3. 역할별 접근 제어가 필요한지 제품 정책 확정
4. 대규모 본문 전문검색과 자료실 외부 index 검토

## 최종 검증

- clean build 반복 성공
- 360 tests, failures 0, errors 0
- JspC 46 inputs, 0 errors
- JavaScript/MJS syntax 성공
- `git diff --check` 성공
- 최근 개발 로그의 Jasper/ClassNotFound/NoSuchMethod/linkage/SEVERE/ERROR 패턴 없음
- 현재 빌드 WAR와 개발 배포 WAR 해시 일치
- 운영 PID·WAR 해시·8080 로그인 응답 유지

## 신뢰도와 최소 추가 검증

- 현재 신뢰도: **88%**
- 최소 추가 검증:
  1. 현재 소스 기준 9개 화면 × 4개 폭 재캡처
  2. 격리된 쓰기 E2E
  3. 실제 운영 HTTPS cookie 속성 확인
