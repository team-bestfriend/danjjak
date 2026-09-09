import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { parse, compileScript } from '@vue/compiler-sfc';
import { createRenderer, h, nextTick, proxyRefs } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { useAppStore } from '../src/stores/appStore.js';

const source = await readFile(new URL('../src/views/InstructionImprovementView.vue', import.meta.url), 'utf8');
const compiled = compileScript(parse(source).descriptor, { id: 'improvement-test' });
const code = compiled.content.replace(/import (\w+) from '([^']+\.vue)';/g, (_, name) => `const ${name} = {};`)
  .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
    ? new URL('../src/views/' + path, import.meta.url).href : import.meta.resolve(path)));
const { default: View } = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'));
const renderer = createRenderer({ createComment: () => ({}), insert() {}, remove() {}, parentNode: () => null, nextSibling: () => null });
const response = (body, status = 200) => new Response(JSON.stringify(body), { status });
const fixture = () => ({ status: 'AVAILABLE', suggestion: { patternId: 3, stepId: 20, stepOrder: 2, stepCode: 'SELECT_PERSON',
  stepName: '받는 사람 선택', patternTitle: '아들에게 송금', currentText: '이전 문구', suggestedText: '쉬운 문구', hasFamilyAudio: true, voiceScriptOutdated: false } });

async function mount(t, fetcher) {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = fetcher;
  const pinia = createPinia();
  const store = useAppStore(pinia);
  const navigations = [];
  store.navigate = (...args) => { navigations.push(args); };
  let state;
  const Page = { ...View, render: () => null, setup(props, context) {
    const bindings = View.setup(props, context); state = proxyRefs(bindings); return bindings;
  } };
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: Page }] });
  await router.push('/?from=2026-09-01&to=2026-09-07');
  const app = renderer.createApp({ render: () => h(RouterView) }).use(pinia).use(router);
  app.mount({});
  t.after(() => { app.unmount(); globalThis.fetch = originalFetch; });
  await new Promise((resolve) => setImmediate(resolve));
  await nextTick();
  return { state, navigations };
}

test('조회만으로 적용하지 않고 명시적 성공 뒤 같은 단계의 가족 녹음 편집으로 이동한다', async (t) => {
  const calls = [];
  const { state, navigations } = await mount(t, async (url, options) => {
    calls.push({ url, options });
    return response(options.method === 'POST' ? { text: '쉬운 문구', audioUrl: '/api/audio', voiceScriptOutdated: true } : fixture());
  });
  assert.equal(calls.length, 1);
  assert.equal(state.suggestion.currentText, '이전 문구');
  await state.apply();
  assert.equal(calls[1].url, '/api/patterns/3/steps/20/instruction-suggestion');
  assert.deepEqual(JSON.parse(calls[1].options.body), { expectedText: '이전 문구', suggestedText: '쉬운 문구' });
  assert.equal(state.suggestion.currentText, '쉬운 문구');
  assert.equal(state.suggestion.voiceScriptOutdated, true);
  assert.equal(state.applied, true);
  state.edit(true);
  assert.deepEqual(navigations, [['step-voice-edit', { params: { patternId: 3, stepOrder: 2 }, query: { rerecord: '1' } }]]);
});

test('적용 실패는 기존 문구와 제안을 유지하고 바뀐 문구 충돌은 재비교를 요구한다', async (t) => {
  const { state } = await mount(t, async (url, options) => options.method === 'POST'
    ? response({ code: 'INSTRUCTION_CHANGED', message: '다시 비교해 주세요.' }, 409) : response(fixture()));
  await state.apply();
  assert.equal(state.suggestion.currentText, '이전 문구');
  assert.equal(state.suggestion.suggestedText, '쉬운 문구');
  assert.equal(state.applied, false);
  assert.equal(state.stale, true);
});

test('제안 재조회 중에도 현재 문구를 유지하고 조회 오류를 제안 영역 상태로 보관한다', async (t) => {
  let callCount = 0;
  let rejectReload;
  const reload = new Promise((resolve, reject) => { rejectReload = reject; });
  const { state } = await mount(t, async () => ++callCount === 1 ? response(fixture()) : reload);

  const loading = state.load();
  assert.equal(state.loading, true);
  assert.equal(state.suggestion.currentText, '이전 문구');
  rejectReload(new Error('연결 실패'));
  await loading;

  assert.equal(state.loading, false);
  assert.equal(state.suggestion.currentText, '이전 문구');
  assert.equal(state.loadError, '연결 실패');
});

for (const status of ['NO_DATA', 'CONSENT_DECLINED', 'CONSENT_REQUIRED']) {
  test(`${status}에서는 제안과 적용 요청을 만들지 않는다`, async (t) => {
    const calls = [];
    const { state } = await mount(t, async (url) => { calls.push(url); return response({ status, suggestion: null }); });
    assert.equal(state.suggestion, null);
    await state.apply();
    assert.equal(calls.length, 1);
    assert.ok(state.emptyMessage);
  });
}
