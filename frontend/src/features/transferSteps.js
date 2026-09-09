/* 직접 송금은 6단계, 저장된 수취 계좌를 사용하는 단축번호 송금은 4단계로 표시한다. */
export const TRANSFER_STEP_TOTAL = 6;
export const SHORTCUT_TRANSFER_STEP_TOTAL = 4;

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

const SHORTCUT_TRANSFER_STEPS = {
  'transfer-source': { order: 1, label: '보낼 계좌' },
  'amount-input': { order: 2, label: '보낼 금액' },
  'final-confirm': { order: 3, label: '최종 확인' },
  'pin-entry': { order: 4, label: '본인 확인' },
};

export function transferStepBar(screenCode, usesSavedPatternRecipient = false) {
  if (usesSavedPatternRecipient) {
    const shortcutStep = SHORTCUT_TRANSFER_STEPS[screenCode];
    if (shortcutStep) {
      return { current: shortcutStep.order, total: SHORTCUT_TRANSFER_STEP_TOTAL, label: shortcutStep.label };
    }
  }
  const step = TRANSFER_STEPS[screenCode];
  if (!step) return { current: 1, total: TRANSFER_STEP_TOTAL, label: '' };
  return { current: step.order, total: TRANSFER_STEP_TOTAL, label: step.label };
}
