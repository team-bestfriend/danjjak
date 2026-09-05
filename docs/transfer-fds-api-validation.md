# 송금·FDS API 검증 가이드

이 문서는 송금·FDS·보호자 대응과 패턴 실행 로그의 대표 흐름을 검증하는 순서입니다.
모든 계좌, 잔액, PIN, 거래는 시연용 Mock 데이터이며 실제 금융 거래를 만들지 않습니다.

## 1. 깨끗한 MySQL과 Flyway 준비

`infra/.env`를 만든 뒤 `infra` 디렉터리에서 실행합니다. 기존 로컬 DB를 지워도 되는
경우에만 `down -v`를 사용합니다.

```powershell
docker compose down -v
docker compose up -d
docker compose ps -a
docker compose logs flyway --tail 30
```

완료 상태는 MySQL `healthy`, Flyway `Exited (0)`입니다. Flyway 이력에는 V1부터 V5까지
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

Frontend 송금 상태와 중복 제출 복구는 다음 명령으로 검증합니다.

```powershell
cd frontend
npm test
npm run build
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

## 3. 인증과 대표 API 순서

아래 예시의 기준 URL은 `http://localhost:8080/danjjak`입니다. 보호 API는 카카오 OAuth로
생성된 같은 서버의 `DANJJAKSESSION`이 필요하며, 세션이 없으면 `401 SESSION_REQUIRED`를
반환합니다. 이 앱 전용 쿠키는 Vite 로그인 시작 요청과 Tomcat 직접 콜백이 공유하도록
루트 경로(`/`)를 사용합니다. 브라우저 시연은 `http://localhost:5173`에서 카카오 로그인을 먼저 완료하고,
자동 통합 테스트는 동일한 세션 조건을 `MockHttpSession`으로 재현합니다. 응답 스키마와
오류 형식의 기준은 [`contracts/openapi.yaml`](../contracts/openapi.yaml)입니다.

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
보호자 공유 동의가 없으면 서버도 `403 GUARDIAN_SHARE_CONSENT_REQUIRED`로 차단합니다.

```powershell
curl.exe -X POST "$api/anomaly-events/$anomalyId/guardian-notification"
```

- 세션에 카카오 액세스 토큰이 없으면 `MOCKED_NO_TOKEN`입니다.
- 실제 호출이 실패하면 `MOCKED_AFTER_ACTUAL_FAILURE`이며 송금 흐름은 계속됩니다.
- 실제 호출이 성공하면 `SENT`이고, 이때만 `guardian_notified_at`이 저장됩니다.

실제 발송에는 카카오 로그인에서 `talk_message` 동의를 받은 액세스 토큰을 같은 서버
세션의 `kakaoAccessToken` 속성으로 넘겨야 합니다. 토큰은 애플리케이션 테이블에
저장하지 않습니다. OAuth 시작·콜백·세션 조회·로그아웃은 구현되어 있으며, 카카오 앱이나
토큰이 준비되지 않은 로컬 시연은 의도적으로 Mock 경로를 사용합니다.

## 5. 자동 검증 범위

- `AccountDatabaseIntegrationTest`: 본인 계좌 2개, 등록 인물별 계좌, 잔액과 거래 카테고리
- `TransferDatabaseIntegrationTest`: 등록/직접 송금, PIN·잔액 롤백, MEDIUM/HIGH, 취소/계속
- `SupportDatabaseIntegrationTest`: 보호자 1명, 고객센터, 카카오 성공·실패·Mock 저장 규칙
- `TransferFdsApiIntegrationTest`: 실제 Spring MVC 요청·응답으로 직접 송금·PIN·잔액 오류 복구와 HIGH 전체 흐름
- `PatternDatabaseIntegrationTest`: 기본 단축번호 8개, 실제 송금 6단계, 번호 교환, 실행·재방문·행동 로그와 동의 게이트
- `UserDatabaseIntegrationTest`: 선택 동의 네 조합, 접근성 설정 저장·재조회, 최초 카카오 연결과 동일 사용자 재로그인
- `frontend/test/appStore.test.js`: 등록 인물 추가·수정과 서버 재조회, 본인·수취 계좌 분리,
  기본 계좌 fallback 경고, 카테고리 조회, 직접 송금 요청값, 완료 후 재조회, PIN 오류,
  중복 제출·처리 중 취소 방지, HIGH 응답, 이미 처리된 이상거래 결과, HIGH 전용 알림과
  실제·Mock 알림 응답 검증

DB 통합 테스트는 `DANJJAK_DB_INTEGRATION_TEST=true`일 때만 실행되며 각 테스트는
트랜잭션 롤백되어 다음 테스트의 초기 상태를 오염시키지 않습니다.

## 6. Frontend 연결 확인

Tomcat에 최신 `danjjak.war`를 배포한 뒤 `frontend`에서 `npm run dev`를 실행합니다.
Frontend는 `/api` 요청을 `http://localhost:8080/danjjak/api`로 프록시합니다.

1. 홈의 `직접 송금하기`에서 기본 본인 계좌가 먼저 선택되는지 확인합니다.
2. 등록 수취인과 직접 입력 수취인으로 각각 정상 송금을 완료합니다.
3. 잘못된 PIN과 잔액 부족 후 PIN만 지워지고 안전한 입력은 유지되는지 확인합니다.
4. 송금 완료 화면의 거래 ID와 잔액이 거래내역 재조회 결과와 같은지 확인합니다.
5. 고액 또는 반복 송금에서 서버가 반환한 `MEDIUM`/`HIGH`와 사유만 표시되는지 확인합니다.
6. `HIGH`에서만 카카오 알림 버튼이 보이고 세 결과가 구분되는지 확인합니다.
7. 보호자·고객센터 전화 링크가 `/api/support`의 번호를 사용하는지 확인합니다.

Frontend는 신규 수취인 여부나 화면 이탈로 위험도를 계산하지 않으며, PIN을 Pinia나
브라우저 저장소에 보관하지 않습니다. 실제 카카오 `SENT` 응답일 때만 서버가 반환한
`sentAt`을 발송 시각으로 표시합니다.

## 7. 2026-09-06 로컬 검증 결과

- Frontend 상태 테스트: 34개 통과
- Frontend 프로덕션 빌드: 통과
- MySQL 연동 Backend 테스트: 30개 suite, 90개 테스트 통과
- Backend WAR 빌드: 통과
- OpenAPI Redocly lint: 오류 0개로 통과(리다이렉트 응답 관련 경고 포함)
- 브라우저 시연: 등록·직접 정상 송금, PIN 오류 복구, MEDIUM 취소, HIGH Mock 알림과
  취소, 잔액·거래내역 재조회, 등록 인물 추가·수정, 보호자 번호 저장과 두 `tel:` 링크 확인
- 실제 로그인 세션 시연: 이용 기록·카카오 알림 동의를 모두 저장한 뒤 등록 계좌로
  10,000원 송금 2건을 완료하고, 10,000,000원 시도에서 `HIGH_AMOUNT`와
  `REPEATED_TRANSFER`가 함께 적용된 `HIGH` 경고 및 보호자 전화·카카오 알림 동작 노출 확인
- 실제 카카오 알림: 로그인 세션의 `talk_message` 토큰으로 `SENT` 성공, 화면에 발송 시각
  `2026-09-06 04:37` 표시, 알림 버튼 비활성화로 같은 화면의 중복 요청 방지 확인
- 고액 송금 후속 처리: 알림 발송 뒤 송금을 취소하고 잔액·거래내역이 바뀌지 않았다는 결과 확인
- 카카오 OAuth: 실제 카카오 인증, state 검증, 토큰 교환, 시드 사용자 연결과 선택 동의 저장 후 홈 진입 확인
- 재로그인: 로그아웃 뒤 같은 카카오 계정으로 즉시 재로그인하여 동일한 `김단짝` 사용자와
  두 선택 동의의 `동의함` 상태 유지 확인
- 인증 없는 보호 API: 계좌·패턴·TTS 요청이 `401`로 차단되고 세션 조회만 익명 상태를 반환

실제 카카오 `SENT`의 브라우저 검증은 `HIGH` 경고 화면에서 사용자 확인을 받은 뒤 수행했다.
실패 분기는 서버 테스트로, 토큰 없는 경로는 `MOCKED_NO_TOKEN`으로 함께 검증한다.

## 8. 담당 범위 완료 근거

| 담당 요구사항 | 권위 있는 현재 근거 | 상태 |
| --- | --- | --- |
| 계좌·거래 Mock 데이터와 공통 테이블 | Flyway V1~V2, `AccountDatabaseIntegrationTest` | 자동 검증 완료 |
| 등록 수취인·직접 수취인 송금 | `TransferDatabaseIntegrationTest`, `TransferFdsApiIntegrationTest`의 HTTP→DB→재조회 시나리오 | 자동 검증 완료 |
| 잔액·전체 거래·연금·관리비·공과금 조회 | `AccountDatabaseIntegrationTest`, `TransferFdsApiIntegrationTest`의 카테고리 HTTP 조회 | 자동 검증 완료 |
| NORMAL·고액 단독·반복 단독·두 규칙 HIGH FDS | `FdsEvaluatorTest`, `TransferDatabaseIntegrationTest` | 자동 검증 완료 |
| 한 시도 한 이상거래, 계속·취소·재결정 원자성 | `TransferServiceTest`, `TransferDatabaseIntegrationTest`, `TransferFdsApiIntegrationTest` | 자동 검증 완료 |
| 보호자 연락처 저장과 보호자·고객센터 번호 조회 | `SupportDatabaseIntegrationTest`, `TransferFdsApiIntegrationTest`와 서버 번호 기반 두 `tel:` UI | 자동·브라우저 검증 완료 |
| HIGH 알림 동의 게이트와 Mock fallback | `GuardianNotificationServiceTest`, `SupportDatabaseIntegrationTest`, Frontend 상태 테스트 | 자동 검증 완료 |
| 실제 카카오 나에게 보내기와 중복 발송 방지 | provider stub 통합 테스트와 실제 OAuth 세션의 `HIGH` 화면에서 `SENT`·발송 시각·버튼 비활성화 확인 | 자동·실제 브라우저 검증 완료 |
| 인증·동의·접근성 설정 유지 | `AuthControllerTest`, `KakaoOAuthServiceTest`, `UserDatabaseIntegrationTest`, Frontend 세션 테스트, 실제 카카오 OAuth 콜백·선택 동의 저장·로그아웃·재로그인 | 자동·실제 브라우저 검증 완료 |
| 패턴 실행·단계 방문·행동 로그 연결 | `PatternDatabaseIntegrationTest`, Frontend 실행·재방문·중복 이벤트 테스트 | 자동 검증 완료 |
| OpenAPI·Backend·Frontend 연결 | Redocly lint, Backend test/WAR, Frontend test/build | 자동 검증 완료 |

## 9. 남은 수동 검증 게이트

담당 범위 완료를 막는 수동 검증 게이트는 없습니다. 실제 카카오 앱에서 수신 메시지를
직접 열어보는 확인은 선택 사항이며, 서버 성공 응답·발송 시각·중복 요청 방지는 브라우저에서 확인했습니다.
