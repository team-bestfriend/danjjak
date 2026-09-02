# 인프라 실행 가이드

로컬 개발용 MySQL과 Flyway를 Docker로 띄웁니다.
백엔드(Tomcat)와 프론트엔드는 컨테이너를 쓰지 않고 각자 실행합니다.

## 준비물

- Docker Desktop (실행 중이어야 합니다)

## 최초 1회 설정

```powershell
cd C:\danjjak\infra
Copy-Item .env.example .env
```

`.env` 를 열어 `MYSQL_ROOT_PASSWORD` 를 아무 값으로 채웁니다. 로컬 전용이라
복잡할 필요 없습니다. 나머지 값은 그대로 두세요 — 백엔드의
`application.properties` 기본값과 맞춰져 있어서, 바꾸면 백엔드가 접속하지 못합니다.

`.env` 는 커밋되지 않습니다. 각자 만들어야 합니다.

## 실행

**반드시 `infra` 폴더에서 실행하세요.** `.env` 를 여기서 읽습니다.

```powershell
cd C:\danjjak\infra
docker compose up -d ; docker compose logs flyway --tail 20
docker compose ps
```

`mysql` 의 STATUS 가 `(healthy)` 가 되면 준비 완료입니다. 첫 실행은 30초쯤 걸립니다.

`flyway` 는 `docker compose ps` 에 안 보입니다. 마이그레이션을 한 번 실행하고
끝나는 작업이라 정상입니다. 종료된 것까지 보려면 `docker compose ps -a` 를 쓰고,
`Exited (0)` 이면 성공입니다.

**마이그레이션이 실패해도 `up -d` 는 성공한 것처럼 보입니다.** 그래서 위 실행 명령에
로그 확인을 붙여두었습니다. pull 을 받거나 SQL 파일을 추가한 뒤에는 반드시 확인하세요.

```powershell
docker compose logs flyway --tail 20
```

## 중지와 삭제

```powershell
docker compose down      # 컨테이너만 중지. 데이터는 남습니다
docker compose down -v   # 볼륨까지 삭제. DB 데이터가 전부 사라집니다
```

**둘의 차이가 중요합니다.** 평소에는 `down` 을 쓰세요.
`-v` 는 DB를 처음 상태로 되돌리고 싶을 때만 씁니다.

## 마이그레이션

SQL 파일은 이 폴더가 아니라 저장소 루트의 **[`db/migration/`](../db/migration)** 에 있습니다.
스키마는 특정 팀의 소유가 아니라 `contracts/openapi.yaml` 과 같은 공동 계약이라서입니다.

파일을 넣고 `docker compose up -d` 하면 자동으로 적용됩니다.

파일 이름 규칙, 번호 선점, 수정 금지 규칙은 [`db/README.md`](../db/README.md)를 보세요.

`infra` 가 관리하는 것은 **실행 환경**입니다 — Flyway 버전, 접속 정보, 실행 시점.

## 백엔드 연결

기본 설정 그대로면 백엔드는 아무 설정 없이 접속됩니다.

```
jdbc:mysql://localhost:3306/danjjak
사용자 danjjak / 비밀번호 danjjak_local
```

`.env` 에서 `MYSQL_PORT` 를 바꿨다면 백엔드에도 환경변수를 줘야 합니다.

```
DANJJAK_DB_URL=jdbc:mysql://localhost:13306/danjjak?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
```

## 문제 해결

### `port is already allocated`

3306을 다른 것이 쓰고 있습니다. 범인을 먼저 찾으세요.

```powershell
docker ps
netstat -ano | findstr :3306
```

**다른 프로젝트의 컨테이너인 경우** — 멈추면 됩니다.

```powershell
docker stop 컨테이너이름
```

껐는데 자꾸 다시 켜진다면 재시작 정책이 걸려 있는 겁니다.

```powershell
docker update --restart=no 컨테이너이름
docker stop 컨테이너이름
```

이래도 Docker Desktop에서 수동으로 켜는 것은 그대로 됩니다.

**로컬에 설치된 MySQL인 경우** (`tasklist` 로 `mysqld.exe` 확인) —
끄지 말고 `.env` 의 `MYSQL_PORT` 를 `13306` 으로 바꾸세요.
백엔드 연결 항목의 `DANJJAK_DB_URL` 도 같이 설정해야 합니다.

### `MYSQL_ROOT_PASSWORD_is_required`

`.env` 가 없거나 값이 비어 있습니다. 최초 1회 설정을 다시 하세요.
`infra` 폴더가 아닌 곳에서 실행했을 때도 이 에러가 납니다.

### STATUS 가 `healthy` 로 안 바뀜

```powershell
docker compose logs mysql
```

첫 30초 동안 헬스체크가 실패하는 것은 정상입니다.
1분이 지나도 안 되면 로그를 확인하세요.

### 한글이 깨짐

DB 문자셋을 확인합니다.

```powershell
docker compose exec mysql mysql -udanjjak -p비밀번호 -e "SHOW VARIABLES LIKE 'character_set%'"
```

`character_set_database` 와 `character_set_server` 가 `utf8mb4` 여야 합니다.
`latin1` 이면 볼륨을 지우고 다시 만들어야 합니다.

```powershell
docker compose down -v
docker compose up -d
```

PowerShell 콘솔에서만 깨져 보이는 경우도 있습니다. `chcp 65001` 을 먼저 실행해보세요.

### `Migration checksum mismatch`

누군가 이미 적용된 마이그레이션 파일을 수정했습니다.
최신 코드를 받은 뒤 DB를 초기화하세요.

```powershell
git pull
docker compose down -v
docker compose up -d
```

**그리고 슬랙에 알리세요.** 팀원 전원이 같은 작업을 해야 합니다.
