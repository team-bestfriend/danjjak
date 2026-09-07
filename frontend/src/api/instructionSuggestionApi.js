import { request } from './httpClient.js';

export const instructionSuggestionApi = {
  get: (from, to) => request(`/api/usage-analysis/instruction-suggestion?${new URLSearchParams({ from, to })}`),
  apply: (patternId, stepId, expectedText) => request(`/api/patterns/${patternId}/steps/${stepId}/instruction-suggestion`, {
    method: 'POST', body: JSON.stringify({ expectedText }),
  }),
};
