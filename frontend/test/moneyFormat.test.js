import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { formatKoreanWon, formatWon, formatWonByUnits, formatWonWithKorean } from '../src/utils/money.js';

const accountImportSource = await readFile(new URL('../src/views/AccountImportView.vue', import.meta.url), 'utf8');
const amountKeypadSource = await readFile(new URL('../src/components/common/AmountKeypad.vue', import.meta.url), 'utf8');
const transferFlowSource = await readFile(new URL('../src/views/TransferFlowView.vue', import.meta.url), 'utf8');

test('경계 금액을 쉼표·원·한글로 일치하게 표시한다', () => {
  const cases = [
    [15000, '15,000원 · 만 오천원'],
    [50000, '50,000원 · 오만원'],
    [100000, '100,000원 · 십만원'],
    [1000000, '1,000,000원 · 백만원'],
    [10000000, '10,000,000원 · 천만원'],
    [100000000, '100,000,000원 · 일억원'],
  ];

  for (const [amount, expected] of cases) {
    assert.equal(formatWonWithKorean(amount), expected);
  }
});

test('복합 자릿수와 문자열 금액도 같은 규칙으로 변환한다', () => {
  assert.equal(formatWon('123456789'), '123,456,789원');
  assert.equal(formatKoreanWon('123456789'), '일억 이천삼백사십오만 육천칠백팔십구원');
});

test('계좌 잔액은 숫자를 만·억 단위로 묶어 표시한다', () => {
  const cases = [
    [50000, '5만원'],
    [1000000, '100만원'],
    [12345678, '1234만 5678원'],
    [123456789, '1억 2345만 6789원'],
    [100010000, '1억 1만원'],
  ];

  for (const [amount, expected] of cases) {
    assert.equal(formatWonByUnits(amount), expected);
  }
});

test('계좌 선택 화면은 숫자 잔액 아래에 작은 단위 잔액을 표시한다', () => {
  for (const source of [accountImportSource, transferFlowSource]) {
    assert.match(source, /잔액 \{\{ formatWon\(account\.balance\) \}\}/);
    assert.match(source, /text-\[(14|16)px\][^>]*>\s*\{\{ formatWonByUnits\(account\.balance\) \}\}/);
    assert.doesNotMatch(source, /formatWonWithKorean\(account\.balance\)/);
  }
});

test('금액 입력 화면은 만원 이상부터 숫자 단위 표기를 우선한다', () => {
  assert.match(amountKeypadSource, /val === '0' \? '금액을 입력해 주세요\.' : formatWonByUnits\(val\)/);
  assert.doesNotMatch(amountKeypadSource, /formatKoreanWon/);
  assert.equal(formatWonByUnits(100000), '10만원');
  assert.equal(formatWonByUnits(12345678), '1234만 5678원');
  assert.equal(formatWonByUnits(5000), '');
});

test('직접 송금 금액 입력은 금액 표시에서 다음 버튼으로 하이라이트가 이동한다', () => {
  assert.match(amountKeypadSource, /highlightComplete && val === '0' \? 'step-guide-target'/);
  assert.match(amountKeypadSource, /highlightComplete && val !== '0' \? 'step-guide-target'/);
});

test('송금 확인·경고·완료 화면은 숫자 금액과 단위 금액을 나누어 표시한다', () => {
  assert.doesNotMatch(transferFlowSource, /formatWonWithKorean/);
  assert.match(transferFlowSource, /value: formatWon\(Number\(store\.transferAmount\)\)/);
  assert.match(transferFlowSource, /unitValue: formatWonByUnits\(Number\(store\.transferAmount\)\)/);
  assert.match(transferFlowSource, /formatWon\(store\.anomaly\?\.amount\)/);
  assert.match(transferFlowSource, /formatWonByUnits\(store\.transferResult\.amount\)/);
});

test('0원과 잘못된 값에 잘못된 한글 금액을 붙이지 않는다', () => {
  assert.equal(formatWonWithKorean(0), '0원');
  assert.equal(formatWonByUnits(9999), '');
  for (const value of ['', null, undefined, -1, 1.5, 'abc']) {
    assert.equal(formatWon(value), '-');
    assert.equal(formatKoreanWon(value), '');
    assert.equal(formatWonWithKorean(value), '-');
    assert.equal(formatWonByUnits(value), '');
  }
});
