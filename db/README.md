# 데이터베이스 스키마

`db/migration/` 은 단짝친구의 **DB 스키마 계약**입니다.
`contracts/openapi.yaml` 이 HTTP API의 기준이듯, 이 폴더가 테이블 구조의 기준입니다.

실행 환경(MySQL 컨테이너, Flyway 버전, 접속 정보)은 `infra/` 가 관리합니다.
여기는 **무엇을 바꿀지**만 담고, **어떻게 실행할지**는 담지 않습니다.

## 적용 방법

파일을 넣고 `infra` 에서 컨테이너를 올리면 Flyway가 자동으로 적용합니다.

```powershell
cd C:\danjjak\infra
docker compose up -d ; docker compose logs flyway --tail 20
```

`Successfully applied N migration(s)` 이 보여야 합니다.
자세한 실행·문제 해결은 [`infra/README.md`](../infra/README.md)를 보세요.

## 파일 이름 규칙

```
V{번호}__{설명}.sql
```

- 언더스코어 **두 개**입니다. 하나면 규칙 위반으로 **실행이 실패합니다**
  (`FLYWAY_VALIDATE_MIGRATION_NAMING` 이 켜져 있습니다)
- 번호는 정수, 설명은 영문 소문자와 언더스코어

```
V1__initial_schema.sql
V2__seed_demo_data.sql
V3__add_guardian_phone.sql
```

## 번호 선점

| 번호 | 내용 | 담당 |
|---|---|---|
| V1 | 초기 스키마 (테이블, 인덱스, 제약조건) | 김인범 |
| V2 | 시연용 시드 데이터 | 이준영 |
| V3 이후 | 이후 변경 | 슬랙에 번호를 먼저 알리고 사용 |

브랜치에서 두 사람이 같은 번호를 만들면 머지할 때 충돌합니다.
**파일을 만들기 전에 슬랙에 번호를 알리세요.** 30초면 되는 예방책입니다.

## 절대 규칙 — 적용된 파일은 수정하지 않습니다

Flyway는 파일 내용의 체크섬을 DB에 기록합니다.
이미 적용된 파일의 내용이 바뀌면 다음 실행에서 이렇게 됩니다.

```
Migration checksum mismatch for migration version 1
```

그 사람은 `docker compose down -v` 로 DB를 통째로 날려야 복구됩니다.
파일을 이미 공유했다면 **팀원 전원에게 같은 일이 벌어집니다.**

고칠 게 있으면 수정하지 말고 **새 번호로 파일을 하나 더** 만드세요.

```sql
-- V3__fix_guardian_phone_length.sql
ALTER TABLE users MODIFY guardian_phone VARCHAR(20);
```

아직 아무에게도 공유하지 않았고 자기 DB에만 적용한 상태라면,
`down -v` 로 초기화한 뒤 수정해도 됩니다.

## 작성할 때

- 한 파일에 한 가지 목적. 테이블 추가와 데이터 삽입을 섞지 않습니다
- 문자셋은 컨테이너에서 `utf8mb4` 로 설정되어 있으므로 테이블마다 지정할 필요는 없습니다
- 시연용 데이터의 값(잔액, 계좌번호, 송금 이력)은 기능 명세의 시드 데이터 표를 기준으로 합니다.
  값이 어긋나면 이상거래 감지 시나리오가 재현되지 않습니다
