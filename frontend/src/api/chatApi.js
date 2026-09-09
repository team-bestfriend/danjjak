import { request } from './httpClient.js';
import { actionFor } from '../features/chat/chatActions.js';

export async function sendChatMessage(message, signal) {
  const reply = await request('/api/chat/messages', {
    method: 'POST', body: JSON.stringify({ message }), signal,
  });
  if (!reply || typeof reply.message !== 'string' || !reply.message.trim()
    || reply.message.length > 300 || (reply.action !== 'NONE' && !actionFor(reply.action))
    || typeof reply.retryable !== 'boolean'
    || typeof reply.showRecommendations !== 'boolean'
    || (reply.action !== 'NONE' && reply.showRecommendations)
    || (reply.patternId !== null && (!Number.isSafeInteger(reply.patternId) || reply.patternId < 1))) {
    throw new Error('Invalid chat response');
  }
  return reply;
}
