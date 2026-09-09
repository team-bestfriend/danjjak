import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { parse, compileScript } from '@vue/compiler-sfc';
import { createRenderer, h, nextTick, proxyRefs } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';
import { useAppStore } from '../src/stores/appStore.js';
import { bindRouter } from '../src/router/navigation.js';

const source = await readFile(new URL('../src/views/PatternRegisterView.vue', import.meta.url), 'utf8');
const patternListSource = await readFile(new URL('../src/views/PatternListView.vue', import.meta.url), 'utf8');
const patternVoiceEditorSource = await readFile(new URL('../src/components/common/PatternVoiceEditor.vue', import.meta.url), 'utf8');
const compiled = compileScript(parse(source).descriptor, { id: 'wizard-test' });
const code = compiled.content.replace(/import (\w+) from '([^']+\.vue)';/g, (_, name) => `const ${name} = {};`)
  .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
    ? new URL('../src/views/' + path + (path.endsWith('.js') ? '' : '.js'), import.meta.url).href
    : import.meta.resolve(path)));
const { default: Wizard } = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'));
Wizard.render = () => null;
const renderer = createRenderer({ createComment: () => ({}), insert() {}, remove() {}, parentNode: () => null, nextSibling: () => null });
const settle = async () => { await new Promise((resolve) => setImmediate(resolve)); await nextTick(); };

test('패턴 상세는 설명·단계 음성을 패턴 수정 흐름으로 통합한다', () => {
  assert.match(patternListSource, /@click="editDetailPattern">패턴 수정/);
  assert.doesNotMatch(patternListSource, />패턴 설명 음성 수정</);
  assert.doesNotMatch(patternListSource, />단계별 음성 안내 수정</);
  assert.match(source, /현재 금융 업무/);
  assert.match(source, />변경할 수 없음</);
  assert.match(source, /업무를 바꾸면 단계와 안내도 달라져요\./);
  assert.match(source, /role="progressbar"/);
  assert.doesNotMatch(source, /'패턴 등록' \}\} \{\{ stageIndex \+ 1 \}\}단계/);
  assert.match(source, /\{\{ stageIndex \+ 1 \}\} \/ \{\{ stages\.length \}\}단계/);
  assert.match(source, /action-label="음성 저장 및 다음"/);
  assert.doesNotMatch(patternVoiceEditorSource, /이 안내에만 선택한 음성을 사용해요\./);
});

test('패턴 수정은 시작 설명과 특정 단계 음성을 함께 저장한다', async (t) => {
  const originalFetch = globalThis.fetch;
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', name: 'pattern-register', component: Wizard }] });
  await router.push('/?edit=7');
  const pinia = createPinia();
  const store = useAppStore(pinia);
  const template = {
    patternType: 'BALANCE_CHECK', defaultTitle: '잔액 확인', defaultDescription: '기본 설명', available: true,
    steps: [{ stepOrder: 1, stepCode: 'CHECK', stepName: '잔액 확인', instructionText: '기본 단계 안내' }],
  };
  const detail = {
    patternId: 7, patternType: 'BALANCE_CHECK', shortcutNumber: 4, title: '잔액 확인', description: '저장된 설명',
    guidance: { text: '저장된 설명', voiceMode: 'TTS' }, linkedAccount: null,
    steps: [{ stepId: 70, stepOrder: 1, stepCode: 'CHECK', stepName: '잔액 확인', instructionText: '저장된 단계 안내', guidance: { text: '저장된 단계 안내', voiceMode: 'TTS' } }],
  };
  store.patternTemplates = [template];
  store.loadPatternTemplates = store.loadPatterns = store.loadFinancialData = async () => {};
  store.loadPatternDetail = async () => structuredClone(detail);
  const navigations = [];
  store.navigate = async (...args) => { navigations.push(args); };
  store.showToast = () => {};
  const requests = [];
  globalThis.fetch = async (url, options) => {
    requests.push({ url, method: options.method, body: options.body ? JSON.parse(options.body) : null });
    if (url.includes('/guidance/')) return new Response(JSON.stringify({ ...JSON.parse(options.body), audioUrl: null }));
    return new Response(JSON.stringify({ patternId: 7 }));
  };
  let state;
  const originalSetup = Wizard.setup;
  Wizard.setup = (props, context) => {
    const bindings = originalSetup(props, context);
    state = proxyRefs(bindings);
    return bindings;
  };
  const app = renderer.createApp({ render: () => h(RouterView) });
  app.use(router).use(pinia);
  app.mount({});
  t.after(() => { app.unmount(); Wizard.setup = originalSetup; globalThis.fetch = originalFetch; bindRouter(null); });
  await settle();

  assert.deepEqual(state.stages, ['details', 'voice', 'steps', 'confirm']);
  assert.equal(state.stage, 'details');
  assert.equal(state.stageLabel, '기본 정보');
  assert.equal(state.description, '저장된 설명');
  assert.equal(state.stepInstructions[0].instructionText, '저장된 단계 안내');
  state.stageIndex = state.stages.indexOf('voice');
  state.acceptDescription({ text: '바꾼 시작 설명', voiceMode: 'FAMILY', recording: null });
  state.selectedStep = state.stepInstructions[0];
  state.acceptStep({ text: '바꾼 단계 안내', voiceMode: 'TTS', recording: null });
  state.stageIndex = state.stages.indexOf('confirm');
  await state.continueOrSubmit();

  const patternUpdate = requests.find((item) => item.url === '/api/patterns/7' && item.method === 'PATCH');
  assert.equal(patternUpdate.body.description, '바꾼 시작 설명');
  assert.deepEqual(patternUpdate.body.stepInstructions, [{ stepCode: 'CHECK', instructionText: '바꾼 단계 안내' }]);
  assert.deepEqual(requests.filter((item) => item.method === 'PUT').map((item) => item.url), [
    '/api/patterns/7/guidance/start',
    '/api/patterns/7/guidance/CHECK',
  ]);
  assert.equal(requests.some((item) => item.url === '/api/patterns' && item.method === 'POST'), false);
  assert.equal(navigations.at(-1)[0], 'pattern-detail');
});

test('패턴 생성 후 녹음 업로드 실패를 재시도해도 한 패턴만 생성하고 초안을 보존한다', async (t) => {
  const originalFetch = globalThis.fetch;
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', name: 'pattern-register', component: Wizard }] });
  await router.push('/');
  const pinia = createPinia();
  const store = useAppStore(pinia);
  const template = { patternType: 'BALANCE_CHECK', defaultTitle: '잔액 확인', defaultDescription: '잔액을 확인해요.', available: true,
    steps: [{ stepOrder: 1, stepCode: 'CHECK', stepName: '잔액 확인', instructionText: '잔액을 보세요.' }] };
  store.patternTemplates = [template];
  store.loadPatternTemplates = store.loadPatterns = store.loadFinancialData = async () => {};
  store.loadPatternDetail = async () => ({ patternId: 10 });
  const navigations = [];
  store.navigate = async (...args) => { navigations.push(args); };
  store.showToast = () => {};
  let failUpload = true;
  const requests = [];
  globalThis.fetch = async (url, options) => {
    requests.push({ url, method: options.method });
    if (url.endsWith('/audio') && failUpload) return new Response(JSON.stringify({ message: '녹음 업로드 실패' }), { status: 503 });
    const data = url.includes('/guidance/') ? { text: '새 안내', voiceMode: 'FAMILY', audioUrl: '/api/audio' } : { patternId: 10 };
    return new Response(JSON.stringify(data));
  };
  let state;
  const originalSetup = Wizard.setup;
  Wizard.setup = (props, context) => {
    const bindings = originalSetup(props, context);
    state = proxyRefs(bindings);
    return bindings;
  };
  const app = renderer.createApp({ render: () => h(RouterView) });
  app.use(router).use(pinia);
  app.mount({});
  t.after(() => { app.unmount(); Wizard.setup = originalSetup; globalThis.fetch = originalFetch; bindRouter(null); });
  await settle();
  state.selectTemplate(template);
  state.shortcutNumber = 1;
  state.acceptDescription({ text: '새 안내', voiceMode: 'FAMILY', recording: { text: '새 안내', blob: new Blob(['recording'], { type: 'audio/webm' }) } });
  state.stageIndex = state.stages.length - 1;
  await state.continueOrSubmit();
  assert.equal(state.persistedId, 10);
  assert.match(state.submitError, /업로드 실패/);
  assert.ok(state.descriptionVoice.recording);
  assert.equal(navigations.length, 0);
  failUpload = false;
  await state.continueOrSubmit();
  assert.equal(requests.filter((item) => item.url === '/api/patterns' && item.method === 'POST').length, 1);
  assert.equal(requests.filter((item) => item.url === '/api/patterns/10' && item.method === 'PATCH').length, 1);
  assert.equal(state.descriptionVoice.recording, null);
  assert.equal(state.saved, true);
  assert.equal(navigations[0][1].params.patternId, 10);
});
