import { apiUrl, request } from './httpClient.js';

const targetPath = (id, target) => `/api/patterns/${id}/guidance/${encodeURIComponent(target)}`;

export const guidanceApi = {
  getAll: (id) => request(`/api/patterns/${id}/guidance`),
  update: (id, target, draft) => request(targetPath(id, target), {
    method: 'PUT', body: JSON.stringify({ text: draft.text, voiceMode: draft.voiceMode ?? null }),
  }),
  upload: (id, target, blob) => {
    const body = new FormData();
    body.append('file', blob, 'family-recording');
    return request(`${targetPath(id, target)}/audio`, { method: 'POST', body });
  },
};

export function withGuidance(pattern, targets) {
  const byTarget = Object.fromEntries(targets.map((item) => [item.target, {
    ...item, audioUrl: item.audioUrl ? apiUrl(item.audioUrl) : null,
  }]));
  return {
    ...pattern, guidance: byTarget.start,
    steps: pattern.steps.map((step) => ({ ...step, guidance: byTarget[step.stepCode] })),
  };
}

// 문구/방식 저장과 녹음 업로드의 완료 범위를 호출자에 돌려줘 재시도 때 유지한다.
export async function saveGuidanceDraft(id, target, draft, onSaved = () => {}) {
  if (draft.recording && draft.recording.text !== draft.text) throw new Error('녹음 후 문구가 바뀌었어요. 다시 녹음하거나 새 녹음을 취소해 주세요.');
  const saved = await guidanceApi.update(id, target, draft);
  onSaved(saved);
  if (!draft.recording) return saved;
  const uploaded = await guidanceApi.upload(id, target, draft.recording.blob);
  onSaved(uploaded);
  return uploaded;
}

export function voiceLabel(draft, defaultMode = 'TTS') {
  const mode = draft?.voiceMode ?? defaultMode;
  return `${draft?.voiceMode ? '' : '전체 설정 · '}${mode === 'FAMILY' ? '가족 음성' : 'AI 음성'}${draft?.recording ? ' · 새 녹음 저장 예정' : draft?.audioUrl ? ' · 저장된 녹음 있음' : mode === 'FAMILY' ? ' · 녹음 없으면 AI 음성' : ''}`;
}
