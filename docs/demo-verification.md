# 반복 실행 및 최종 검증

2026-09-07 기준. 기존 카카오 시연 계정, Marin, 이모지, 고정 조회 템플릿과 STT 명령,
두 FDS 규칙을 유지한다. 분석 리포트 개선(#63)은 팀원 담당이며 발표(#68)는 이번 작업에서 제외한다.

## 새 시드에서 자동 검증

PowerShell 7, Docker Desktop, JDK 17, Node와 설치된 frontend 의존성이 필요하다. 저장소 루트에서 실행한다.

```powershell
.\scripts\verify-demo.ps1
```

스크립트는 임의 이름의 새 MySQL 8.4 컨테이너를 127.0.0.1:23307에 만들고 V1부터 최신
마이그레이션을 적용·검증한다. 초기 데이터 확인 후 모든 DB 통합 테스트, WAR, 프론트 테스트와
빌드를 실행한다. 종료 시 자신이 만든 컨테이너와 익명 볼륨만 제거하고 환경변수를 복원한다.
기존 `danjjak-mysql-1`, `infra/.env`, 카카오 연결, 녹음 파일은 변경하지 않는다.
포트가 사용 중이면 `-Port 23308`로 실행한다. 실패한 단계는 오류로 끝나며 다음 검증을 성공으로 표시하지 않는다.

API 계약 검증은 루트에서 별도 실행한다.

```powershell
npx.cmd --yes @redocly/cli@2.47.0 lint contracts/openapi.yaml
```

## 기준 시드와 시나리오 시작 상태

| 대상 | 새 DB 상태 / 준비 방법 |
| --- | --- |
| SC-001~002 | 김단짝 1명, 카카오 연결 없음, 동의 선택 미완료. 같은 카카오 계정으로 최초 로그인 → 동의 선택 → 로그아웃·재로그인. 기존 시연 DB의 연결을 지울 필요는 없다. 최초 상태는 격리 DB 테스트로 검증한다. |
| 본인 계좌 | 생활비 통장 50,000,000원, 저축 통장 30,000,000원. Mock PIN은 `1234`. |
| 수취인 | 아들 김민수·딸 김지영, 등록 수취 계좌 각 1개. 수취 계좌는 본인 잔액 계좌와 구분한다. |
| SC-003 | 활성 단축번호 8개. 빈 번호에 생성하고 기존 번호와 교환한 뒤 비활성화한다. |
| 조회 | 생활비 통장에 공과금·관리비·연금 각 1건. 생성 시점 기준 30·20·10일 전. 템플릿은 선택 계좌의 전체 기간 중 해당 카테고리를 보여준다. |
| SC-004 | 기록 동의 후 패턴을 시작한다. 같은 단계에서 다시 듣기·잘못된 터치·뒤로가기를 수행하고 종료한다. 편집과 미리듣기는 실행을 생성하지 않는다. |
| SC-005~007 | 최근 송금 없는 상태에서 소액 송금. 직접 계좌는 8~20자리 숫자와 중간 하이픈 허용. 틀린 PIN·잔액 초과 후 잔액/거래 변화 없음 확인. 실존 계좌 확인은 하지 않는다. |
| SC-010 | 보호자 `010-0000-1004`와 서버가 반환하는 고객센터 번호. 데스크톱에서는 번호·tel 링크를 확인하고 실제 발신은 휴대기기에서 한다. |
| SC-011 | 시작·단계 녹음 없음, 대상별 음성 설정 없음, 전체 기본 TTS/느리게/큰 글씨. 실제 녹음은 아래 수동 체크리스트로 추가한다. |
| SC-012 | 실행·단계 기록 0개. 동의 전 CONSENT_REQUIRED, 거부하면 CONSENT_DECLINED, 동의 후 기록 없으면 NO_DATA. 기록 동의 후 실행·단계 행동을 쌓아 분석하고 명시적으로 제안을 적용한다. |

분석용 기록을 시드에 꾸며 넣지 않는다. `InstructionSuggestionIntegrationTest`가 실제 서비스로
실행·방문·행동·종료 기록을 만들고 문구 적용 및 다음 실행 반영을 검증한다. 각 DB 테스트는 롤백한다.

### SC-008~009 FDS 재현

최근 횟수는 **이번 송금 이전 10분 안에 완료된 TRANSFER_OUT**이다. 검토 중이거나 취소한 송금은 세지 않는다.
고액 기준은 **1,000만원 이상**이다. 새 시드에는 최근 송금이 없다.

| 결과 | 이번 금액 | 이전 10분 완료 횟수 | 화면/판정 |
| --- | --- | --- | --- |
| NORMAL | 1,000원 | 0 또는 1 | 정상 완료 |
| MEDIUM | 10,000,000원 | 0 또는 1 | 고액 사유 1개, 계속/취소 |
| MEDIUM | 1,000원 | 2 이상 | 반복 사유 1개, 계속/취소 |
| HIGH | 10,000,000원 | 2 이상 | 두 사유, 알림과 계속/취소를 별도로 선택 |

HIGH는 1,000원 송금을 두 번 완료한 뒤 10분 안에 1,000만원을 요청하면 재현된다.
다시 NORMAL을 확인할 때는 마지막 완료 후 10분이 지나도록 기다리거나 새 격리 시드에서 실행한다.
자동 검증은 `TransferDatabaseIntegrationTest`, `TransferFdsApiIntegrationTest`가 실제 DB/HTTP 경로로 재현한다.

## 시연 앱 실행

기존 시연 데이터는 초기화하지 않는다. 저장소 루트에서 다음으로 새 마이그레이션만 적용한다.

```powershell
docker compose --env-file infra/.env -f infra/compose.yaml up -d mysql
docker compose --env-file infra/.env -f infra/compose.yaml run --rm flyway migrate
```

Flyway 성공을 확인한 뒤 새 `backend/build/libs/danjjak.war`를 Tomcat 9에 재배포하고 재시작한다.
기존 IntelliJ Tomcat Run Configuration의 OpenAI·카카오 환경변수를 유지한다.
현재 시연 MySQL 호스트 포트는 13306이므로 `DANJJAK_DB_URL`도 13306을 가리켜야 한다.
`http://localhost:8080/danjjak/api/health` 확인 후 `frontend`에서 `npm.cmd run dev`를 실행하고
`http://localhost:5173`을 연다. 기존 카카오 계정으로 로그인한다.

이번 작업에서 기존 시연 DB의 V6·V7 적용까지 완료했다. 적용 전후 카카오 연결 1개,
패턴 8개, 실행 기록 13개가 유지됐다. Tomcat health는 응답하지 않았으므로 현재 Run
Configuration의 환경변수를 사용한 재배포·시작과 실제 외부 연동 확인은 남아 있다.

가족 파일은 Tomcat 사용자 홈의 `.danjjak/voices` 또는 `DANJJAK_VOICE_DIR`에 저장한다.
해당 디렉터리는 쓰기 가능하고 재배포 뒤에도 보존되어야 한다. 다른 기기의 마이크 접근은 HTTPS가 필요하다.

## 검증 결과와 남은 사람 확인

| 시나리오 | 이번 자동/브라우저 검증 근거 | 남은 확인 |
| --- | --- | --- |
| SC-001~002 | UserDatabaseIntegrationTest, OAuth 서비스·컨트롤러 테스트: 최초 연결/재로그인/동의·설정 보존 | 현재 Tomcat에서 기존 계정 실제 OAuth 왕복 |
| SC-003 | PatternDatabaseIntegrationTest, appStore/voiceWizard 테스트, 실제 홈 스와이프 브라우저 확인 | 실기기 길게 누르기와 터치감 |
| SC-004 | ExecutionLoggingServiceTest, stepGuidance 테스트, 문구 개선 DB 통합 | 실제 음성과 자막 타이밍 |
| SC-005~009 | TransferDatabaseIntegrationTest, TransferFdsApiIntegrationTest: 정상·직접·오류 원자성·MEDIUM·HIGH·알림 분기 | 현재 세션 카카오 실제 전송이 필요할 때만 별도 확인 |
| SC-010 | SupportDatabaseIntegrationTest와 HTTP 테스트: 번호 저장·조회 | 휴대기기 전화 앱 연결 |
| SC-011 | GuidanceDatabaseIntegrationTest, GuidanceHttpIntegrationTest, familyVoice/voiceWizard/ttsLifecycle 테스트; 실제 편집 UI 브라우저 확인 | 실제 마이크 녹음·재생과 Marin 음질 |
| SC-012 | InstructionSuggestionIntegrationTest, usageAnalysis/instructionImprovement 테스트; 비교→적용→같은 단계 편집 브라우저 확인 | 팀원 #63 리포트 최종본과 연결 확인 |

백엔드 166개(실패·오류·skip 0), 프론트 70개 테스트와 두 빌드 성공. OpenAPI lint는 성공하며
기존 OAuth 302 응답에 대한 2XX 경고 2개만 남는다. 별도 브라우저 확인은 실제 컴포넌트와
테스트 API 응답을 사용했다. 이를 실제 외부 서비스까지 연결한 전체 브라우저 E2E 성공으로 간주하지 않는다.

- [가족 음성 수동 검증](family-voice-validation.md): 실제 녹음, 교체, 새로고침, 실패 복구, 음질
- [접근성 검증](accessibility-validation.md): 390×844·320×568, 큰 글씨, 48px 이상 버튼, 안내 접기, 스와이프

OpenAI TTS는 외부 API이며 실패 시 오류/재시도를 표시한다. 가족 재생 실패는 같은 문구 TTS로
전환하고 금융 버튼은 계속 사용할 수 있다. STT는 브라우저 지원 여부와 정해진 문장을 안내한다.
카카오 알림의 SENT는 실제 성공, MOCKED_NO_TOKEN은 토큰 없음,
MOCKED_AFTER_ACTUAL_FAILURE는 실제 실패 후 Mock 처리이며 송금 승인을 대신하지 않는다.
금융 계좌·거래·FDS는 전부 Mock이다.
