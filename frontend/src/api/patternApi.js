import { request } from './httpClient.js';
import { guidanceApi, withGuidance } from './guidanceApi.js';

export const patternApi = {
  getTemplates: () => request('/api/pattern-templates'),
  getPatterns: () => request('/api/patterns'),
  getPattern: async (patternId) => {
    const [pattern, targets] = await Promise.all([
      request(`/api/patterns/${patternId}`), guidanceApi.getAll(patternId),
    ]);
    return withGuidance(pattern, targets);
  },
  createPattern: (payload) => request('/api/patterns', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
  updatePattern: (patternId, payload) => request(`/api/patterns/${patternId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  }),
  reorderPatterns: (items) => request('/api/patterns/order', {
    method: 'PUT',
    body: JSON.stringify({ items }),
  }),
  deactivatePattern: (patternId) => request(`/api/patterns/${patternId}`, {
    method: 'DELETE',
  }),
  startExecution: async (patternId, sourceBankAccountId) => {
    const targets = await guidanceApi.getAll(patternId);
    const result = await request(`/api/patterns/${patternId}/executions`, {
      method: 'POST', body: JSON.stringify(sourceBankAccountId ? { sourceBankAccountId } : {}),
    });
    return { ...result, pattern: withGuidance(result.pattern, targets) };
  },
  startVisit: (executionId, stepId) => request(`/api/pattern-executions/${executionId}/visits`, {
    method: 'POST',
    body: JSON.stringify({ stepId }),
  }),
  updateVisit: (executionId, visitId, payload) => request(`/api/pattern-executions/${executionId}/visits/${visitId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  }),
  finishExecution: (executionId, status) => request(`/api/pattern-executions/${executionId}`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  }),
};
