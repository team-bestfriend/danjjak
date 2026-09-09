import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { compileScript, parse } from '@vue/compiler-sfc';
import { createRenderer, nextTick } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { useAppStore } from '../src/stores/appStore.js';
import { actionFor, openChatAction, showChatFab } from '../src/features/chat/chatActions.js';
import { discardChatConversation, preserveChatConversation, restoreChatConversation } from '../src/features/chat/chatConversation.js';
import { sendChatMessage } from '../src/api/chatApi.js';

const source = await readFile(new URL('../src/views/ChatView.vue', import.meta.url), 'utf8');
const taskViewSource = await readFile(new URL('../src/views/TaskView.vue', import.meta.url), 'utf8');
const compiled = compileScript(parse(source).descriptor, { id: 'chat-test', inlineTemplate: true });
const code = ("import { h } from 'vue';\n" + compiled.content)
  .replace(/import danjjakee from '[^']+\.png';/, "const danjjakee = 'danjjakee.png';")
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
  message: '돈 보내는 걸 도와드릴게요.', action, patternId: null, retryable: false,
  showRecommendations: false, ...extra,
}));
const analysisResponse = (patterns = [], status = patterns.length ? 'AVAILABLE' : 'NO_DATA') => new Response(JSON.stringify({
  status, from: '2026-09-03', to: '2026-09-09', patterns, steps: [], difficultStep: null,
}));

test('조회 화면은 시연용 표현 없이 조회 기간을 안내한다', () => {
  assert.doesNotMatch(taskViewSource, /시연용 거래 데이터/);
  assert.match(taskViewSource, /조회 기간 · 전체 기간/);
});

async function mount(t, fetcher, usagePatterns = []) {
  discardChatConversation();
  const original = globalThis.fetch;
  globalThis.fetch = (url, options) => String(url).startsWith('/api/usage-analysis')
    ? Promise.resolve(analysisResponse(usagePatterns))
    : fetcher(url, options);
  const originalDocument = globalThis.Document;
  const originalShadowRoot = globalThis.ShadowRoot;
  globalThis.Document = class {};
  globalThis.ShadowRoot = class {};
  t.after(() => { globalThis.Document = originalDocument; globalThis.ShadowRoot = originalShadowRoot; });
  const pinia = createPinia(); const store = useAppStore(pinia);
  const router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/chat', name: 'chat', component: View },
    { path: '/transfer/source', name: 'transfer-source', component: {} },
    { path: '/tasks/balance', name: 'task-4', component: {} },
  ] });
  await router.push('/chat'); store.navigate = name => router.push({ name });
  const root = node('root');
  const app = renderer.createApp(View).use(pinia).use(router); app.mount(root);
  t.after(() => { app.unmount(); globalThis.fetch = original; });
  t.after(discardChatConversation);
  await nextTick();
  async function send(message) {
    all(root).find(n => n.type === 'textarea').props['onUpdate:modelValue'](message);
    await nextTick();
    all(root).find(n => n.type === 'form').props.onSubmit({ preventDefault() {} });
    await nextTick();
  }
  return { root, router, store, send };
}

test('금융 업무에 다녀오는 동안만 채팅 내용과 추천을 한 번 복원한다', () => {
  const conversation = {
    messages: [{ id: 1, role: 'user', message: '잔액 보고 싶어', action: 'NONE' }],
    recommendations: [{ action: 'BALANCE_CHECK', label: '잔액 확인하기', patternId: 42 }],
    nextMessageId: 2,
  };

  preserveChatConversation(conversation);
  conversation.messages[0].message = '바뀐 내용';
  const restored = restoreChatConversation();

  assert.equal(restored.messages[0].message, '잔액 보고 싶어');
  assert.equal(restored.recommendations[0].patternId, 42);
  assert.equal(restored.nextMessageId, 2);
  assert.equal(restoreChatConversation(), null);
});

test('화면 렌더링, 입력 전송, 로딩, CTA 클릭 후 기존 송금 route 이동', async t => {
  let complete; const calls = [];
  const { root, router, store, send } = await mount(t, (url, options) => {
    calls.push([url, JSON.parse(options.body)]);
    return new Promise(resolve => { complete = resolve; });
  });
  let starts = 0; store.startTransfer = () => { starts++; };
  assert.match(text(root), /단짝에게 물어보기/);
  assert.match(text(root), /무엇을 도와드릴까요/);
  assert.equal(all(root).filter(n => n.type === 'button' && ['송금 시작하기', '잔액 확인하기', '연금 입금 확인하기'].includes(text(n).trim())).length, 3);
  await send('아들에게 돈 보내고 싶어');
  assert.deepEqual(calls, [['/api/chat/messages', { message: '아들에게 돈 보내고 싶어' }]]);
  assert.match(text(root), /생각하고 있어요/); assert.equal(starts, 0);
  complete(response()); await flush();
  assert.doesNotMatch(text(root), /생각하고 있어요/);
  const cta = all(root).filter(n => n.type === 'button' && text(n).trim() === '송금 시작하기').at(-1);
  assert.ok(cta); cta.props.onClick(); await flush();
  assert.equal(starts, 1); assert.equal(router.currentRoute.value.name, 'transfer-source');
});

test('오류 후 다시 물어보기는 초기 인사와 추천 3개로 돌아간다', async t => {
  let calls = 0;
  const { root, send } = await mount(t, async () => { calls++; throw new Error('offline'); });
  await send('잔액 확인'); await flush();
  assert.match(text(root), /잔액 확인/); assert.match(text(root), /문장을 바꿔 적어 주세요/);
  all(root).find(n => n.type === 'button' && text(n).trim() === '다시 물어보기').props.onClick();
  await flush();
  assert.equal(calls, 1);
  assert.doesNotMatch(text(root), /문장을 바꿔 적어 주세요/);
  assert.match(text(root), /자주 하시는 일을 아래에서 바로 눌러도 돼요/);
  assert.equal(all(root).filter(n => n.type === 'button'
    && ['송금 시작하기', '잔액 확인하기', '연금 입금 확인하기'].includes(text(n).trim())).length, 3);
});

test('이용 빈도 상위 3개만 추천하고 추천 클릭 후 대화와 CTA를 먼저 보여준다', async t => {
  let chatCalls = 0;
  const usagePatterns = [
    { patternId: 11, patternType: 'PENSION_CHECK', title: '연금 확인', completedCount: 3 },
    { patternId: 12, patternType: 'BALANCE_CHECK', title: '잔액 보기', completedCount: 8 },
    { patternId: 13, patternType: 'TRANSFER', title: '아들에게 송금', completedCount: 5 },
    { patternId: 14, patternType: 'CUSTOMER_CENTER', title: '고객센터', completedCount: 1 },
  ];
  const { root, router, store } = await mount(t, () => { chatCalls++; return response(); }, usagePatterns);
  store.loadPatternDetail = async () => ({ patternId: 12, patternType: 'BALANCE_CHECK' });
  store.startPatternExecution = async () => router.push({ name: 'task-4' });
  await flush();
  const quickActions = all(root).filter(n => n.type === 'button'
    && ['잔액 보기', '아들에게 송금', '연금 확인', '고객센터'].includes(text(n).trim()));
  assert.deepEqual(quickActions.map(button => text(button).trim()), ['잔액 보기', '아들에게 송금', '연금 확인']);
  quickActions[0].props.onClick();
  await nextTick();
  assert.equal(chatCalls, 0);
  assert.match(text(root), /잔액을 확인하고 싶어요/);
  assert.match(text(root), /잔액 확인을 도와드릴게요/);
  const cta = all(root).find(n => n.type === 'button' && text(n).trim() === '잔액 확인하기');
  assert.ok(cta);
  cta.props.onClick();
  await flush();
  assert.equal(router.currentRoute.value.name, 'task-4');
});

test('빈 입력은 전송하지 않고 전송 버튼을 비활성화한다', async t => {
  let calls = 0;
  const { root, send } = await mount(t, () => { calls++; return response(); });
  const submit = all(root).find(n => n.type === 'button' && n.props['aria-label'] === '메시지 전송');
  assert.equal(submit.props.disabled, true);
  await send('   ');
  assert.equal(calls, 0);
});

test('인사와 업무 선택 안내 응답에 각각 추천 3개를 붙인다', async t => {
  let calls = 0;
  const replies = [
    response('NONE', { message: '안녕하세요! 무엇을 도와드릴까요?', showRecommendations: true }),
    response('NONE', {
      message: '송금이나 잔액 확인을 도와드릴 수 있어요. 원하는 일을 골라 주세요.',
      showRecommendations: true,
    }),
  ];
  const { root, send } = await mount(t, async () => {
    calls++;
    return replies.shift();
  });
  const recommendationLabels = ['송금 시작하기', '잔액 확인하기', '연금 입금 확인하기'];
  const recommendationCount = () => all(root).filter(n => n.type === 'button'
    && recommendationLabels.includes(text(n).trim())).length;

  await send('안녕');
  await flush();
  assert.match(text(root), /안녕하세요! 무엇을 도와드릴까요/);
  assert.equal(calls, 1);
  assert.equal(recommendationCount(), 6);

  await send('뭘 할 수 있어?');
  await flush();
  assert.equal(calls, 2);
  assert.equal(recommendationCount(), 9);
});

test('감사와 짧은 일상 대화는 API 실패 안내 없이 즉시 답한다', async t => {
  let calls = 0;
  const { root, send } = await mount(t, async () => { calls++; throw new Error('offline'); });
  await send('고마워');
  await send('밥 먹었어?');
  await flush();
  assert.equal(calls, 2);
  assert.match(text(root), /별말씀을요/);
  assert.match(text(root), /저는 밥을 먹지는 않지만/);
  assert.match(text(root), /자주 찾는 도움/);
  assert.doesNotMatch(text(root), /답변을 준비하지 못했어요/);
});

test('자기소개·칭찬·감정 표현에도 짧고 자연스럽게 답한다', async t => {
  let calls = 0;
  const { root, send } = await mount(t, async () => { calls++; throw new Error('offline'); });
  for (const message of ['너 누구야?', '최고야!', '힘들어', '기분 좋아']) await send(message);
  await flush();
  assert.equal(calls, 4);
  assert.match(text(root), /금융 업무를 천천히 도와드리는 단짝/);
  assert.match(text(root), /더 편하게 도와드릴게요/);
  assert.match(text(root), /잠시 쉬어 가며/);
  assert.match(text(root), /좋은 기분이 오래 갔으면/);
});

test('특정 금융 action 응답은 추천 3개 대신 해당 CTA만 추가한다', async t => {
  const { root, send } = await mount(t, async () => response('BALANCE_CHECK', {
    message: '잔액 확인을 도와드릴게요.', showRecommendations: false,
  }));
  await send('잔액 보고 싶어');
  await flush();
  assert.equal(all(root).filter(n => n.type === 'button' && text(n).trim() === '잔액 확인하기').length, 2);
  assert.equal(all(root).filter(n => n.type === 'button' && text(n).trim() === '송금 시작하기').length, 1);
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

test('챗봇 단축번호 CTA는 패턴 종류와 관계없이 등록된 업무를 실행한다', async () => {
  const calls = [];
  await openChatAction({ action: 'PATTERN', patternId: 91 }, {
    loadPatternDetail: async id => { calls.push(id); return { patternId: id, patternType: 'TRANSFER' }; },
    startPatternExecution: async pattern => { calls.push(pattern); },
  });
  assert.deepEqual(calls, [91, { patternId: 91, patternType: 'TRANSFER' }]);
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

test('패턴 금융 업무의 뒤로가기는 홈 고정 이동 대신 실제 진입 화면으로 돌아간다', () => {
  const leaveTask = taskViewSource.match(/async function leaveTask\(\) \{[\s\S]*?\n\}/)?.[0] ?? '';

  assert.match(leaveTask, /await store\.finishPatternExecution\('CANCELLED'\)/);
  assert.match(leaveTask, /store\.goBack\(\)/);
  assert.doesNotMatch(leaveTask, /store\.navigate\('home'/);
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
  assert.match(app, /궁금한 게 있으면 저한테 물어보세요!/);
  assert.match(app, /routeName !== "home"/);
  assert.doesNotMatch(app, /sessionStorage/);
  assert.match(app, /@mouseenter="fabHovered = true"/);
  assert.match(app, /void router.push\(\{ name: 'chat' \}\)/);
  assert.match(app, /prefers-reduced-motion: reduce/);
});
