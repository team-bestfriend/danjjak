export const chatActions = Object.freeze({
  TRANSFER: { label: '송금 시작하기', route: 'transfer-source' },
  BALANCE_CHECK: { label: '잔액 확인하기', route: 'task-4' },
  PENSION_CHECK: { label: '연금 입금 확인하기', route: 'task-2' },
  MANAGEMENT_FEE_CHECK: { label: '관리비 확인하기', route: 'task-3' },
  UTILITY_BILL_CHECK: { label: '공과금 확인하기', route: 'task-8' },
  CUSTOMER_CENTER: { label: '고객센터 보기', route: 'task-6' },
  PATTERN: { label: '이 업무 시작하기', route: null },
  APP_HELP: { label: '단짝 사용 방법 보기', route: 'service-guide' },
});

export function actionFor(action) {
  return Object.hasOwn(chatActions, action) ? chatActions[action] : null;
}

export function showChatFab(routeName) {
  return ['home', 'patterns', 'analysis', 'settings'].includes(routeName);
}

export function containsSensitiveInfo(message) {
  return /비밀번호|비번|계좌\s*번호|주민\s*등록|인증\s*번호|password|pin|otp|(?:[0-9][ -]?){4,}/i.test(message);
}

export const sensitiveNotice = '비밀번호나 계좌번호는 채팅에 쓰지 마세요. 필요한 정보는 해당 금융 화면에서 입력해 주세요.';

export async function openChatAction(reply, store) {
  const action = actionFor(reply.action);
  if (!action) return;
  if (reply.action === 'PATTERN' && reply.patternId != null) {
    // 현재 사용자의 활성 패턴인지 다시 확인한 뒤 저장된 단계로 실행한다.
    const detail = await store.loadPatternDetail(reply.patternId);
    await store.startPatternExecution(detail);
    return;
  }
  if (reply.action === 'TRANSFER') {
    store.startTransfer();
  } else if (reply.action !== 'APP_HELP' && reply.patternId != null) {
    // 저장 후 변경·삭제된 패턴과 다른 업무로 연결되는 응답을 다시 확인한다.
    const detail = await store.loadPatternDetail(reply.patternId);
    if (detail.patternType !== reply.action) throw new Error('Pattern changed');
    await store.startPatternExecution(detail);
    return;
  } else if (reply.action !== 'APP_HELP') {
    store.resetPatternExecution();
  }
  await store.navigate(action.route);
}
