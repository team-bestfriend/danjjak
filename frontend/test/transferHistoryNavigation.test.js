import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { parse, compileScript } from '@vue/compiler-sfc';
import { createRenderer, proxyRefs } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { useAppStore } from '../src/stores/appStore.js';
import { bindRouter } from '../src/router/navigation.js';

async function loadView(filename) {
  const source = await readFile(new URL(`../src/views/${filename}`, import.meta.url), 'utf8');
  const compiled = compileScript(parse(source).descriptor, { id: filename });
  const code = compiled.content
    // 화면 이동 테스트에서는 이미지 표시 함수를 대체해 PNG 로딩을 피한다.
    .replace(/import \{ profileImageForPerson \} from ["'][^"']+\/profileImages\.js["'];/g, 'const profileImageForPerson = () => null;')
    .replace(/import (\w+) from ["'][^"']+\.(vue|png)["'];/g, (_, name) => `const ${name} = {};`)
    .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
      ? new URL('../src/views/' + path + (path.endsWith('.js') ? '' : '.js'), import.meta.url).href
      : import.meta.resolve(path)));
  return (await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'))).default;
}

const TransferFlow = await loadView('TransferFlowView.vue');
const Task = await loadView('TaskView.vue');
const renderer = createRenderer({
  createComment: () => ({}), insert() {}, remove() {}, parentNode: () => null, nextSibling: () => null,
});

function mountView(t, view, props, pinia) {
  let state;
  const app = renderer.createApp({
    ...view,
    setup(props, context) {
      const bindings = view.setup(props, context);
      state = proxyRefs(bindings);
      return bindings;
    },
    render: () => null,
  }, props).use(pinia);
  app.mount({});
  t.after(() => app.unmount());
  return state;
}

function nextNavigation(router, action) {
  return new Promise((resolve) => {
    const remove = router.afterEach(() => {
      remove();
      resolve();
    });
    action();
  });
}

for (const previousScreen of ['pin-entry', 'fraud-warning']) {
  for (const backMethod of ['화면 뒤로 버튼', '브라우저 뒤로 가기']) {
    test(`${previousScreen} 이후 송금 완료 → 거래내역 → ${backMethod} 실행 시 홈으로 이동한다`, async (t) => {
      const pinia = createPinia();
      const store = useAppStore(pinia);
      const router = createRouter({
        history: createMemoryHistory(),
        routes: ['home', 'pin-entry', 'fraud-warning', 'complete', 'task-5'].map((name) => ({
          name, path: '/' + name, component: { render: () => null },
        })),
      });
      bindRouter(router);
      t.after(() => bindRouter(null));
      await router.push({ name: 'home' });
      await router.push({ name: previousScreen });
      await router.push({ name: 'complete' });
      store.transferResult = { transactionId: 117, amount: 50000 };
      store.transferAmount = '50000';
      store.loadFinancialData = async () => false;

      const completed = mountView(t, TransferFlow, { flowStep: 'complete' }, pinia);
      await completed.goToHistory();
      assert.equal(router.currentRoute.value.name, 'task-5');
      assert.equal(store.transferResult, null);
      assert.equal(store.transferAmount, '0');

      const history = mountView(t, Task, { taskName: 'task-5' }, pinia);
      await nextNavigation(router, () => backMethod === '화면 뒤로 버튼' ? history.leaveTask() : router.back());
      assert.equal(router.currentRoute.value.name, 'home');
      await nextNavigation(router, () => router.forward());
      assert.equal(router.currentRoute.value.name, 'task-5');
    });
  }
}

for (const pattern of [false, true]) {
  test(`${pattern ? '송금 패턴' : '직접 송금'}: 계좌를 눌러 진행하고 뒤로 이동·재선택해도 선택을 유지한다`, async (t) => {
    const pinia = createPinia();
    const store = useAppStore(pinia);
    const router = createRouter({
      history: createMemoryHistory(),
      routes: ['home', 'transfer-source', 'guide-person', 'guide-account', 'amount-input'].map((name) => ({
        name, path: '/' + name, component: { render: () => null },
      })),
    });
    bindRouter(router);
    t.after(() => bindRouter(null));
    store.ownedAccounts = [{ accountId: 1, primary: true }, { accountId: 2 }];
    store.people = [{ id: 10 }, { id: 20 }];
    store.accountsByPerson = {
      10: [{ accountId: 101, masked: '111-***' }, { accountId: 102, masked: '222-***' }],
      20: [{ accountId: 201, masked: '333-***' }],
    };
    store.financeLoaded = true;
    store.startTransfer(pattern ? { pattern: true, personId: 10, recipientAccountId: 102 } : {});
    await router.push({ name: 'home' });
    await router.push({ name: 'transfer-source' });
    const source = mountView(t, TransferFlow, { flowStep: 'transfer-source' }, pinia);
    await Promise.resolve();
    assert.equal(router.currentRoute.value.name, 'transfer-source');
    assert.equal(store.selectedSourceAccountId, 1);

    store.financeLoading = true;
    await source.handleSelectSourceAccount(2);
    assert.equal(router.currentRoute.value.name, 'transfer-source');
    assert.equal(store.selectedSourceAccountId, 1);
    store.financeLoading = false;

    await source.handleSelectSourceAccount(2);
    assert.equal(router.currentRoute.value.name, 'guide-person');
    assert.equal(store.selectedSourceAccountId, 2);
    await nextNavigation(router, () => store.goBack());
    assert.equal(router.currentRoute.value.name, 'transfer-source');
    assert.equal(store.selectedSourceAccountId, 2);
    await source.handleSelectSourceAccount(2);
    assert.equal(router.currentRoute.value.name, 'guide-person');

    const person = mountView(t, TransferFlow, { flowStep: 'guide-person' }, pinia);
    await nextNavigation(router, () => person.handleSelectFamilyPerson(10));
    assert.equal(router.currentRoute.value.name, 'guide-account');
    assert.equal(store.selectedRecipientAccountId, pattern ? 102 : null);
    const account = mountView(t, TransferFlow, { flowStep: 'guide-account' }, pinia);
    await nextNavigation(router, () => account.handleSelectAccount(store.accountsByPerson[10][1]));
    assert.equal(router.currentRoute.value.name, 'amount-input');
    await nextNavigation(router, () => store.goBack());
    assert.equal(store.selectedRecipientAccountId, 102);
    await nextNavigation(router, () => store.goBack());
    assert.equal(store.selectedPersonId, 10);
    await nextNavigation(router, () => person.handleSelectFamilyPerson(10));
    assert.equal(store.selectedRecipientAccountId, 102);
    assert.equal(store.selectedRecipientAccount.accountId, 102);

    await nextNavigation(router, () => store.goBack());
    await nextNavigation(router, () => person.handleSelectFamilyPerson(20));
    assert.equal(store.selectedPersonId, 20);
    assert.equal(store.selectedRecipientAccountId, 201);
  });
}
