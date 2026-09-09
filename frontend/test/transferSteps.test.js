import assert from 'node:assert/strict';
import test from 'node:test';
import { TRANSFER_STEPS, TRANSFER_STEP_TOTAL, transferStepBar } from '../src/features/transferSteps.js';

const SHORTCUT_TRANSFER_FLOW = [
  'transfer-source',
  'guide-person',
  'guide-account',
  'amount-input',
  'final-confirm',
  'pin-entry',
];

const DIRECT_FLOW = [
  'transfer-source',
  'direct-transfer',
  'direct-newaccount',
  'amount-input',
  'final-confirm',
  'pin-entry',
];

test('단축번호 송금은 단계 번호를 건너뛰지 않고 1에서 6까지 올라간다', () => {
  const orders = SHORTCUT_TRANSFER_FLOW.map((screen) => transferStepBar(screen).current);
  assert.deepEqual(orders, [1, 2, 3, 4, 5, 6]);
  for (let index = 1; index < orders.length; index += 1) {
    const gap = orders[index] - orders[index - 1];
    assert.equal(gap, 1, `단계가 ${orders[index - 1]}에서 ${orders[index]}로 이어지지 않았다`);
  }
  assert.equal(orders.at(-1), TRANSFER_STEP_TOTAL);
});

test('직접 입력 송금도 같은 6단계 뼈대를 연속으로 사용한다', () => {
  const orders = DIRECT_FLOW.map((screen) => transferStepBar(screen).current);
  assert.deepEqual(orders, [1, 2, 3, 4, 5, 6]);
});

test('같은 번호를 쓰는 화면은 같은 단계 이름을 보여 준다', () => {
  const labelsByOrder = new Map();
  for (const step of Object.values(TRANSFER_STEPS)) {
    const known = labelsByOrder.get(step.order);
    if (known) assert.equal(step.label, known);
    else labelsByOrder.set(step.order, step.label);
  }
  assert.equal(labelsByOrder.size, TRANSFER_STEP_TOTAL);
});

test('모든 단계에 이름이 있고 총 단계 수를 넘지 않는다', () => {
  for (const [screen, step] of Object.entries(TRANSFER_STEPS)) {
    assert.ok(step.label.length > 0, `${screen} 단계 이름이 비어 있다`);
    assert.ok(step.order >= 1 && step.order <= TRANSFER_STEP_TOTAL, `${screen} 단계 번호가 범위를 벗어났다`);
  }
});

test('알 수 없는 화면은 진행 표시를 깨뜨리지 않는다', () => {
  assert.deepEqual(transferStepBar('unknown-screen'), { current: 1, total: TRANSFER_STEP_TOTAL, label: '' });
});
