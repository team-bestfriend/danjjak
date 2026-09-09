import assert from 'node:assert/strict';
import test from 'node:test';
import { formatKoreanWon, formatWon, formatWonWithKorean } from '../src/utils/money.js';

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

test('0원과 잘못된 값에 잘못된 한글 금액을 붙이지 않는다', () => {
  assert.equal(formatWonWithKorean(0), '0원');
  for (const value of ['', null, undefined, -1, 1.5, 'abc']) {
    assert.equal(formatWon(value), '-');
    assert.equal(formatKoreanWon(value), '');
    assert.equal(formatWonWithKorean(value), '-');
  }
});
