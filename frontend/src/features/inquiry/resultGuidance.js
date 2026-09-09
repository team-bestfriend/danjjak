export const RESULT_INQUIRY_CATEGORIES = {
  'task-2': 'PENSION',
  'pension-history': 'PENSION',
  'task-3': 'MANAGEMENT_FEE',
  'task-8': 'UTILITY_BILL',
};

const CATEGORY_LABELS = { PENSION: '연금', MANAGEMENT_FEE: '관리비', UTILITY_BILL: '공과금' };

function formatAmount(amount) {
  let remaining = Number(amount);
  const parts = [];
  for (const [unit, label] of [[100000000, '억'], [10000, '만'], [1000, '천']]) {
    const value = Math.floor(remaining / unit);
    if (value) parts.push(`${value}${label}`);
    remaining %= unit;
  }
  parts.push(remaining || parts.length === 0 ? `${remaining}원` : '원');
  return parts.join(' ');
}

function formatDate(value, currentYear) {
  const date = new Date(value);
  const year = date.getFullYear();
  return `${year === currentYear ? '' : `${year}년 `}${date.getMonth() + 1}월 ${date.getDate()}일`;
}

function destination(name) {
  const last = name.charCodeAt(name.length - 1);
  const finalConsonant = (last - 0xAC00) % 28;
  return name + (last >= 0xAC00 && last <= 0xD7A3 && finalConsonant !== 0 && finalConsonant !== 8 ? '으로' : '로');
}

export function inquiryResultText(category, transactions, currentYear = new Date().getFullYear()) {
  const label = CATEGORY_LABELS[category];
  if (!label) return '';
  const latest = transactions
    .filter((item) => item.category === category && (category !== 'PENSION' || item.transactionType === 'DEPOSIT'))
    .sort((a, b) => b.transactionAt.localeCompare(a.transactionAt) || b.transactionId - a.transactionId)[0];

  if (!latest) return `이 계좌에는 ${label}${category === 'PENSION' ? ' 입금' : ''} 내역이 없어요.`;

  const date = formatDate(latest.transactionAt, currentYear);
  const amount = formatAmount(latest.amount);
  const counterparty = latest.counterpartyName?.trim();
  if (category === 'PENSION') {
    return `가장 최근 연금은 ${date}에 ${counterparty ? `${counterparty}에서 ` : ''}입금됐어요. 금액은 ${amount}이에요.`;
  }
  const deposit = latest.transactionType === 'DEPOSIT';
  const party = counterparty ? `${deposit ? `${counterparty}에서` : destination(counterparty)} ` : '';
  return `가장 최근 ${label} 거래는 ${date}이에요. ${party}${amount}이 ${deposit ? '입금' : '출금'}됐어요.`;
}
