import assert from 'node:assert/strict';
import test from 'node:test';
import { inquiryResultText } from '../src/features/inquiry/resultGuidance.js';

const pension = {
  transactionId: 1,
  category: 'PENSION',
  transactionType: 'DEPOSIT',
  transactionAt: '2026-08-24T09:00:00',
  amount: 1100000,
  counterpartyName: '국민연금공단',
};
const guidance = (category, transactions) => inquiryResultText(category, transactions, 2026);

test('연금은 다른 분류와 출금을 제외하고 최신 입금 한 건만 안내한다', () => {
  const transactions = [
    { ...pension, transactionAt: '2026-07-24T09:00:00', amount: 650000 },
    { ...pension, transactionAt: '2026-08-25T09:00:00', transactionType: 'WITHDRAWAL' },
    { ...pension, transactionAt: '2026-08-26T09:00:00', category: 'UTILITY_BILL' },
    pension,
  ];
  assert.equal(guidance('PENSION', transactions), '가장 최근 연금은 8월 24일에 국민연금공단에서 입금됐어요. 금액은 110만 원이에요.');
  assert.equal(transactions[0].amount, 650000);
});

test('같은 시각의 거래는 거래 번호가 큰 한 건을 안내한다', () => {
  assert.match(guidance('PENSION', [pension, { ...pension, transactionId: 2, amount: 1200000 }]), /120만 원/);
});

test('관리비와 공과금의 상대방과 금액을 실제 출금에 맞게 안내한다', () => {
  assert.equal(guidance('MANAGEMENT_FEE', [{
    ...pension, category: 'MANAGEMENT_FEE', transactionType: 'PAYMENT',
    transactionAt: '2026-08-25T09:00:00', counterpartyName: '단짝아파트 관리사무소', amount: 150000,
  }]), '가장 최근 관리비 거래는 8월 25일이에요. 단짝아파트 관리사무소로 15만 원이 출금됐어요.');
  assert.equal(guidance('UTILITY_BILL', [{
    ...pension, category: 'UTILITY_BILL', transactionType: 'WITHDRAWAL',
    transactionAt: '2026-08-26T09:00:00', counterpartyName: '한국전력공사', amount: 53000,
  }]), '가장 최근 공과금 거래는 8월 26일이에요. 한국전력공사로 5만 3천 원이 출금됐어요.');
});

test('관리비·공과금 입금을 출금 또는 납부로 설명하지 않는다', () => {
  for (const category of ['MANAGEMENT_FEE', 'UTILITY_BILL']) {
    const text = guidance(category, [{ ...pension, category, counterpartyName: '관리사무소', amount: 12500 }]);
    assert.match(text, /관리사무소에서 1만 2천 500원이 입금됐어요/);
    assert.doesNotMatch(text, /출금|납부/);
  }
});

test('과거 연도와 원 단위 금액을 보존하고 상대방이 없으면 생략한다', () => {
  assert.equal(guidance('PENSION', [{ ...pension, transactionAt: '2025-08-24T09:00:00', amount: 1100123, counterpartyName: null }]),
    '가장 최근 연금은 2025년 8월 24일에 입금됐어요. 금액은 110만 123원이에요.');
  assert.match(guidance('PENSION', [{ ...pension, amount: 500 }]), /500원이에요/);
  assert.match(guidance('PENSION', [{ ...pension, amount: 0 }]), /0원이에요/);
});

test('빈 결과와 연금 출금만 있는 결과는 입금이 없다고 안내한다', () => {
  assert.equal(guidance('PENSION', []), '이 계좌에는 연금 입금 내역이 없어요.');
  assert.equal(guidance('PENSION', [{ ...pension, transactionType: 'WITHDRAWAL' }]), '이 계좌에는 연금 입금 내역이 없어요.');
  assert.equal(guidance('MANAGEMENT_FEE', []), '이 계좌에는 관리비 내역이 없어요.');
  assert.equal(guidance('UTILITY_BILL', []), '이 계좌에는 공과금 내역이 없어요.');
});
