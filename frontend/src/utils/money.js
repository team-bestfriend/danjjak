const koreanDigits = ['', '일', '이', '삼', '사', '오', '육', '칠', '팔', '구'];
const smallUnits = ['', '십', '백', '천'];
const largeUnits = ['', '만', '억', '조', '경'];

function normalizeWon(value) {
  if (value === null || value === undefined || value === '') return null;
  const amount = Number(value);
  return Number.isSafeInteger(amount) && amount >= 0 ? amount : null;
}

function formatKoreanGroup(value) {
  let result = '';
  for (let position = 3; position >= 0; position -= 1) {
    const divisor = 10 ** position;
    const digit = Math.floor(value / divisor) % 10;
    if (digit === 0) continue;
    if (digit !== 1 || position === 0) result += koreanDigits[digit];
    result += smallUnits[position];
  }
  return result;
}

export function formatWon(value) {
  const amount = normalizeWon(value);
  return amount === null ? '-' : `${amount.toLocaleString('ko-KR')}원`;
}

export function formatKoreanWon(value) {
  let amount = normalizeWon(value);
  if (amount === null || amount === 0) return '';

  const groups = [];
  let position = 0;
  while (amount > 0) {
    const group = amount % 10000;
    if (group > 0) {
      // 명세 표기에 맞춰 10,000원은 '일만원'이 아닌 '만원'으로 읽는다.
      const groupText = position === 1 && group === 1 ? '' : formatKoreanGroup(group);
      groups.unshift(`${groupText}${largeUnits[position]}`);
    }
    amount = Math.floor(amount / 10000);
    position += 1;
  }
  return `${groups.join(' ')}원`;
}

export function formatWonWithKorean(value) {
  const numeric = formatWon(value);
  const korean = formatKoreanWon(value);
  return korean ? `${numeric} · ${korean}` : numeric;
}
