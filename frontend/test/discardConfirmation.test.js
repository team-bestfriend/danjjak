import assert from 'node:assert/strict';
import test from 'node:test';
import { effectScope } from 'vue';
import { useDiscardConfirmation } from '../src/composables/useDiscardConfirmation.js';

test('반복된 이탈 요청은 같은 결정을 기다리고 다음 요청은 새로 확인한다', async () => {
  const scope = effectScope();
  const confirmation = scope.run(useDiscardConfirmation);
  const first = confirmation.confirmDiscard();
  assert.equal(confirmation.confirmDiscard(), first);
  confirmation.resolveDiscard(false);
  assert.equal(await first, false);
  assert.equal(confirmation.discardDialog.value, null);
  const second = confirmation.confirmDiscard({ confirmLabel: '업무 변경' });
  confirmation.resolveDiscard(true);
  assert.equal(await second, true);
  scope.stop();
});

test('화면이 제거되면 대기 중인 이탈 요청을 취소해 나중에 실행되지 않게 한다', async () => {
  const scope = effectScope();
  const confirmation = scope.run(useDiscardConfirmation);
  const pending = confirmation.confirmDiscard();
  scope.stop();
  assert.equal(await pending, false);
  assert.equal(confirmation.discardDialog.value, null);
});
