<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />
    <header class="flex flex-shrink-0 flex-wrap items-center gap-x-3 border-b border-[#EEEEED] bg-white px-4 py-3">
      <button type="button" class="min-h-12 rounded-xl px-2 text-[18px] font-semibold text-[#374151] active:bg-[#F3F4F6]" @click="goBack">‹ 뒤로</button>
      <h1 class="text-[22px] font-bold text-[#111827]">서비스 이용방법</h1>
    </header>

    <main class="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 pb-6 pt-5">
      <div class="pb-2">
        <h2 class="text-[24px] font-bold text-[#111827]">단짝, 이렇게 이용해요</h2>
        <p class="mt-2 text-[18px] leading-relaxed text-[#6B7280]">궁금한 항목을 눌러 사용법을 확인해 보세요.</p>
      </div>

      <details v-for="section in sections" :key="section.id" class="guide-section rounded-[18px] border border-[#E5E7EB] bg-white">
        <summary class="flex min-h-16 cursor-pointer list-none items-center justify-between gap-3 rounded-[18px] p-5 text-[20px] font-semibold text-[#111827] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#B8860B]">
          <span>{{ section.title }}</span>
          <span class="guide-chevron shrink-0 text-[#92650A]" aria-hidden="true">⌄</span>
        </summary>
        <div class="space-y-4 border-t border-[#F3F4F6] p-5 text-[20px] leading-relaxed text-[#374151]">
          <ol class="list-decimal space-y-3 pl-6">
            <li v-for="step in section.steps" :key="step">{{ step }}</li>
          </ol>
          <template v-if="section.id === 'voice'">
            <div class="rounded-[14px] bg-[#FFFBEB] p-4">
              <h3 class="font-semibold text-[#92650A]">이렇게 말해 보세요</h3>
              <ul class="mt-3 space-y-2">
                <li v-for="example in voiceCommandExamples" :key="example">“{{ example }}”</li>
              </ul>
            </div>
            <p>말한 업무가 단축번호에 등록되어 있어야 찾을 수 있어요. 잘 알아듣지 못하면 다시 말하거나 화면에서 직접 골라 주세요.</p>
            <p class="text-[18px] text-[#6B7280]">말로 고르는 동안 마이크를 사용해요. 단짝은 말한 내용과 음성을 저장하지 않아요. 다만, 음성을 알아듣는 브라우저 서비스로 소리가 전송될 수 있어요.</p>
          </template>
          <p v-if="section.note" class="rounded-[14px] bg-[#FFFBEB] p-4 text-[18px] text-[#92650A]">{{ section.note }}</p>
        </div>
      </details>
    </main>

    <NavBar active="settings" :onSelect="store.navTo" />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import NavBar from '../components/common/NavBar.vue';
import { voiceCommandExamples } from '../features/voice/shortcutCommands.js';

const store = useAppStore();
const router = useRouter();

function goBack() {
  if (router.options.history.state.back) store.goBack();
  else void router.replace({ name: 'settings' });
}

const sections = [
  {
    id: 'shortcuts',
    title: '단축번호로 시작하기',
    steps: [
      '홈의 ‘내 단축번호’에서 원하는 업무를 눌러요.',
      '옆으로 밀거나 아래의 점을 누르면 다른 번호를 볼 수 있어요. 번호를 꾹 누른 뒤 옮기면 순서를 바꿀 수 있어요.',
      '업무 내용을 확인한 뒤 ‘시작하기’를 눌러요.',
      '아래의 ‘패턴’ 메뉴에서 자주 하는 업무를 등록하거나 수정할 수 있어요.',
    ],
  },
  {
    id: 'voice',
    title: '말로 선택하기',
    steps: [
      '홈에서 ‘말로 선택하기’를 눌러요. 마이크 사용을 물어보면 허용해 주세요.',
      '‘아들에게 돈 보내 줘’처럼 원하는 업무를 말해요. 말을 마치면 ‘말하기 완료’를 눌러도 돼요.',
      '찾은 업무의 ‘업무 확인하기’를 눌러 내용을 살펴봐요. 원하는 업무가 맞으면 ‘시작하기’를 눌러요.',
    ],
    note: '말로 선택하는 것만으로 돈이 보내지지는 않아요. 보내는 내용과 금액은 화면에서 확인해요.',
  },
  {
    id: 'transfer',
    title: '직접 돈 보내기',
    steps: [
      '홈에서 ‘직접 송금하기’를 눌러요.',
      '돈이 빠져나갈 내 계좌와 돈을 받을 사람·계좌를 골라요.',
      '보낼 금액을 입력하고, 받는 사람과 계좌·금액이 맞는지 확인해요.',
      '화면 안내에 따라 본인 확인을 마치고 송금 결과를 확인해요.',
    ],
    note: '설정의 ‘사람 및 계좌 관리’에서 자주 돈을 보낼 사람과 계좌를 등록할 수 있어요.',
  },
  {
    id: 'family',
    title: '가족 목소리로 안내 듣기',
    steps: [
      '‘패턴’ 메뉴에서 업무를 고르고 수정 화면을 열어요.',
      '업무 설명이나 단계별 안내 문구를 바꾸고, 가족 목소리를 녹음해 저장할 수 있어요.',
      '설정에서 안내 속도와 기본 음성을 고른 뒤 ‘화면·음성 설정 저장’을 눌러요.',
    ],
    note: 'AI 음성은 컴퓨터가 만들어 읽어 주는 목소리예요. 가족 녹음이 없거나 들을 수 없으면 AI 음성으로 안내해요.',
  },
  {
    id: 'analysis',
    title: '이용 기록 살펴보기',
    steps: [
      '아래의 ‘이용 분석’을 누르면 자주 이용한 업무와 안내를 살펴볼 단계를 확인할 수 있어요.',
      '어려웠던 단계의 안내 문구나 가족 녹음을 바꿔 더 쉽게 이용해 보세요.',
    ],
    note: '이용 기록 수집에 동의한 경우에 기록을 모아요. 설정의 ‘선택 동의 관리’에서 동의를 바꿀 수 있어요.',
  },
  {
    id: 'settings',
    title: '글씨와 안내 속도 바꾸기',
    steps: [
      '아래의 ‘설정’을 눌러요.',
      '편하게 읽을 수 있는 글씨 크기와 듣기 좋은 안내 속도를 골라요.',
      '미리 보거나 들어 본 뒤 ‘화면·음성 설정 저장’을 눌러요.',
    ],
  },
  {
    id: 'demo',
    title: '금융 업무와 보호자 도움',
    steps: [
      '단짝에서 계좌·잔액·거래를 확인하고 송금 흐름을 진행할 수 있어요.',
      '큰 금액을 보내거나 짧은 시간에 여러 번 보내면 한 번 더 확인하도록 안내해요. 내용을 살펴보고 계속 보낼지 멈출지 골라요.',
      '보호자에게 전화하려면 설정에 전화번호를 저장해 주세요. 전화 연결을 선택하면 전화 앱으로 이동해요.',
    ],
    note: '‘보호자에게 카톡 보내기’는 시연 기능이에요. 실제 전송 시 보호자가 아닌 로그인한 본인의 카카오톡 ‘나에게 보내기’로 알림이 가요. 실제로 전송하지 않은 경우에는 화면에 시연 결과를 따로 알려드려요.',
  },
];
</script>

<style scoped>
.guide-section summary::-webkit-details-marker {
  display: none;
}

.guide-section[open] .guide-chevron {
  transform: rotate(180deg);
}
</style>
