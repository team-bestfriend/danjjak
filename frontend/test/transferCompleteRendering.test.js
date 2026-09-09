import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { parse, compileScript } from '@vue/compiler-sfc';
import { createSSRApp } from 'vue';
import { renderToString } from 'vue/server-renderer';
import { createPinia } from 'pinia';
import { useAppStore } from '../src/stores/appStore.js';

const viewUrl = new URL('../src/views/TransferFlowView.vue', import.meta.url);
const source = await readFile(viewUrl, 'utf8');
const compiled = compileScript(parse(source).descriptor, {
  id: 'transfer-complete-rendering',
  inlineTemplate: true,
  templateOptions: { ssr: true },
});
const code = compiled.content
  // 완료 화면의 실제 템플릿을 렌더링하되 이미지와 공통 컴포넌트만 대체한다.
  .replace(/import \{ profileImageForPerson \} from ["'][^"']+\/profileImages\.js["'];/g, 'const profileImageForPerson = () => null;')
  .replace(/import (\w+) from ["'][^"']+\.(vue|png)["'];/g, (_, name) => `const ${name} = { inheritAttrs: false, render() { return this.$slots.default?.(); } };`)
  .replace(/from (['"])([^'"]+)\1/g, (_, quote, path) => 'from ' + JSON.stringify(path.startsWith('.')
    ? new URL(path + (path.endsWith('.js') ? '' : '.js'), viewUrl).href
    : import.meta.resolve(path)));
const TransferFlow = (await import('data:text/javascript;base64,' + Buffer.from(code + '\n//# sourceURL=TransferFlowView.render-test.js').toString('base64'))).default;

async function renderComplete(result) {
  const pinia = createPinia();
  useAppStore(pinia).transferResult = result;
  return renderToString(createSSRApp(TransferFlow, { flowStep: 'complete' }).use(pinia));
}

test('송금 완료 화면에 받는 사람, 금액, 잔액과 다음 행동이 표시된다', async () => {
  const html = await renderComplete({
    transactionId: 117,
    recipientName: '김민수',
    amount: 5555555,
    balanceAfter: 47615298,
  });
  for (const text of ['송금이 완료됐어요!', '김민수님에게', '5,555,555원', '555만 5555원', '47,615,298원', '4761만 5298원', '거래내역에서 확인', '홈으로 돌아가기']) {
    assert.ok(html.includes(text), `${text} 표시`);
  }
});

test('송금 결과가 없으면 완료로 표시하지 않고 홈으로 안내한다', async () => {
  const html = await renderComplete(null);
  assert.ok(html.includes('확인할 송금 완료 정보가 없어요.'));
  assert.ok(html.includes('홈으로 돌아가기'));
  assert.ok(!html.includes('송금이 완료됐어요!'));
});
