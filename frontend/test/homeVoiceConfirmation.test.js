import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { parse, compileScript } from '@vue/compiler-sfc';
import { createRenderer, nextTick, proxyRefs } from 'vue';
import { createPinia } from 'pinia';
import { useAppStore } from '../src/stores/appStore.js';

const source = await readFile(new URL('../src/views/HomeView.vue', import.meta.url), 'utf8');
const compiled = compileScript(parse(source).descriptor, { id: 'home-voice-confirmation-test' });
const code = compiled.content.replace(/import (\w+) from '([^']+\.vue)';/g, (_, name) => `const ${name} = {};`)
  .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
    ? new URL('../src/views/' + path + (path.endsWith('.js') ? '' : '.js'), import.meta.url).href
    : import.meta.resolve(path)));
const { default: Home } = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'));
Home.render = () => null;

const renderer = createRenderer({
  createComment: () => ({}), insert() {}, remove() {}, parentNode: () => null, nextSibling: () => null,
});
const settle = async () => {
  await new Promise((resolve) => setImmediate(resolve));
  await nextTick();
};

function mountHome(t, loadPatternDetail) {
  const originalDocument = globalThis.document;
  const originalRecognition = globalThis.SpeechRecognition;
  const instances = [];
  globalThis.document = {
    activeElement: null,
    addEventListener() {},
    removeEventListener() {},
  };
  globalThis.SpeechRecognition = class {
    constructor() { instances.push(this); }
    start() {}
    stop() {}
    abort() {}
    result(text) {
      const result = Object.assign([{ transcript: text }], { isFinal: true });
      this.onresult?.({ results: [result] });
    }
  };

  const pinia = createPinia();
  const store = useAppStore(pinia);
  const pattern = {
    patternId: 4,
    num: 4,
    label: '잔액 확인',
    patternType: 'BALANCE_CHECK',
  };
  store.patterns = [pattern];
  store.loadPatterns = async () => true;
  store.loadPatternDetail = loadPatternDetail;
  let state;
  const originalSetup = Home.setup;
  Home.setup = (props, context) => {
    const bindings = originalSetup(props, context);
    state = proxyRefs(bindings);
    return bindings;
  };
  const app = renderer.createApp(Home).use(pinia);
  app.mount({});
  t.after(() => {
    app.unmount();
    Home.setup = originalSetup;
    globalThis.document = originalDocument;
    globalThis.SpeechRecognition = originalRecognition;
  });
  return { instances, pattern, state, store };
}

test('음성 단일 일치는 상세 조회 후 확인창만 열고 시작 요청은 기다린다', async (t) => {
  const loaded = [];
  const started = [];
  let pattern;
  const mounted = mountHome(t, async (patternId) => {
    loaded.push(patternId);
    mounted.store.activePattern = pattern;
    return pattern;
  });
  pattern = mounted.pattern;
  mounted.store.startPatternExecution = async (selected) => { started.push(selected.patternId); };

  mounted.state.startSpeech();
  mounted.instances[0].result('사 번');
  await settle();

  assert.deepEqual(loaded, [4]);
  assert.equal(mounted.state.focusedPat.patternId, 4);
  assert.deepEqual(started, []);

  mounted.state.closeFocusedPattern();
  assert.equal(mounted.state.focusedPat, null);
  assert.deepEqual(started, []);

  mounted.state.startSpeech();
  mounted.instances[1].result('4번');
  await settle();
  await mounted.state.startFocusedPattern();
  assert.deepEqual(loaded, [4, 4]);
  assert.deepEqual(started, [4]);
});

test('취소 뒤 늦게 끝난 상세 조회는 확인창이나 실행을 만들지 않는다', async (t) => {
  let resolveDetail;
  const started = [];
  const mounted = mountHome(t, () => new Promise((resolve) => { resolveDetail = resolve; }));
  mounted.store.startPatternExecution = async (selected) => { started.push(selected.patternId); };

  mounted.state.startSpeech();
  mounted.instances[0].result('잔액 알려 줘');
  await nextTick();
  mounted.state.resetSpeech();
  mounted.store.activePattern = mounted.pattern;
  resolveDetail(mounted.pattern);
  await settle();

  assert.equal(mounted.state.focusedPat, null);
  assert.deepEqual(started, []);
});
