import assert from 'node:assert/strict';
import test from 'node:test';
import { directAccountPattern } from '../src/features/directRecipient.js';

test('직접 수취 계좌는 숫자 8~20자리와 숫자 사이 하이픈만 허용한다', () => {
  for (const value of ['12345678', '123-45-678901', '12345678901234567890']) assert.equal(directAccountPattern.test(value), true);
  for (const value of ['--------', '1-------', '1234567', '-12345678', '12345678-', '123--45678', '123456789012345678901', 'ABC12345678']) assert.equal(directAccountPattern.test(value), false);
});
