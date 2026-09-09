/*
 * 송금 화면의 단계 번호와 이름을 한 곳에서 정한다.
 * 단축번호 송금의 6단계를 기준으로 직접 입력 화면도 대응 단계에 표시한다.
 * 화면을 건너뛰거나 같은 번호가 서로 다른 뜻으로 쓰이지 않는다.
 * 명세: 보낼 계좌 → 받는 사람 → 받는 계좌 → 보낼 금액 → 최종 확인 → 본인 확인
 */
export const TRANSFER_STEP_TOTAL = 6;

export const TRANSFER_STEPS = {
  'transfer-source': { order: 1, label: '보낼 계좌' },
  'direct-transfer': { order: 2, label: '받는 사람' },
  'guide-person': { order: 2, label: '받는 사람' },
  'guide-account': { order: 3, label: '받는 계좌' },
  'direct-newaccount': { order: 3, label: '받는 계좌' },
  'amount-input': { order: 4, label: '보낼 금액' },
  'final-confirm': { order: 5, label: '최종 확인' },
  'pin-entry': { order: 6, label: '본인 확인' },
};

export function transferStepBar(screenCode) {
  const step = TRANSFER_STEPS[screenCode];
  if (!step) return { current: 1, total: TRANSFER_STEP_TOTAL, label: '' };
  return { current: step.order, total: TRANSFER_STEP_TOTAL, label: step.label };
}
