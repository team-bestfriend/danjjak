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
const compiled = compileScript(parse(source).descriptor, { id: 'wizard-test' });
const code = compiled.content.replace(/import (\w+) from '([^']+\.vue)';/g, (_, name) => `const ${name} = {};`)
  .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
    ? new URL('../src/views/' + path + (path.endsWith('.js') ? '' : '.js'), import.meta.url).href
    : import.meta.resolve(path)));
const { default: Wizard } = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'));
Wizard.render = () => null;
const renderer = createRenderer({ createComment: () => ({}), insert() {}, remove() {}, parentNode: () => null, nextSibling: () => null });
const settle = async () => { await new Promise((resolve) => setImmediate(resolve)); await nextTick(); };

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
