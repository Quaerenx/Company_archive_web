# Archive 단계별 감사 결과

- 감사일: 2026-08-10
- 대상 저장소: `/opt/frog2-dev/repo/frog2`
- 기준 브랜치/HEAD: `develop` / `21b06b55150c`
- 수행 방식: 정적 분석, 기존 감사 증거 재검토, `/tmp` 복사본 빌드, 개발·운영 상태 읽기 전용 확인
- 변경 제한: DB DDL/DML, 인증 쓰기 E2E, Tomcat 재시작·배포, commit·push·PR을 수행하지 않음

## 보고서

1. [저장소·빌드·의존성·배포 기준선](01-build-deployment-baseline-audit.md)
   - [기준선 개선 조치 결과](01-build-deployment-baseline-remediation.md)
2. [데이터베이스 접근 구조·데이터 무결성](02-database-integrity-audit.md)
3. [인증·권한·세션·CSRF·파일 자료실 보안](03-security-auth-file-repository-audit.md)
   - [보안 개선 조치 결과](03-security-auth-file-repository-remediation.md)
4. [업무 기능·도메인 구현](04-domain-functionality-audit.md)
5. [성능·동시성·자원 관리·관측성](05-performance-observability-audit.md)
6. [프론트엔드·접근성·반응형·최종 릴리스 준비](06-frontend-release-readiness-audit.md)

## 공통 검증 기준선

| 항목 | 결과 |
| --- | --- |
| Java 22 clean build | 반복 성공, 최종 재검증 성공 |
| 단위 테스트 | 383개, 실패 0, 오류 0, skipped 0 |
| JspC | Jasper 10.1.41 소스 생성 + Java 22 컴파일, 생성 Java 38개, class 63개, 오류 0 |
| JavaScript/MJS 문법 | 전체 성공 |
| WAR 검증 | allowlist·금지 파일·Vertica JDBC checksum 성공 |
| `git diff --check` | 성공 |
| 개발 WAR | 보안 변경 새 WAR는 미배포; 실행 중 WAR와 소스 빌드는 의도적으로 다름 |
| 운영 무영향 | PID·WAR 해시 유지, 로그인 GET 200 |

현재 작업 트리에는 감사 전부터 존재한 사용자 작업과 1~3단계 개선 변경이 함께 있다. 기존 변경은 되돌리지 않았고, commit·push·Tomcat 배포는 수행하지 않았다.
