<template>
  <div class="flex h-full flex-col bg-[#FAFAF8] text-[#111827]">
    <SafeArea />
    <TopBar title="단짝에게 물어보기" :onBack="store.goBack" />
    <div ref="chatArea" role="log" aria-label="단짝과의 대화" aria-live="polite"
      class="min-h-0 flex-1 space-y-4 overflow-y-auto p-4" style="font-size: 20px; line-height: 1.6;">
      <article v-for="(item, index) in messages" :key="index"
        :class="['rounded-[20px] border p-4', item.role === 'user'
          ? 'ml-8 border-[#F5D56A] bg-[#FFF3CC]' : 'mr-3 border-[#EBEBEA] bg-white']">
        <p class="mb-1 font-bold">{{ item.role === 'user' ? '나' : '단짝' }}</p>
        <p class="whitespace-pre-wrap break-words">{{ item.message }}</p>
        <Btn v-if="actionFor(item.action)" class="mt-4" :disabled="opening" @click="openAction(item)">
          {{ actionFor(item.action).label }}
        </Btn>
        <Btn v-if="item.retryable" variant="secondary" class="mt-4" :disabled="loading" @click="retry(item)">
          다시 물어보기
        </Btn>
      </article>
      <article v-if="loading" class="mr-3 rounded-[20px] border border-[#EBEBEA] bg-white p-4" role="status">
        <p class="font-bold">단짝</p><p>생각하고 있어요...</p>
      </article>
      <p v-if="actionError" role="alert">{{ actionError }}</p>
      <div v-if="showMenu" class="space-y-3" aria-label="업무 메뉴">
        <Btn v-for="(action, key) in chatActions" :key="key" variant="secondary" :disabled="opening"
          @click="openAction({ action: key, patternId: null })">{{ action.label }}</Btn>
      </div>
    </div>
    <form class="flex-shrink-0 space-y-2 border-t border-[#EBEBEA] bg-white p-4"
      style="padding-bottom: max(16px, env(safe-area-inset-bottom));" @submit.prevent="send">
      <label for="chat-message" class="block text-[16px] text-[#374151]">비밀번호와 계좌번호는 쓰지 마세요.</label>
      <textarea id="chat-message" v-model="draft" rows="2" maxlength="500" placeholder="무엇을 도와드릴까요?"
        class="w-full resize-none rounded-[18px] border border-[#9CA3AF] p-3 text-[20px] focus:outline-2 focus:outline-[#92650A]"
        :disabled="loading" />
      <Btn type="submit" :disabled="loading || !draft.trim()">전송</Btn>
    </form>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Btn from '../components/common/Btn.vue';
import { useAppStore } from '../stores/appStore.js';
import { sendChatMessage } from '../api/chatApi.js';
import { actionFor, chatActions, containsSensitiveInfo, openChatAction, sensitiveNotice } from '../features/chat/chatActions.js';

const store = useAppStore();
const draft = ref('');
const messages = ref([{ role: 'assistant', message: '무엇을 도와드릴까요? 하고 싶은 일을 말씀해 주세요.', action: 'NONE' }]);
const loading = ref(false);
const opening = ref(false);
const actionError = ref('');
const chatArea = ref(null);
const showMenu = computed(() => !loading.value && messages.value.at(-1)?.action === 'NONE');
let controller;
let disposed = false;

async function scrollToLatest() {
  await nextTick();
  if (chatArea.value) chatArea.value.scrollTop = chatArea.value.scrollHeight;
}

async function ask(message) {
  loading.value = true;
  controller = new AbortController();
  const timeout = setTimeout(() => controller?.abort(), 20000);
  void scrollToLatest();
  try {
    const reply = await sendChatMessage(message, controller.signal);
    if (!disposed) messages.value.push({ ...reply, role: 'assistant', retryMessage: reply.retryable ? message : null });
  } catch {
    if (!disposed) messages.value.push({ role: 'assistant', action: 'NONE', retryable: true, retryMessage: message,
      message: '지금은 답변을 준비하지 못했어요. 다시 물어보거나 아래 메뉴를 눌러 주세요.' });
  } finally {
    clearTimeout(timeout);
    loading.value = false;
    if (!disposed) void scrollToLatest();
  }
}

async function send() {
  const message = draft.value.trim();
  if (!message || loading.value) return;
  draft.value = '';
  if (containsSensitiveInfo(message)) {
    messages.value.push({ role: 'assistant', message: sensitiveNotice, action: 'NONE' });
    void scrollToLatest();
    return;
  }
  messages.value.push({ role: 'user', message });
  await ask(message);
}

async function retry(item) {
  if (loading.value || !item.retryMessage) return;
  item.retryable = false;
  await ask(item.retryMessage);
}

async function openAction(item) {
  if (opening.value) return;
  opening.value = true;
  actionError.value = '';
  try {
    await openChatAction(item, store);
  } catch {
    actionError.value = '업무 화면을 열지 못했어요. 버튼을 다시 눌러 주세요.';
  } finally {
    opening.value = false;
  }
}

onBeforeUnmount(() => {
  disposed = true;
  controller?.abort();
});
</script>
