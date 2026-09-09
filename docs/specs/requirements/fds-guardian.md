# 이상거래·보호자 대응

[목차](../requirements.md)

## 요구사항

| ID | 기능 | 완료 조건 |
| --- | --- | --- |
| FR-035 | 공통 FDS | 패턴·직접 송금에 같은 서버 판정 |
| FR-036 | 고액 | 10,000,000원 이상 |
| FR-037 | 반복 | 이전 10분 내 완료 출금 송금 2건 이상 |
| FR-038 | 위험 단계 | 사유 0개 정상 / 1개 주의 / 2개 높은 주의 |
| FR-039 | 경고 | 위험 정도·전체 사유·받는 사람·계좌·금액 표시 |
| FR-040 | 사용자 결정 | 다시 확인·계속 보내기·보내지 않기 |
| FR-041 | 판정 기록 | 이상 시도당 하나의 기록 유지 |
| FR-042 | 가족 알림 | 높은 주의 + 동의 + 명시적 선택. 내부 실제/모의 결과 구별, 사용자에게는 전송 여부를 쉬운 문구로 안내 |
| FR-043 | 보호자 전화 | 저장 번호 확인 후 전화 앱 연결 |

## 판정·화면

| 항목 | 기준 |
| --- | --- |
| 판정 주체 | 서버 |
| 시간 범위 | 판정 시각의 10분 전부터 현재까지 양 끝 포함, 현재 시도 제외 |
| 합산 | 같은 사용자의 모든 내 계좌, 직접·패턴 완료 송금 |
| 제외 | 실패·취소·결정 대기, 이용 기록 동의 여부는 무관 |
| FDS 아님 | 새 계좌·경로 이탈·잘못 누름·소요 시간·분석 점수 |
| 정상 | 경고 기록 없이 송금 처리 |
| 주의·높은 주의 | 결정 전 차감 없음, 사유가 둘이어도 판정 기록 하나 |

| 화면 항목 | 표시 |
| --- | --- |
| 제목 | ‘돈을 보내기 전에 한 번 더 확인해 주세요.’, ‘아직 돈은 보내지 않았어요.’ |
| 정도 | ‘주의’ / ‘높은 주의’, 색상 외 글자 구분 |
| 단짝이 말풍선 | ‘누군가 돈을 보내라고 했나요?’ → 전화·문자로 송금 요구나 재촉을 받았다면 사기일 수 있음을 안내 → 잠깐 멈추고 가족과 확인 |
| 고액 이유 | ‘1,000만원 이상 보내려고 해요.’ |
| 반복 이유 | ‘최근 10분 동안 2번 보냈어요.’ |
| 대상·금액 | 받는 사람·은행·계좌, 숫자·한글 금액 |
| 행동 | 다시 확인 / 계속 보내기 / 보내지 않기 |
| 연락 | 가족에게 전화하기, 높은 주의일 때 가족에게 알림 보내기 |

- 화면 순서: 주의 박스 → 단짝이 말풍선 → 이번 송금에서 확인할 점·받는 분과 보낼 금액 → 가족 도움 요청·알림 결과 → 송금 선택.
- 고액·반복 사유는 ‘이번 송금에서 확인할 점’에 보조 정보로 표시. 말풍선·음성은 구체적인 사기 의심 상황과 다음 행동을 안내하되, 현재 송금을 사기로 단정하거나 새 판정 조건을 추가하지 않음.
- 화면·음성 안내에 ‘서버’, ‘토큰’, ‘Mock’, ‘모의 시연’, ‘본인 수신’ 등 기술·시연 설명을 표시하지 않음.

‘처음 보내는 계좌예요.’는 팀 문구 예시이며 이번 FDS 조건에 추가하지 않음.

## 결정

| 행동 | 결과 |
| --- | --- |
| 다시 확인 | 같은 시도의 정보 읽기 전용 확인, 재확인 여부 기록 |
| 계좌·금액 변경 | 현재 시도 취소 후 새 송금 |
| 계속 보내기 | 잔액 재확인 → 차감·거래 확정 → 완료 |
| 보내지 않기 | 취소 확정, 차감·거래 없음 |
| 중복 결정 | 저장된 결과 표시, 금융 처리 반복 없음 |
| 요청 실패 | 판정 식별 정보·안전한 입력 유지, 재시도 |

## 카톡 시연

| 항목 | 확정 내용 |
| --- | --- |
| 버튼 | **가족에게 알림 보내기** |
| 실제 수신자 | **로그인한 본인 계정: 나에게 보내기** |
| 화면 설명 | 경고 화면과 이용방법에서 시연·본인 수신 설명 제외. 가족 알림과 정보 공유 동의 안내 |
| 조건 | 본인 소유·미결정·높은 주의 + 보호자 공유 동의 + 버튼 선택 |
| 동의 없음 | 설정 안내, 실제·모의 전송 없음 |
| 전화번호 | 카카오 수신자 식별자로 사용하지 않음 |

| 결과 | 사용자 안내 | 실제 전송 시각 |
| --- | --- | --- |
| 전송 성공 | ‘카카오톡 알림을 보냈어요.’ | 저장 |
| 자격 정보 없음 | ‘카카오톡 알림을 보내지 못했어요.’ + 가족 전화 안내 | 없음 |
| 실패 후 모의 전환 | ‘카카오톡 알림을 보내지 못했어요.’ + 가족 전화 안내 | 없음 |

내부 전송 방식과 결과 코드는 유지한다. 가족 수신·확인이 확인되지 않았으므로 ‘가족에게 보냈어요’ 또는 ‘가족이 확인했어요’로 표시하지 않는다.

## 전화

- 저장된 보호자 번호 표시 → 사용자 확인·선택 → 전화 앱.
- 미등록: 연락처 등록 안내. 통화 미지원: 번호 표시.
- 전화·카톡 결과는 보호자 승인이나 실제 확인의 증거가 아님.

## Agent Notes

- Render returned risk and reasons; never infer extra rules client-side. Family guidance must not hide system reasons.
- Reuse the same anomaly identity across recheck, notification, and decision. Do not create another record on resubmission/re-entry for the same active attempt.
- Notification requires server-side ownership, unresolved HIGH risk, consent, and explicit choice. A phone contact is insufficient.
- Include demo context, reasons, recipient, amount, and masked account in the message; exclude PIN/full account numbers.
- Reuse an existing actual-send result without resending while unresolved. Reject sends after resolution or below HIGH. Mock outcomes have no actual-send timestamp or durable delivery history.
- Audio/notification failure must not block continue, cancel, or calling. Lock mutation controls while pending.
- Calls do not confirm connection or change the transfer decision automatically.
- References: [UX](shared-ux.md), [provider review](design-decisions.md), [SC-008–010, 015](validation-scenarios.md).
