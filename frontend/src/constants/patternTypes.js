const TYPE_LABELS = {
  TRANSFER: '등록한 사람에게 송금',
  PENSION_CHECK: '연금 입금 확인',
  MANAGEMENT_FEE_CHECK: '관리비 확인',
  BALANCE_CHECK: '잔액 확인',
  TRANSACTION_HISTORY: '거래내역 조회',
  CUSTOMER_CENTER: '고객센터 연결',
  UTILITY_BILL_CHECK: '공과금 확인',
  AUTO_TRANSFER_CHECK: '자동이체 확인',
  CARD_HISTORY: '카드 이용내역',
  DEPOSIT_MATURITY_CHECK: '예금 만기 확인',
};

export function typeLabel(type) {
  return Object.hasOwn(TYPE_LABELS, type) ? TYPE_LABELS[type] : '확인할 수 없는 서비스';
}
