# frog2 개발 2차 배포 롤백

이 문서는 2026-07-20 개발 전용 배포를 되돌리기 위한 절차다. 운영 unit
`tomcat.service`, 운영 경로 `/opt/tomcat/webapps`, 운영 포트 8080에는 어떤
명령도 실행하지 않는다.

## 배포 기준

- 개발 unit: `tomcat-dev.service`
- 개발 WAR: `/opt/tomcat-dev/webapps/frog2.war`
- 개발 exploded app: `/opt/tomcat-dev/webapps/frog2`
- 배포 백업: `/opt/frog2-dev/backups/deploy-20260720_194005`
- 배포 전 WAR SHA-256: `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`
- 배포 WAR SHA-256: `d89440526c89231717e59046efa10f35b4ac90d0e44240021d61f1a6de5e1afb`
- 개발 JVM drop-in: `/etc/systemd/system/tomcat-dev.service.d/20-frog2-safety.conf`
- 개발 외부 DB 설정: `/opt/frog2-dev/config/db.properties`
- 개발 파일 저장소: `/opt/frog2-dev/data/files`

백업에는 아래 파일이 있다.

- `frog2.war.before`
- `frog2-exploded.before/`
- `frog2.war.live-before-deploy`
- `frog2-exploded.live-before-deploy/`
- `tomcat-dev.service.before`

## 전체 롤백 절차

1. 운영 상태를 먼저 읽기 전용으로 기록한다.

   ```bash
   systemctl show tomcat.service -p MainPID -p ActiveState -p SubState
   sha256sum /opt/tomcat/webapps/frog2.war
   curl -sS --max-time 5 -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8080/frog2/login
   ```

2. 개발 Tomcat만 중지하고 현재 배포 파일을 삭제하지 않고 격리한다.

   ```bash
   systemctl stop tomcat-dev.service
   install -d -m 0700 /opt/frog2-dev/backups/deploy-20260720_194005/rollback-quarantine
   mv /opt/tomcat-dev/webapps/frog2.war /opt/frog2-dev/backups/deploy-20260720_194005/rollback-quarantine/frog2.war.phase2
   mv /opt/tomcat-dev/webapps/frog2 /opt/frog2-dev/backups/deploy-20260720_194005/rollback-quarantine/frog2-exploded.phase2
   ```

3. 배포 전 WAR와 exploded app을 복원한다.

   ```bash
   cp -a /opt/frog2-dev/backups/deploy-20260720_194005/frog2.war.before /opt/tomcat-dev/webapps/frog2.war
   cp -a /opt/frog2-dev/backups/deploy-20260720_194005/frog2-exploded.before /opt/tomcat-dev/webapps/frog2
   ```

4. 2차 배포의 개발 JVM drop-in을 삭제하지 않고 백업으로 이동한 뒤 기존
   DB 설정 권한을 복원한다.

   ```bash
   mv /etc/systemd/system/tomcat-dev.service.d/20-frog2-safety.conf /opt/frog2-dev/backups/deploy-20260720_194005/20-frog2-safety.conf.disabled
   chown root:root /opt/frog2-dev/config /opt/frog2-dev/config/db.properties
   chmod 0700 /opt/frog2-dev/config
   chmod 0600 /opt/frog2-dev/config/db.properties
   systemctl daemon-reload
   ```

5. 개발 Tomcat만 시작하고 복원을 확인한다.

   ```bash
   systemctl start tomcat-dev.service
   systemctl show tomcat-dev.service -p MainPID -p ActiveState -p SubState
   sha256sum /opt/tomcat-dev/webapps/frog2.war
   curl -sS --max-time 5 -o /dev/null -w '%{http_code}\n' http://127.0.0.1:18081/frog2/login
   ```

   복원된 개발 WAR 해시는 배포 전 값
   `68e404808ba352e4827c6b3aa05c0ac0f20654de1cb67bef15333c2e79442c88`이어야 한다.

6. 운영 PID, WAR 해시, 8080 응답을 1단계 값과 다시 비교한다.

## 보존 원칙

- `/opt/frog2-dev/data/files`는 롤백 중에도 삭제하거나 이동하지 않는다.
- `/opt/frog2-dev/config/db.properties`의 내용은 출력하거나 복사하지 않는다.
- 운영 파일과 실제 운영 자료실 파일은 이동하거나 삭제하지 않는다.
- `rollback-quarantine`이 이미 존재하면 덮어쓰지 말고 새 타임스탬프 이름을 사용한다.
