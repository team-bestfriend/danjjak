import assert from 'node:assert/strict';
import test from 'node:test';
import { swipePage } from '../src/features/shortcutSwipe.js';
import { readFile } from 'node:fs/promises';
import { parse, compileScript } from '@vue/compiler-sfc';
import { createRenderer, proxyRefs } from 'vue';
import { createPinia } from 'pinia';
import { useAppStore } from '../src/stores/appStore.js';

test('가로 스와이프만 페이지를 바꾸고 경계와 순서 변경을 보존한다', () => {
  assert.equal(swipePage(1, -100, 10), 2);
  assert.equal(swipePage(2, 100, -10), 1);
  assert.equal(swipePage(1, -65, 260), 1);
  assert.equal(swipePage(1, -100, 90), 1);
  assert.equal(swipePage(1, -59, 0), 1);
  assert.equal(swipePage(1, -100, 0, true), 1);
  assert.equal(swipePage(1, 100, 0), 1);
  assert.equal(swipePage(3, -100, 0), 3);
});

test('홈의 세로 이동·드래그·취소는 페이지를 유지하고 스와이프 직후 클릭만 막는다', async (t) => {
  const source = await readFile(new URL('../src/views/HomeView.vue', import.meta.url), 'utf8');
  const compiled = compileScript(parse(source).descriptor, { id: 'swipe-test' });
  const code = compiled.content.replace(/import (\w+) from '([^']+\.vue)';/g, (_, name) => `const ${name} = {};`)
    .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
      ? new URL('../src/views/' + path + (path.endsWith('.js') ? '' : '.js'), import.meta.url).href
      : import.meta.resolve(path)));
  const { default: Home } = await import('data:text/javascript;base64,' + Buffer.from(code).toString('base64'));
  const originalDocument = globalThis.document;
  globalThis.document = { removeEventListener() {}, activeElement: null };
  const pinia = createPinia();
  const store = useAppStore(pinia);
  store.loadPatterns = async () => {};
  const setup = Home.setup;
  let state;
  Home.setup = (props, context) => { const bindings = setup(props, context); state = proxyRefs(bindings); return bindings; };
  Home.render = () => null;
  const renderer = createRenderer({ createComment: () => ({}), insert() {}, remove() {}, parentNode: () => null, nextSibling: () => null });
  const app = renderer.createApp(Home).use(pinia);
  app.mount({});
  t.after(() => { app.unmount(); globalThis.document = originalDocument; });
  const scroll = { scrollTop: 200 };
  const target = { closest: () => scroll, setPointerCapture() {} };
  const event = (x, y) => ({ pointerId: 1, pointerType: 'touch', button: 0, clientX: x, clientY: y, currentTarget: target });
  state.beginSwipe(event(250, 300));
  state.moveSwipe(event(185, 560));
  state.endSwipe(event(185, 560));
  assert.equal(store.homePage, 1);
  assert.equal(scroll.scrollTop, -60);
  state.beginSwipe(event(250, 300));
  state.moveSwipe(event(100, 305));
  state.endSwipe(event(100, 305));
  assert.equal(store.homePage, 2);
  let blocked = 0;
  const click = { detail: 1, preventDefault() { blocked++; }, stopPropagation() {} };
  state.guardSwipeClick(click);
  state.guardSwipeClick(click);
  assert.equal(blocked, 1);
  state.beginSwipe(event(250, 300));
  state.drag = { sourceNum: 1 };
  state.endSwipe(event(100, 300));
  assert.equal(store.homePage, 2);
  state.drag = null;
  state.beginSwipe(event(250, 300));
  state.cancelSwipe();
  state.endSwipe(event(100, 300));
  assert.equal(store.homePage, 2);
  state.guardSwipeClick({ ...click, detail: 0 });
  assert.equal(blocked, 1);
});
