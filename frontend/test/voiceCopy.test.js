import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { voiceLabel } from '../src/api/guidanceApi.js';

const source = async (path) => readFile(new URL(path, import.meta.url), 'utf8');

test('음성 설정 요약은 AI 음성과 가족 음성 명칭을 사용한다', () => {
  assert.equal(voiceLabel({ voiceMode: 'TTS' }), 'AI 음성');
  assert.equal(voiceLabel({ voiceMode: null }, 'TTS'), '전체 설정 · AI 음성');
  assert.equal(voiceLabel({ voiceMode: 'FAMILY' }), '가족 음성 · 녹음 없으면 AI 음성');
});

test('설정과 음성 편집 화면에 이전 표시 명칭이 남지 않고 음성 아이콘을 표시한다', async () => {
  const [settings, focus, editor, segment] = await Promise.all([
    source('../src/views/SettingsView.vue'),
    source('../src/components/common/FocusModeCard.vue'),
    source('../src/components/common/PatternVoiceEditor.vue'),
    source('../src/components/common/SegControl.vue'),
  ]);

  assert.doesNotMatch(settings, /단짝 시연 사용자|자동 음성\(TTS\)/);
  assert.doesNotMatch(focus, /자동 TTS/);
  assert.doesNotMatch(editor, /TTS 자동 음성|대본을 TTS로|TTS로 안내|TTS로 바꿀/);
  assert.match(settings, /label: 'AI 음성', icon: robotIcon/);
  assert.match(settings, /label: '가족 음성', icon: micIcon/);
  assert.match(focus, /'AI 음성'/);
  assert.match(editor, />AI 음성</);
  assert.match(editor, /:src="robotIcon"/);
  assert.match(editor, /:src="micIcon"/);
  assert.doesNotMatch(editor, /🤖|🎙️/);
  assert.match(segment, /v-if="o\.icon"/);
});
