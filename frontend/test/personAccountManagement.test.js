import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { parse, compileScript } from '@vue/compiler-sfc';
import { createRenderer, proxyRefs } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import { useAppStore } from '../src/stores/appStore.js';

async function loadSfc(relativePath) {
  const fileUrl = new URL(relativePath, import.meta.url);
  const source = await readFile(fileUrl, 'utf8');
  const compiled = compileScript(parse(source).descriptor, { id: relativePath });
  const code = compiled.content
    .replace(/import \{ profileImageForPerson \} from ["'][^"']+\/profileImages\.js["'];/g, 'const profileImageForPerson = () => null;')
    .replace(/import (\w+) from ["'][^"']+\.(vue|png)["'];/g, (_, name) => `const ${name} = {};`)
    .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => {
      if (!path.startsWith('.')) return `from ${JSON.stringify(import.meta.resolve(path))}`;
      const resolvedPath = /\.[a-z]+$/i.test(path) ? path : `${path}.js`;
      return `from ${JSON.stringify(new URL(resolvedPath, fileUrl).href)}`;
    });
  return (await import(`data:text/javascript;base64,${Buffer.from(code).toString('base64')}`)).default;
}

const [AddPersonView, AddPersonForm, RecipientAccountForm, ContactManageView] = await Promise.all([
  loadSfc('../src/views/AddPersonView.vue'),
  loadSfc('../src/components/common/AddPersonForm.vue'),
  loadSfc('../src/components/common/RecipientAccountForm.vue'),
  loadSfc('../src/views/ContactManageView.vue'),
]);
const addPersonViewSource = await readFile(
  new URL('../src/views/AddPersonView.vue', import.meta.url),
  'utf8',
);

const renderer = createRenderer({
  createComment: () => ({}),
  insert() {},
  remove() {},
  parentNode: () => null,
  nextSibling: () => null,
});

function mountSfc(t, component, props, pinia, router = null) {
  let state;
  const app = renderer.createApp({
    ...component,
    setup(componentProps, context) {
      const bindings = component.setup(componentProps, context);
      state = proxyRefs(bindings);
      return bindings;
    },
    render: () => null,
  }, props).use(pinia);
  if (router) app.use(router);
  app.mount({});
  t.after(() => app.unmount());
  return state;
}

test('사람 및 받는 계좌 저장은 응답 본문 없이도 완료 이벤트를 보낸다', async (t) => {
  const pinia = createPinia();
  const store = useAppStore(pinia);
  const personSavedEvents = [];
  const accountSavedEvents = [];
  store.saveRegisteredPerson = async () => null;
  store.saveRecipientAccount = async () => null;

  const personForm = mountSfc(t, AddPersonForm, {
    existingPerson: {
      id: 2,
      name: '김지영',
      relation: '딸',
      profileImageKey: 'adult_woman',
    },
    onSaved: (...args) => personSavedEvents.push(args),
  }, pinia);
  const accountForm = mountSfc(t, RecipientAccountForm, {
    registeredPersonId: 2,
    personName: '김지영',
    existingAccount: {
      accountId: 9,
      bankCode: '088',
      bankName: '신한은행',
      accountNumber: '110-222-333333',
      accountAlias: '생활비',
    },
    onSaved: (...args) => accountSavedEvents.push(args),
  }, pinia);

  await personForm.handleSave();
  await accountForm.handleSave();

  assert.deepEqual(personSavedEvents, [[]]);
  assert.deepEqual(accountSavedEvents, [[]]);
});

test('저장 후 목록을 재조회하는 동안 수정·생성 폼을 유지한다', () => {
  assert.match(addPersonViewSource, /v-if="initialLoading"/);
  assert.doesNotMatch(addPersonViewSource, /v-if="store\.financeLoading"/);
});

test('저장 완료 후 편집 상태를 비우고 사람 및 계좌 관리 목록으로 이동한다', async (t) => {
  const pinia = createPinia();
  const store = useAppStore(pinia);
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/settings/people', name: 'contact-manage', component: { render: () => null } },
      { path: '/settings/people/edit', name: 'add-person', component: { render: () => null } },
    ],
  });
  await router.push({ name: 'add-person' });
  store.loadFinancialData = async () => true;
  store.editingPersonId = 2;
  store.accountFormPersonId = 2;
  store.editingRecipientAccountId = 9;

  const view = mountSfc(t, AddPersonView, {}, pinia, router);
  await view.onSaved();

  assert.equal(router.currentRoute.value.name, 'contact-manage');
  assert.equal(store.editingPersonId, null);
  assert.equal(store.accountFormPersonId, null);
  assert.equal(store.editingRecipientAccountId, null);
});

test('계좌 삭제 확인 후 선택한 사람과 계좌 식별자로 삭제한다', async (t) => {
  const pinia = createPinia();
  const store = useAppStore(pinia);
  const calls = [];
  store.loadFinancialData = async () => true;
  store.deleteRecipientAccount = async (...args) => {
    calls.push(args);
    return true;
  };
  const view = mountSfc(t, ContactManageView, {}, pinia);
  const person = { id: 2, name: '김지영' };
  const account = {
    accountId: 9,
    bankName: '신한은행',
    masked: '110-****-333',
  };

  view.deleteAccount(person, account);
  assert.equal(view.deleteDialog.target, 'account');
  await view.confirmDeleteTarget();

  assert.deepEqual(calls, [[2, 9]]);
  assert.equal(view.deleteDialog, null);
});
