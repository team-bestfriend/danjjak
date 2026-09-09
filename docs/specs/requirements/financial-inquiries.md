# 금융 조회

[목차](../requirements.md)

## 요구사항

| ID | 기능 | 완료 조건 |
| --- | --- | --- |
| FR-032 | 모의 금융 조회 | 내 계좌의 잔액·거래·연금·관리비·공과금 조회 |
| FR-033 | 거래 정보 | 입출금·금액·상대방·시각·거래 후 잔액 표시 |
| FR-034 | 고객센터 | 제공된 번호 표시, 사용자 선택 시 전화 앱 연결 |

**단축번호 → 실행 전 확인 → 시작 → 결과 확인 1단계**

| 업무 | 표시·조작 |
| --- | --- |
| 잔액 | 최초 숨김, ‘잔액 보기/숨기기’ |
| 거래내역 | 전체/입금/출금 필터, 최신순 |
| 연금 | 연금 분류 입금 |
| 관리비·공과금 | 해당 분류 거래 |
| 내 계좌 변경 | 같은 결과 화면에서 재조회 |
| 조회 기간 | 선택 계좌의 전체 저장 기간. ‘이번 달’ 고정 문구 금지 |
| 금액 | 쉼표·원 단위, 상세 핵심 금액은 한글 병기 |
| 고객센터 | 번호·‘전화 연결하기’, 컴퓨터에서도 번호 표시 |

## 연금·관리비·공과금 결과 안내

- 시작 전에는 기존 업무 설명을 제공하고, 시작 후에는 선택 계좌의 최신 거래 한 건을 날짜·상대방·금액으로 안내한다. 연금은 연금 분류 입금 중 최신 한 건을 사용한다.
- 업무별 고정 문장 틀에 조회 데이터를 채운다. 관리비·공과금은 실제 입출금 방향을 반영하고 전체 납부 완료를 추정하지 않는다.
- 화면 안내와 TTS는 같은 문구를 사용한다. 이 세 업무의 결과는 저장된 단계 문구·가족 음성 대신 자동 생성 문구·TTS를 사용하며, 기존 저장값과 편집 화면은 유지한다.
- 조회가 끝난 뒤 안내하고 계좌 변경 시 이전 안내를 중단한다. 빈 결과·계좌 없음·조회 실패를 구분하며, 날짜·금액·기관명을 시연 값으로 고정하지 않는다.

## 결과 상태

| 상태 | 안내·완료 |
| --- | --- |
| 정상 자료 | 결과 표시 후 조회 완료 |
| 정상 빈 결과 | 계좌·분류를 설명하고 조회 완료 가능 |
| 계좌 없음 | 불러오기 안내, 완료 아님 |
| 조회 실패 | 재시도, 완료 아님 |
| 고객센터 | 전화 버튼 선택 시 패턴 완료. 실제 연결·상담 성공 의미 아님 |

## Agent Notes

- Query only owned accounts; switching accounts refetches that account's data without adding a template step.
- Derive results from stored mock data, never fixed preview values. Refetch after transfer to show the new debit and balance.
- Do not infer assistance or ability from balance visibility.
- Use the returned support number; no invented fallback.
- Amount formatting: [FR-059](mock-transfer.md). Acceptance: [SC-004, 010](validation-scenarios.md).
