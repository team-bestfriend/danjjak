import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { compileScript, parse } from '@vue/compiler-sfc';
import { createRenderer, nextTick } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { useAppStore } from '../src/stores/appStore.js';
import { actionFor, openChatAction, showChatFab } from '../src/features/chat/chatActions.js';
import { sendChatMessage } from '../src/api/chatApi.js';

const source = await readFile(new URL('../src/views/ChatView.vue', import.meta.url), 'utf8');
const compiled = compileScript(parse(source).descriptor, { id: 'chat-test', inlineTemplate: true });
const code = ("import { h } from 'vue';\n" + compiled.content)
  .replace(/import (\w+) from '([^']+\.vue)';/g, (_, name) => name === 'Btn'
    ? "const Btn = { props: ['disabled'], setup: (p, {slots, attrs}) => () => h('button', {...attrs, disabled: p.disabled}, slots.default?.()) };"
    : name === 'TopBar' ? "const TopBar = { props: ['title'], setup: p => () => h('header', p.title) };" : `const ${name} = { render: () => null };`)
  .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
    ? new URL('../src/views/' + path, import.meta.url).href : import.meta.resolve(path)));
const { default: View } = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'));
const node = (type, text = '') => ({ type, text, children: [], props: {}, addEventListener() {}, removeEventListener() {}, getRootNode() { return {}; } });
const renderer = createRenderer({
  createElement: type => node(type), createText: text => node('#text', text), createComment: () => node('#comment'),
  setText: (n, text) => { n.text = text; }, setElementText: (n, text) => { n.text = text; n.children = []; },
  patchProp: (n, key, old, value) => { n.props[key] = value; },
  insert(n, parent, anchor) {
    if (n.parent) n.parent.children = n.parent.children.filter(child => child !== n);
    const index = anchor ? parent.children.indexOf(anchor) : -1;
    parent.children.splice(index < 0 ? parent.children.length : index, 0, n); n.parent = parent;
  },
  remove(n) { if (n.parent) n.parent.children = n.parent.children.filter(child => child !== n); },
  parentNode: n => n.parent, nextSibling: n => n.parent?.children[n.parent.children.indexOf(n) + 1],
});
const all = root => [root, ...root.children.flatMap(all)];
const text = root => root.text + root.children.map(text).join('');
const flush = async () => { await new Promise(resolve => setImmediate(resolve)); await nextTick(); };
const response = (action = 'TRANSFER', extra = {}) => new Response(JSON.stringify({
  message: '돈 보내는 걸 도와드릴게요.', action, patternId: null, retryable: false, ...extra,
}));

async function mount(t, fetcher) {
  const original = globalThis.fetch; globalThis.fetch = fetcher;
  const originalDocument = globalThis.Document;
  const originalShadowRoot = globalThis.ShadowRoot;
  globalThis.Document = class {};
  globalThis.ShadowRoot = class {};
  t.after(() => { globalThis.Document = originalDocument; globalThis.ShadowRoot = originalShadowRoot; });
  const pinia = createPinia(); const store = useAppStore(pinia);
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/chat', name: 'chat', component: View },
    { path: '/transfer/source', name: 'transfer-source', component: {} },
  ] });
  await router.push('/chat'); store.navigate = name => router.push({ name });
  const root = node('root');
  const app = renderer.createApp(View).use(pinia).use(router); app.mount(root);
  t.after(() => { app.unmount(); globalThis.fetch = original; });
  await nextTick();
  async function send(message) {
    all(root).find(n => n.type === 'textarea').props['onUpdate:modelValue'](message);
    await nextTick();
    all(root).find(n => n.type === 'form').props.onSubmit({ preventDefault() {} });
    await nextTick();
  }
  return { root, router, store, send };
}

test('화면 렌더링, 입력 전송, 로딩, CTA 클릭 후 기존 송금 route 이동', async t => {
  let complete; const calls = [];
  const { root, router, store, send } = await mount(t, (url, options) => {
    calls.push([url, JSON.parse(options.body)]);
    return new Promise(resolve => { complete = resolve; });
  });
  let starts = 0; store.startTransfer = () => { starts++; };
  assert.match(text(root), /단짝에게 물어보기/);
  assert.match(text(root), /무엇을 도와드릴까요/);
  await send('아들에게 돈 보내고 싶어');
  assert.deepEqual(calls, [['/api/chat/messages', { message: '아들에게 돈 보내고 싶어' }]]);
  assert.match(text(root), /생각하고 있어요/); assert.equal(starts, 0);
  complete(response()); await flush();
  assert.doesNotMatch(text(root), /생각하고 있어요/);
  const cta = all(root).find(n => n.type === 'button' && text(n).trim() === '송금 시작하기');
  assert.ok(cta); cta.props.onClick(); await flush();
  assert.equal(starts, 1); assert.equal(router.currentRoute.value.name, 'transfer-source');
});

test('오류는 대화를 유지하며 명시적 재시도를 제공한다', async t => {
  let calls = 0;
  const { root, send } = await mount(t, async () => {
    if (++calls === 1) throw new Error('offline');
    return response('BALANCE_CHECK');
  });
  await send('잔액 확인'); await flush();
  assert.match(text(root), /잔액 확인/); assert.match(text(root), /아래 메뉴/);
  all(root).find(n => n.type === 'button' && text(n).trim() === '다시 물어보기').props.onClick();
  await flush(); assert.equal(calls, 2); assert.match(text(root), /잔액 확인하기/);
});

test('민감정보는 표시하거나 전송하지 않는다', async t => {
  let calls = 0;
  const { root, send } = await mount(t, () => { calls++; return response(); });
  await send('비밀번호 1234'); await flush();
  assert.equal(calls, 0); assert.doesNotMatch(text(root), /1234/); assert.match(text(root), /채팅에 쓰지 마세요/);
});

test('조회 CTA는 실제 패턴을 검증하여 기존 패턴 실행을 재사용한다', async () => {
  const calls = [];
  await openChatAction({ action: 'BALANCE_CHECK', patternId: 42 }, {
    loadPatternDetail: async id => { calls.push(id); return { patternId: id, patternType: 'BALANCE_CHECK' }; },
    startPatternExecution: async pattern => { calls.push(pattern); },
  });
  assert.deepEqual(calls, [42, { patternId: 42, patternType: 'BALANCE_CHECK' }]);
  await assert.rejects(openChatAction({ action: 'BALANCE_CHECK', patternId: 42 }, {
    loadPatternDetail: async () => ({ patternType: 'TRANSFER' }),
  }));
});

test('패턴 없는 조회와 사용 안내는 허용된 기존 route로만 이동한다', async () => {
  const calls = [];
  const store = { resetPatternExecution() {}, navigate: async name => { calls.push(name); } };
  for (const action of ['BALANCE_CHECK', 'PENSION_CHECK', 'MANAGEMENT_FEE_CHECK', 'UTILITY_BILL_CHECK', 'CUSTOMER_CENTER', 'APP_HELP', 'NONE', 'constructor']) {
    await openChatAction({ action, patternId: null, route: '/transfer/confirm' }, store);
  }
  assert.deepEqual(calls, ['task-4', 'task-2', 'task-3', 'task-8', 'task-6', 'service-guide']);
  assert.equal(actionFor('constructor'), null);
});

test('잘못된 action 응답은 API 경계에서 거부한다', async t => {
  const original = globalThis.fetch; t.after(() => { globalThis.fetch = original; });
  globalThis.fetch = async () => response('BUY_STOCK');
  await assert.rejects(sendChatMessage('추천'));
});

test('FAB는 주요 4개 화면에서만 표시하고 모든 금융 실행 route에서 숨긴다', async () => {
  for (const route of ['home', 'patterns', 'analysis', 'settings']) assert.equal(showChatFab(route), true);
  const routes = await readFile(new URL('../src/router/index.js', import.meta.url), 'utf8');
  const transferRoutes = routes.split('const transferRoutes = [')[1].split('].map')[0];
  for (const [, route] of transferRoutes.matchAll(/\["([^"]+)"/g)) assert.equal(showChatFab(route), false);
  for (const route of ['chat', 'task-transfer', 'task-4', 'login', 'pattern-detail']) assert.equal(showChatFab(route), false);
  const app = await readFile(new URL('../src/App.vue', import.meta.url), 'utf8');
  assert.match(app, /v-if="showChatFab\(String\(route.name\)\) && !showSplash"/);
  assert.match(app, /router.push\(\{ name: 'chat' \}\)/);
});
