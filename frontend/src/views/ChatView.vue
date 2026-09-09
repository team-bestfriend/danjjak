<template>
  <div class="flex h-full min-h-0 flex-col bg-[#FAFAF8] text-[#111827]">
    <SafeArea />
    <TopBar title="단짝에게 물어보기" :onBack="store.goBack" />

    <main ref="chatArea" role="log" aria-label="단짝과의 대화" aria-live="polite"
      class="min-h-0 flex-1 space-y-5 overflow-y-auto px-4 py-5">
      <div v-for="(item, index) in messages" :key="item.id"
        :class="['flex items-end gap-2.5', item.role === 'user' ? 'justify-end pl-12' : 'pr-8']">
        <img v-if="item.role === 'assistant'" :src="danjjakee" alt="단짝"
          class="h-10 w-10 flex-shrink-0 rounded-full border border-[#F5D56A] bg-[#FFF3CC] object-contain p-0.5" />

        <div :class="['min-w-0', item.role === 'assistant' ? 'max-w-[82%]' : 'max-w-[86%]']">
          <p v-if="item.role === 'assistant'" class="mb-1.5 ml-1 text-[16px] font-bold text-[#374151]">단짝</p>
          <article :class="[
            'rounded-[22px] border px-4 py-3.5 text-[19px] leading-[1.65] shadow-sm',
            item.role === 'user'
              ? 'rounded-br-md border-[#F2CA4D] bg-[#FFF0B8]'
              : 'rounded-bl-md border-[#E5E7EB] bg-white',
          ]">
            <p class="whitespace-pre-wrap break-words">{{ item.message }}</p>
          </article>

          <section v-if="(index === 0 || item.showRecommendations) && recommendations.length"
            class="mt-4" aria-label="자주 찾는 도움">
            <p class="mb-2 text-[16px] font-semibold text-[#6B7280]">자주 찾는 도움</p>
            <div class="flex flex-wrap gap-2">
              <button v-for="recommendation in recommendations"
                :key="`${recommendation.action}:${recommendation.patternId}`" type="button"
                class="min-h-12 rounded-full border border-[#E4B936] bg-[#FFF9E8] px-4 py-2 text-left text-[17px] font-semibold text-[#5F4500] shadow-sm transition active:scale-[0.98] disabled:opacity-50"
                :disabled="loading || opening" @click="selectRecommendation(recommendation)">
                {{ recommendation.label }}
              </button>
            </div>
          </section>

          <Btn v-if="actionFor(item.action)" class="mt-3" :disabled="opening" @click="openAction(item)">
            {{ actionFor(item.action).label }}
          </Btn>
          <Btn v-if="item.retryable" variant="secondary" class="mt-3" :disabled="loading" @click="resetConversation">
            다시 물어보기
          </Btn>
        </div>
      </div>

      <div v-if="loading" class="flex items-end gap-2.5 pr-8" role="status"
        aria-label="단짝이 답변을 생각하고 있어요">
        <img :src="danjjakee" alt="단짝"
          class="h-10 w-10 flex-shrink-0 rounded-full border border-[#F5D56A] bg-[#FFF3CC] object-contain p-0.5" />
        <div>
          <p class="mb-1.5 ml-1 text-[16px] font-bold text-[#374151]">단짝</p>
          <div class="rounded-[22px] rounded-bl-md border border-[#E5E7EB] bg-white px-5 py-4 shadow-sm">
            <span class="sr-only">생각하고 있어요...</span>
            <span class="flex items-center gap-1.5" aria-hidden="true">
              <span v-for="dot in 3" :key="dot" class="typing-dot h-2.5 w-2.5 rounded-full bg-[#C69200]"
                :style="{ animationDelay: `${(dot - 1) * 160}ms` }" />
            </span>
          </div>
        </div>
      </div>

      <p v-if="actionError" role="alert"
        class="rounded-[16px] bg-[#FEF2F2] px-4 py-3 text-[16px] text-[#B91C1C]">
        {{ actionError }}
      </p>
    </main>

    <form class="flex-shrink-0 border-t border-[#E5E7EB] bg-white px-4 pt-3 shadow-[0_-4px_16px_rgba(17,24,39,0.05)]"
      style="padding-bottom: max(12px, env(safe-area-inset-bottom));" @submit.prevent="send">
      <label for="chat-message" class="mb-2 block text-[15px] text-[#6B7280]">비밀번호와 계좌번호는 입력하지 마세요.</label>
      <div class="flex items-end gap-2 rounded-[22px] border border-[#B8BDC6] bg-[#FAFAF8] p-1.5 pl-4 focus-within:border-[#C69200] focus-within:ring-2 focus-within:ring-[#FFE8A3]">
        <textarea id="chat-message" v-model="draft" rows="1" maxlength="500"
          placeholder="하고 싶은 일을 적어 주세요"
          class="max-h-28 min-h-12 min-w-0 flex-1 resize-none bg-transparent py-2.5 text-[18px] leading-7 outline-none placeholder:text-[#8B919C]"
          :disabled="loading" @keydown.enter.exact.prevent="send" />
        <button type="submit"
          class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-[17px] bg-[#FFBC00] text-[#111827] transition active:scale-95 disabled:bg-[#E5E7EB] disabled:text-[#9CA3AF]"
          :disabled="loading || !draft.trim()" aria-label="메시지 전송">
          <svg viewBox="0 0 24 24" class="h-6 w-6" aria-hidden="true">
            <path d="M5 12h13M13 6l6 6-6 6" fill="none" stroke="currentColor" stroke-width="2.3"
              stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Btn from '../components/common/Btn.vue';
import { useAppStore } from '../stores/appStore.js';
import { sendChatMessage } from '../api/chatApi.js';
import { usageAnalysisApi } from '../api/usageAnalysisApi.js';
import { actionFor, containsSensitiveInfo, openChatAction, sensitiveNotice } from '../features/chat/chatActions.js';
import { discardChatConversation, preserveChatConversation, restoreChatConversation } from '../features/chat/chatConversation.js';
import { recommendationMessages, selectChatRecommendations } from '../features/chat/chatRecommendations.js';
import { smallTalkReply } from '../features/chat/chatSmallTalk.js';
import danjjakee from '../assets/danjjakee.png';

const store = useAppStore();
const draft = ref('');
const initialMessage = Object.freeze({
  id: 1,
  role: 'assistant',
  message: '무엇을 도와드릴까요?\n자주 하시는 일을 아래에서 바로 눌러도 돼요.',
  action: 'NONE',
});
const restoredConversation = restoreChatConversation();
const messages = ref(restoredConversation?.messages ?? [{ ...initialMessage }]);
const recommendations = ref(restoredConversation?.recommendations ?? selectChatRecommendations(null));
const loading = ref(false);
const opening = ref(false);
const actionError = ref('');
const chatArea = ref(null);
let nextMessageId = restoredConversation?.nextMessageId ?? 2;
let controller;
let disposed = false;

function localDate(date) {
  return [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
}

async function loadRecommendations() {
  const to = new Date();
  const from = new Date(to);
  from.setDate(from.getDate() - 6);
  try {
    const report = await usageAnalysisApi.getUsageAnalysis(localDate(from), localDate(to));
    if (!disposed) recommendations.value = selectChatRecommendations(report);
  } catch {
    // 추천을 불러오지 못해도 기본 업무 3개로 채팅을 바로 시작할 수 있다.
  }
}

async function scrollToLatest() {
  await nextTick();
  if (chatArea.value) chatArea.value.scrollTop = chatArea.value.scrollHeight;
}

function addMessage(message) {
  messages.value.push({ id: nextMessageId++, ...message });
}

function selectRecommendation(recommendation) {
  if (loading.value || opening.value) return;
  const copy = recommendationMessages(recommendation);
  addMessage({ role: 'user', message: copy.user, action: 'NONE' });
  addMessage({
    role: 'assistant', message: copy.assistant, action: recommendation.action,
    patternId: recommendation.patternId,
  });
  void scrollToLatest();
}

async function ask(message) {
  loading.value = true;
  controller = new AbortController();
  const timeout = setTimeout(() => controller?.abort(), 20000);
  void scrollToLatest();
  try {
    const reply = await sendChatMessage(message, controller.signal);
    if (!disposed) addMessage({ ...reply, role: 'assistant', retryMessage: reply.retryable ? message : null });
  } catch {
    if (!disposed) {
      const fallback = smallTalkReply(message);
      addMessage(fallback
        ? { role: 'assistant', ...fallback, action: 'NONE' }
        : {
            role: 'assistant', action: 'NONE', retryable: true, retryMessage: message,
            showRecommendations: false,
            message: '지금은 답변을 준비하지 못했어요. 다시 물어보거나 문장을 바꿔 적어 주세요.',
          });
    }
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
    addMessage({ role: 'assistant', message: sensitiveNotice, action: 'NONE', showRecommendations: false });
    void scrollToLatest();
    return;
  }
  addMessage({ role: 'user', message, action: 'NONE' });
  await ask(message);
}

function resetConversation() {
  if (loading.value) return;
  messages.value = [{ ...initialMessage }];
  nextMessageId = 2;
  draft.value = '';
  actionError.value = '';
  void nextTick(() => {
    if (chatArea.value) chatArea.value.scrollTop = 0;
  });
}

async function openAction(item) {
  if (opening.value) return;
  opening.value = true;
  actionError.value = '';
  preserveChatConversation({
    messages: messages.value,
    recommendations: recommendations.value,
    nextMessageId,
  });
  try {
    await openChatAction(item, store);
  } catch {
    discardChatConversation();
    actionError.value = '업무 화면을 열지 못했어요. 버튼을 다시 눌러 주세요.';
  } finally {
    opening.value = false;
  }
}

onMounted(() => {
  if (restoredConversation) void scrollToLatest();
  else void loadRecommendations();
});

onBeforeUnmount(() => {
  disposed = true;
  controller?.abort();
});
</script>

<style scoped>
.typing-dot {
  animation: typing-bounce 1s ease-in-out infinite;
}

@keyframes typing-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.45; }
  30% { transform: translateY(-4px); opacity: 1; }
}
</style>
