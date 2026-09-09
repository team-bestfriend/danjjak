import { onScopeDispose, shallowRef } from 'vue';

export function useDiscardConfirmation() {
  const discardDialog = shallowRef(null);
  let pending = null;
  let resolvePending;

  function confirmDiscard(options = {}) {
    // 연속된 뒤로 가기에도 같은 확인 결과를 기다린다.
    if (pending) return pending;
    discardDialog.value = options;
    pending = new Promise((resolve) => { resolvePending = resolve; });
    return pending;
  }

  function resolveDiscard(confirmed) {
    const resolve = resolvePending;
    discardDialog.value = null;
    pending = null;
    resolvePending = null;
    resolve?.(confirmed);
  }

  onScopeDispose(() => resolveDiscard(false));
  return { discardDialog, confirmDiscard, resolveDiscard };
}
