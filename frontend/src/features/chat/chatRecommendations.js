import { actionFor } from './chatActions.js';

const FALLBACK_RECOMMENDATIONS = Object.freeze([
  { label: '송금 시작하기', action: 'TRANSFER', patternId: null },
  { label: '잔액 확인하기', action: 'BALANCE_CHECK', patternId: null },
  { label: '연금 입금 확인하기', action: 'PENSION_CHECK', patternId: null },
]);

export function selectChatRecommendations(report) {
  const usedActions = new Set();
  const recommendations = report?.status === 'AVAILABLE'
    ? [...report.patterns]
      .filter((pattern) => pattern.completedCount > 0 && actionFor(pattern.patternType))
      .sort((first, second) => second.completedCount - first.completedCount)
      .map((pattern) => ({ label: pattern.title, action: pattern.patternType, patternId: pattern.patternId }))
      .slice(0, 3)
    : [];

  recommendations.forEach((pattern) => usedActions.add(pattern.action));

  for (const fallback of FALLBACK_RECOMMENDATIONS) {
    if (recommendations.length === 3) break;
    if (!usedActions.has(fallback.action)) {
      recommendations.push({ ...fallback });
      usedActions.add(fallback.action);
    }
  }
  return recommendations;
}

const recommendationCopy = Object.freeze({
  TRANSFER: { user: '송금을 시작하고 싶어요.', assistant: '송금을 도와드릴게요.' },
  BALANCE_CHECK: { user: '잔액을 확인하고 싶어요.', assistant: '잔액 확인을 도와드릴게요.' },
  PENSION_CHECK: { user: '연금 입금을 확인하고 싶어요.', assistant: '연금 입금 확인을 도와드릴게요.' },
});

export function recommendationMessages(recommendation) {
  const copy = recommendationCopy[recommendation.action];
  return {
    user: copy?.user ?? `“${recommendation.label}” 업무를 도와주세요.`,
    assistant: copy?.assistant ?? `“${recommendation.label}” 업무를 도와드릴게요.`,
  };
}
