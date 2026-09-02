# 데이터베이스 스키마

`db/migration/`은 단짝친구의 DB 스키마 계약입니다.
`contracts/openapi.yaml`이 HTTP API의 기준이듯, 이 폴더가 테이블 구조의 기준입니다.

실행 환경은 `infra/`에서 관리합니다. 이 문서는 마이그레이션 작성 규칙만 설명합니다.

## 적용 방법

`infra` 디렉터리에서 컨테이너를 실행하면 Flyway가 마이그레이션을 적용합니다.

```powershell
cd C:\danjjak\infra
docker compose up -d
docker compose logs flyway --tail 20
```

- 적용할 파일이 있으면 `Successfully applied N migration(s)`가 표시됩니다.
- 이미 모두 적용됐다면 `Schema is up to date`가 표시됩니다.
- 자세한 실행 방법은 [`infra/README.md`](../infra/README.md)를 확인합니다.

## 파일 이름

```text
V{번호}__{설명}.sql
```

- 버전과 설명 사이에는 언더스코어 두 개를 사용합니다.
- 번호는 정수로 작성합니다.
- 설명은 영문 소문자와 언더스코어를 사용합니다.

```text
V1__initial_schema.sql
V2__seed_demo_data.sql
V3__add_guardian_phone.sql
```

## 적용된 마이그레이션은 수정하지 않습니다

Flyway는 적용한 파일의 체크섬을 기록합니다. 이미 공유하거나 적용한 마이그레이션을 수정하면 체크섬 오류가 발생합니다.

- 실수로 수정했다면 원래 내용으로 되돌립니다.
- 변경이 필요하면 다음 번호의 새 마이그레이션을 추가합니다.
- 기존 마이그레이션 파일은 삭제하거나 이름을 바꾸지 않습니다.

## 작성 규칙

- 한 파일에는 한 가지 목적만 담습니다.
- 스키마 변경과 시연용 시드 데이터는 서로 다른 파일에 작성합니다.
- 문자셋은 컨테이너에서 `utf8mb4`로 설정하므로 테이블마다 지정하지 않습니다.
- 시연용 잔액, 계좌번호와 송금 이력은 확정된 기능 명세와 일치해야 합니다.
