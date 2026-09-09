import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';

const readSource = (path) => readFile(new URL(path, import.meta.url), 'utf8');
const [html, app, accessibilityCss, indexCss, safeArea, onboarding, intro, login, nicknameSetup, guardianSetup, addPerson, patternRegister, transferFlow] = await Promise.all([
  readSource('../index.html'),
  readSource('../src/App.vue'),
  readSource('../src/accessibility.css'),
  readSource('../src/index.css'),
  readSource('../src/components/common/SafeArea.vue'),
  readSource('../src/views/OnboardingView.vue'),
  readSource('../src/views/FeatureIntroView.vue'),
  readSource('../src/views/LoginView.vue'),
  readSource('../src/views/NicknameSetupView.vue'),
  readSource('../src/views/GuardianSetupView.vue'),
  readSource('../src/views/AddPersonView.vue'),
  readSource('../src/views/PatternRegisterView.vue'),
  readSource('../src/views/TransferFlowView.vue'),
]);

test('앱 화면은 기준 폭과 동적 높이 및 기기 안전 영역을 사용한다', () => {
  assert.match(html, /viewport-fit=cover/);
  assert.match(app, /maxWidth: "428px"/);
  assert.match(app, /height: "100dvh"/);
  assert.match(app, /paddingBottom: "env\(safe-area-inset-bottom\)"/);
  assert.match(safeArea, /safe-area-top/);
  assert.match(indexCss, /height: max\(36px, env\(safe-area-inset-top\)\)/);
});

test('공통 버튼과 축소 가능한 내용 영역은 작은 화면에서도 조작할 수 있다', () => {
  assert.match(accessibilityCss, /min-width: 48px; min-height: 48px/);
  assert.match(indexCss, /min-height: 0/);
  assert.match(indexCss, /scroll-padding-block: 1rem/);
  for (const source of [nicknameSetup, guardianSetup, addPerson, patternRegister, transferFlow]) {
    assert.match(source, /overflow-y-auto/);
  }
});

test('첫 이용 화면은 노치와 키보드를 고려한 배치를 사용한다', () => {
  assert.ok(login.indexOf('단짝 로그인하기') < login.indexOf(':src="kakaoLoginButton"'));
  assert.match(nicknameSetup, /mx-auto flex h-\[138px\]/);
  assert.match(nicknameSetup, /@focus="keepNicknameInputVisible"/);
  assert.match(nicknameSetup, /scrollIntoView/);

  for (const source of [nicknameSetup, guardianSetup]) {
    assert.match(source, /padding-top: max\(36px, env\(safe-area-inset-top\)\)/);
  }
});

test('화면에서 사용하는 큰 제목도 글씨 크기 설정을 적용한다', () => {
  for (const size of [34, 43, 72]) {
    assert.match(accessibilityCss, new RegExp(`text-\\[${size}px\\]`));
  }
});

test('첫 이용 화면은 낮은 화면에서도 내용을 스크롤하고 주요 버튼을 유지한다', () => {
  for (const source of [onboarding, intro, login]) {
    assert.match(source, /overflow-y-auto/);
    assert.match(source, /flex-shrink-0/);
  }
  assert.match(login, /<SafeArea \/>/);
  assert.doesNotMatch(login, /pt-\[32dvh\]|pb-\[25dvh\]/);
});
