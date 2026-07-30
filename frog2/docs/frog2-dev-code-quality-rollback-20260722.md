# frog2 개발 코드 품질 배포 롤백

이 문서는 2026-07-22 개발 전용 배포만 되돌리는 절차다. 운영 unit
`tomcat.service`, 운영 경로 `/opt/tomcat`, 운영 포트 8080과 공유 DB에는 어떤
변경 명령도 실행하지 않는다.

## 배포 기준

- 개발 unit: `tomcat-dev.service`
- 개발 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- 개발 exploded app: `/opt/tomcat-dev/webapps/frog2`
- 배포 백업: `/opt/frog2-dev/backups/code-quality-final-20260722_114325`
- 배포 전 WAR: `frog2.war.before`
- 배포 전 exploded app: `frog2.exploded.before/`
- 격리한 stale Jasper cache: `tomcat-work.stale-before-csrf-fix/`
- 배포 전 WAR SHA-256: `2a2bc9fc188504dbec278dc9fa432c19c3a330154b17559af880e5d6bc4bf8e1`
- 배포 WAR SHA-256: `6e765f6c25810f41b432cef4e363581e84dd0aaabcd06a283f2de947fb5d25bb`
- 개발 외부 설정: `/opt/frog2-dev/config/db.properties`
- 개발 파일 저장소: `/opt/frog2-dev/data/files`

## 롤백 절차

1. 운영 기준을 읽기 전용으로 기록한다.

   ```bash
   systemctl show tomcat.service -p MainPID -p ActiveState -p SubState
   sha256sum /opt/tomcat/webapps/frog2.war /opt/tomcat/conf/server.xml
   curl -sS --max-time 5 -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/frog2/login
   ```

2. 개발 Tomcat만 중지한다.

   ```bash
   systemctl stop tomcat-dev.service
   systemctl is-active tomcat-dev.service
   ```

   두 번째 명령 결과가 `inactive`인지 확인한다. 운영 Tomcat은 중지하지 않는다.

3. 현재 개발 배포를 삭제하지 않고 백업 폴더 아래에 격리한다.

   ```bash
   install -d -m 0750 /opt/frog2-dev/backups/code-quality-final-20260722_114325/rollback-current
   mv /opt/tomcat-dev/webapps/frog2.war /opt/frog2-dev/backups/code-quality-final-20260722_114325/rollback-current/frog2.war.after
   mv /opt/tomcat-dev/webapps/frog2 /opt/frog2-dev/backups/code-quality-final-20260722_114325/rollback-current/frog2.exploded.after
   mv /opt/tomcat-dev/work/Catalina/localhost/frog2 /opt/frog2-dev/backups/code-quality-final-20260722_114325/rollback-current/tomcat-work.after
   ```

   `rollback-current`가 이미 존재하거나 대상 파일이 있으면 덮어쓰지 말고 중단한다.
   Jasper cache가 없으면 세 번째 `mv`만 생략한다. 격리된
   `tomcat-work.stale-before-csrf-fix`는 복원하지 않는다.

4. 2026-07-22 배포 직전 WAR와 exploded app을 복원한다.

   ```bash
   cp -a /opt/frog2-dev/backups/code-quality-final-20260722_114325/frog2.war.before /opt/tomcat-dev/webapps/frog2.war
   cp -a /opt/frog2-dev/backups/code-quality-final-20260722_114325/frog2.exploded.before /opt/tomcat-dev/webapps/frog2
   ```

5. 개발 Tomcat만 기동하고 복원을 확인한다.

   ```bash
   systemctl start tomcat-dev.service
   systemctl show tomcat-dev.service -p MainPID -p ActiveState -p SubState
   sha256sum /opt/tomcat-dev/webapps/frog2.war
   curl -sS --max-time 5 -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18081/frog2/login
   ```

   복원 WAR 해시는
   `2a2bc9fc188504dbec278dc9fa432c19c3a330154b17559af880e5d6bc4bf8e1`이어야 한다.
   개발 JVM에는 `frog2.env=dev`, `frog2.readOnly=true`,
   `frog2.fileRepoRoot=/opt/frog2-dev/data/files`가 계속 적용되어야 한다.

6. 운영 PID, WAR·설정 해시와 8080 응답을 1단계 값과 다시 비교한다.

## 보존 원칙

- `/opt/frog2-dev/data/files`는 이동하거나 삭제하지 않는다.
- `/opt/frog2-dev/config/db.properties`의 내용은 출력하거나 변경하지 않는다.
- DB migration, DDL, DML과 비밀번호 도구는 실행하지 않는다.
- 백업과 `rollback-current`는 검증 전 삭제하지 않는다.
- 운영 파일, 운영 서비스와 실제 운영 자료실은 변경하지 않는다.
