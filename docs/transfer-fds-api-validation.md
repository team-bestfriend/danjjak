# 송금·FDS API 검증 가이드

이 문서는 이슈 #9의 대표 흐름을 UI 없이 재현하는 순서입니다. 모든 계좌, 잔액,
PIN, 거래는 시연용 Mock 데이터이며 실제 금융 거래를 만들지 않습니다.

## 1. 깨끗한 MySQL과 Flyway 준비

`infra/.env`를 만든 뒤 `infra` 디렉터리에서 실행합니다. 기존 로컬 DB를 지워도 되는
경우에만 `down -v`를 사용합니다.

```powershell
docker compose down -v
docker compose up -d
docker compose ps -a
docker compose logs flyway --tail 30
```

완료 상태는 MySQL `healthy`, Flyway `Exited (0)`입니다. Flyway 이력에는 V1과 V2가
성공으로 기록되어야 합니다.

기본 포트가 아닌 `13306`을 쓸 때 백엔드 검증 환경은 다음과 같습니다.

```powershell
$env:DANJJAK_DB_INTEGRATION_TEST='true'
$env:DANJJAK_DB_URL='jdbc:mysql://localhost:13306/danjjak?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul'
cd ..\backend
.\gradlew.bat test
.\gradlew.bat check war
```

API 계약은 저장소 루트에서 검증합니다.

```powershell
npx.cmd --yes @redocly/cli@2.47.0 lint contracts/openapi.yaml
```

## 2. 초기 Mock 데이터

Flyway V2 적용 직후 데모 사용자 ID는 `1`, 송금 PIN은 `1234`입니다.

| 구분 | ID/값 | 내용 |
| --- | --- | --- |
| 기본 출금 계좌 | `1` | 신한은행, 잔액 50,000,000원 |
| 보조 본인 계좌 | `2` | 국민은행, 잔액 30,000,000원 |
| 등록 수취 계좌 | `3` | 김민수(아들), 우리은행 |
| 등록 수취 계좌 | `4` | 김지영(딸), 하나은행 |
| 보호자 전화 | `010-0000-1004` | 전화 연결용 |
| 고객센터 전화 | `1588-0000` | 로컬 기본 설정값 |

기본 계좌에는 연금, 관리비, 공과금 거래가 한 건씩 들어 있습니다.

## 3. 대표 API 순서

아래 예시의 기준 URL은 `http://localhost:8080/danjjak`입니다. 응답 스키마와 오류
형식의 기준은 [`contracts/openapi.yaml`](../contracts/openapi.yaml)입니다.

```powershell
$api='http://localhost:8080/danjjak/api'
curl.exe "$api/accounts"
curl.exe "$api/accounts/1/balance"
curl.exe "$api/accounts/1/transactions"
curl.exe "$api/accounts/1/transactions?category=PENSION"
curl.exe "$api/accounts/1/transactions?category=MANAGEMENT_FEE"
curl.exe "$api/accounts/1/transactions?category=UTILITY_BILL"
curl.exe "$api/registered-persons"
curl.exe "$api/support"
```

등록 수취 계좌 정상 송금은 거래와 잔액을 함께 갱신합니다.

```powershell
curl.exe -X POST "$api/transfers" -H "Content-Type: application/json" -d '{"sourceAccountId":1,"registeredRecipientAccountId":3,"amount":1000,"pin":"1234"}'
```

직접 입력 송금은 수취 계좌를 등록하지 않습니다.

```powershell
curl.exe -X POST "$api/transfers" -H "Content-Type: application/json" -d '{"sourceAccountId":1,"directRecipient":{"name":"박친구","bankCode":"003","bankName":"기업은행","accountNumber":"000-000-000003"},"amount":1000,"pin":"1234"}'
```

최근 10분의 완료 송금이 2건 미만인 상태에서 1천만원 이상을 시도하면 `MEDIUM`과
`HIGH_AMOUNT`가 반환됩니다. 최근 완료 송금이 2건 이상이면 1천만원 미만 송금도
`MEDIUM`과 `REPEATED_TRANSFER`가 반환됩니다. 두 조건이 동시에 맞으면 `HIGH`입니다.

```powershell
curl.exe -X POST "$api/transfers" -H "Content-Type: application/json" -d '{"sourceAccountId":1,"registeredRecipientAccountId":3,"amount":10000000,"pin":"1234"}'
```

응답의 `anomalyEventId`를 사용해 사용자가 취소하거나 계속 송금합니다.

```powershell
$anomalyId=1
curl.exe -X POST "$api/anomaly-events/$anomalyId/resolve" -H "Content-Type: application/json" -d '{"action":"CANCEL","rechecked":true}'
curl.exe -X POST "$api/anomaly-events/$anomalyId/resolve" -H "Content-Type: application/json" -d '{"action":"CONTINUE","rechecked":true}'
```

한 이상거래에는 둘 중 하나만 실행할 수 있습니다. 취소하면 거래가 생기지 않고, 계속
송금하면 생성된 거래 ID가 이상거래 기록에 연결됩니다.

PIN 불일치와 잔액 부족은 공통 오류 응답을 반환하며 잔액, 거래, 이상거래를 변경하지
않습니다.

```powershell
curl.exe -X POST "$api/transfers" -H "Content-Type: application/json" -d '{"sourceAccountId":1,"registeredRecipientAccountId":3,"amount":1000,"pin":"0000"}'
curl.exe -X POST "$api/transfers" -H "Content-Type: application/json" -d '{"sourceAccountId":1,"registeredRecipientAccountId":3,"amount":50000001,"pin":"1234"}'
```

## 4. 보호자 연락처와 카카오 알림

보호자 전화번호는 전화 연결용이며 카카오 수신자 식별에 사용하지 않습니다.

```powershell
curl.exe "$api/support"
curl.exe -X PUT "$api/support/guardian" -H "Content-Type: application/json" -d '{"phoneNumber":"010-1111-2222"}'
```

`HIGH` 응답을 받은 뒤 사용자가 알림을 선택한 경우에만 아래 API를 호출합니다.

```powershell
curl.exe -X POST "$api/anomaly-events/$anomalyId/guardian-notification"
```

- 세션에 카카오 액세스 토큰이 없으면 `MOCKED_NO_TOKEN`입니다.
- 실제 호출이 실패하면 `MOCKED_AFTER_ACTUAL_FAILURE`이며 송금 흐름은 계속됩니다.
- 실제 호출이 성공하면 `SENT`이고, 이때만 `guardian_notified_at`이 저장됩니다.

실제 발송에는 카카오 로그인에서 `talk_message` 동의를 받은 액세스 토큰을 같은 서버
세션의 `kakaoAccessToken` 속성으로 넘겨야 합니다. 토큰은 애플리케이션 테이블에
저장하지 않습니다. 현재 이슈 범위에는 카카오 OAuth 로그인 화면과 콜백 구현이 없으므로,
토큰이 없는 로컬 시연은 의도적으로 Mock 경로를 사용합니다.

## 5. 자동 검증 범위

- `AccountDatabaseIntegrationTest`: 본인 계좌 2개, 등록 인물별 계좌, 잔액과 거래 카테고리
- `TransferDatabaseIntegrationTest`: 등록/직접 송금, PIN·잔액 롤백, MEDIUM/HIGH, 취소/계속
- `SupportDatabaseIntegrationTest`: 보호자 1명, 고객센터, 카카오 성공·실패·Mock 저장 규칙
- `TransferFdsApiIntegrationTest`: 실제 Spring MVC 요청·응답으로 HIGH 전체 흐름

DB 통합 테스트는 `DANJJAK_DB_INTEGRATION_TEST=true`일 때만 실행되며 각 테스트는
트랜잭션 롤백되어 다음 테스트의 초기 상태를 오염시키지 않습니다.
