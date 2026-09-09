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
