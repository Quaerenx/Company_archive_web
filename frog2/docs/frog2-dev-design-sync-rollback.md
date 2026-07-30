# frog2 개발 디자인 보정 롤백

이 문서는 2026-07-21 개발 디자인 보정만 되돌리는 절차다. 운영 unit
`tomcat.service`, 운영 경로 `/opt/tomcat/webapps`, 운영 포트 8080에는 어떤
명령도 실행하지 않는다. DB migration, DDL, DML도 실행하지 않는다.

## 기준 경로

- 개발 unit: `tomcat-dev.service`
- 개발 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- 개발 exploded app: `/opt/tomcat-dev/webapps/frog2`
- 개발 Jasper cache: `/opt/tomcat-dev/work/Catalina/localhost/frog2`
- 보정 백업: `/opt/frog2-dev/backups/design-sync-20260721_132134`
- 보정 전 WAR SHA-256:
  `d89440526c89231717e59046efa10f35b4ac90d0e44240021d61f1a6de5e1afb`
- 보정 후 WAR SHA-256:
  `2a2bc9fc188504dbec278dc9fa432c19c3a330154b17559af880e5d6bc4bf8e1`

## 롤백 절차

1. 운영 PID, WAR 해시와 8080 로그인 응답을 읽기 전용으로 기록한다.

   ```bash
   systemctl show tomcat.service -p MainPID -p ActiveState -p SubState
   sha256sum /opt/tomcat/webapps/frog2.war
   curl -sS --max-time 5 -o /dev/null -w '%{http_code}\\n' http://127.0.0.1:8080/frog2/login
   ```

2. 개발 Tomcat만 중지하고 새 타임스탬프 격리 디렉터리를 만든다.

   ```bash
   systemctl stop tomcat-dev.service
   install -d -m 0700 /opt/frog2-dev/backups/design-sync-rollback-<timestamp>
   ```

3. 현재 개발 WAR, exploded app과 Jasper cache를 삭제하지 않고 격리한다.

   ```bash
   mv /opt/tomcat-dev/webapps/frog2.war /opt/frog2-dev/backups/design-sync-rollback-<timestamp>/frog2.war.corrected
   mv /opt/tomcat-dev/webapps/frog2 /opt/frog2-dev/backups/design-sync-rollback-<timestamp>/frog2-exploded.corrected
   mv /opt/tomcat-dev/work/Catalina/localhost/frog2 /opt/frog2-dev/backups/design-sync-rollback-<timestamp>/tomcat-work.corrected
   ```

   Jasper cache가 없으면 세 번째 `mv`만 생략한다.

4. 보정 직전 배포 파일을 복원한다.

   ```bash
   cp -a /opt/frog2-dev/backups/design-sync-20260721_132134/frog2.war.before /opt/tomcat-dev/webapps/frog2.war
   cp -a /opt/frog2-dev/backups/design-sync-20260721_132134/frog2-exploded.before /opt/tomcat-dev/webapps/frog2
   ```

5. 개발 Tomcat만 시작하고 복원된 WAR 해시와 로그인 응답을 확인한다.

   ```bash
   systemctl start tomcat-dev.service
   systemctl show tomcat-dev.service -p MainPID -p ActiveState -p SubState
   sha256sum /opt/tomcat-dev/webapps/frog2.war
   curl -sS --max-time 5 -o /dev/null -w '%{http_code}\\n' http://127.0.0.1:18081/frog2/login
   ```

6. 운영 PID, WAR 해시와 8080 응답이 1단계 값과 같은지 확인한다.

## 보존 원칙

- 백업과 격리 파일은 검증 전 삭제하지 않는다.
- `/opt/frog2-dev/data/files`와 운영 자료실 파일은 이동하거나 삭제하지 않는다.
- `/opt/frog2-dev/config/db.properties`의 내용은 출력하거나 변경하지 않는다.
- 기존 격리 디렉터리를 덮어쓰지 않는다.
