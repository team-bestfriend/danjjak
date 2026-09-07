import assert from 'node:assert/strict';
import test from 'node:test';
import { createRenderer, nextTick, ref } from 'vue';
import { guidanceApi, saveGuidanceDraft, withGuidance } from '../src/api/guidanceApi.js';
import { patternApi } from '../src/api/patternApi.js';
import { useFamilyRecorder } from '../src/composables/useFamilyRecorder.js';
import { useGuidanceAudio } from '../src/composables/useGuidanceAudio.js';

const renderer = createRenderer({ createComment: () => ({}), insert() {}, remove() {}, parentNode: () => null, nextSibling: () => null });
const response = (value, status = 200) => new Response(JSON.stringify(value), { status });
const settle = async () => { await nextTick(); await new Promise((resolve) => setImmediate(resolve)); };

test('녹음 업로드는 브라우저 multipart 경계와 세션을 사용하고 문구 저장 이후 수행한다', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  const calls = [];
  globalThis.fetch = async (url, options) => {
    calls.push({ url, options });
    return response({ target: 'start', text: '시작해요.', voiceMode: 'FAMILY' });
  };
  const blob = new Blob(['sample'], { type: 'audio/webm' });
  await saveGuidanceDraft(3, 'start', { text: '시작해요.', voiceMode: 'FAMILY', recording: { blob, text: '시작해요.' } });
  assert.equal(calls.length, 2);
  assert.equal(calls[0].options.method, 'PUT');
  assert.equal(calls[1].url, '/api/patterns/3/guidance/start/audio');
  assert.ok(calls[1].options.body instanceof FormData);
  assert.equal(calls[1].options.headers['Content-Type'], undefined);
  assert.equal(calls[1].options.credentials, 'include');
  assert.equal(await calls[1].options.body.get('file').text(), 'sample');
});

test('부분 저장 실패는 완료된 문구를 알리고 녹음 초안을 보존하며 잘못된 대본 녹음은 보내지 않는다', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  const draft = { text: '새 안내', voiceMode: 'FAMILY', recording: { blob: new Blob(['sample']), text: '새 안내' } };
  let count = 0;
  const saved = [];
  globalThis.fetch = async () => ++count === 1 ? response({ text: '새 안내' }) : response({ message: '저장 실패' }, 503);
  await assert.rejects(saveGuidanceDraft(3, 'start', draft, (value) => saved.push(value)), /저장 실패/);
  assert.equal(saved.length, 1);
  assert.ok(draft.recording.blob);
  draft.text = '녹음 후 수정';
  await assert.rejects(saveGuidanceDraft(3, 'start', draft), /다시 녹음/);
  assert.equal(count, 2);
});

test('안내 설정 조회가 실패하면 금융 실행을 시작하지 않는다', async (t) => {
  const original = globalThis.fetch;
  t.after(() => { globalThis.fetch = original; });
  const calls = [];
  globalThis.fetch = async (url) => { calls.push(url); return response({ message: '실패' }, 503); };
  await assert.rejects(patternApi.startExecution(3));
  assert.deepEqual(calls, ['/api/patterns/3/guidance']);
});

test('시작 안내와 단계 음성 선택을 서로 덮어쓰지 않는다', () => {
  const detail = withGuidance({ steps: [{ stepCode: 'A' }, { stepCode: 'B' }] }, [
    { target: 'start', voiceMode: 'FAMILY', audioUrl: '/api/start' },
    { target: 'A', voiceMode: 'TTS' }, { target: 'B', voiceMode: null },
  ]);
  assert.equal(detail.guidance.voiceMode, 'FAMILY');
  assert.equal(detail.steps[0].guidance.voiceMode, 'TTS');
  assert.equal(detail.steps[1].guidance.voiceMode, null);
});

test('녹음 권한 거절 후 재시도·실제 데이터 수집·트랙 해제를 수행하며 업로드하지 않는다', async (t) => {
  const descriptors = Object.fromEntries(['navigator', 'MediaRecorder', 'isSecureContext'].map((key) => [key, Object.getOwnPropertyDescriptor(globalThis, key)]));
  const captures = [];
  let deny = true;
  let stopped = 0;
  const track = { stop() { stopped++; } };
  Object.defineProperty(globalThis, 'navigator', { configurable: true, value: { mediaDevices: {
    async getUserMedia() { if (deny) throw Object.assign(new Error(), { name: 'NotAllowedError' }); return { getTracks: () => [track] }; },
  } } });
  globalThis.isSecureContext = true;
  globalThis.MediaRecorder = class {
    static isTypeSupported() { return true; }
    constructor(stream, options) { this.mimeType = options.mimeType; this.state = 'inactive'; }
    start() { this.state = 'recording'; }
    stop() { this.state = 'inactive'; this.ondataavailable({ data: new Blob(['actual capture']) }); this.onstop(); }
  };
  let recorder;
  const app = renderer.createApp({ setup() { recorder = useFamilyRecorder((value) => captures.push(value)); return () => null; } });
  app.mount({});
  t.after(() => {
    app.unmount();
    for (const [key, descriptor] of Object.entries(descriptors)) {
      if (descriptor) Object.defineProperty(globalThis, key, descriptor); else delete globalThis[key];
    }
  });
  await recorder.start('가족 안내');
  assert.match(recorder.error.value, /권한이 거절/);
  assert.equal(recorder.state.value, 'idle');
  deny = false;
  await recorder.start('가족 안내');
  assert.equal(recorder.state.value, 'recording');
  assert.equal(captures.length, 0);
  recorder.stop();
  assert.equal(captures[0].text, '가족 안내');
  assert.equal(await captures[0].blob.text(), 'actual capture');
  assert.ok(stopped > 0);
});

test('가족 음성은 선택 속도로 재생하고 실패 시 같은 자막의 TTS로 전환한다', async (t) => {
  const originalAudio = globalThis.Audio;
  const originalFetch = globalThis.fetch;
  const audios = [];
  const requests = [];
  globalThis.Audio = class extends EventTarget {
    constructor() { super(); this.paused = true; audios.push(this); }
    async play() { this.paused = false; this.onplaying?.(); this.dispatchEvent(new Event('play')); }
    pause() { this.paused = true; this.onpause?.(); }
    removeAttribute() {}
    load() {}
  };
  globalThis.fetch = async (url, options) => {
    requests.push(JSON.parse(options.body));
    return new Response(new Blob(['tts'], { type: 'audio/mpeg' }));
  };
  const text = ref('현재 안내');
  let audio;
  const app = renderer.createApp({ setup() {
    audio = useGuidanceAudio(text, { voiceMode: 'FAMILY', familyAudioUrl: '/api/family', speed: 'FAST' });
    return () => null;
  } });
  app.mount({});
  t.after(() => { app.unmount(); globalThis.Audio = originalAudio; globalThis.fetch = originalFetch; });
  await settle();
  assert.equal(audios[0].playbackRate, 1.2);
  assert.equal(audios[0].crossOrigin, 'use-credentials');
  assert.equal(requests.length, 0);
  audios[0].onerror();
  await settle();
  assert.deepEqual(requests, [{ text: '현재 안내', speed: 'FAST' }]);
  assert.match(audio.notice.value, /재생하지 못해/);
  assert.equal(audios[0].paused, true);
});
