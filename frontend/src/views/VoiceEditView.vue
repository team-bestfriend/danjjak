<template>
  <!-- TTS Mode -->
  <div v-if="mode === 'tts'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="TTS 자동 음성" :onBack="() => { mode = 'select'; ttsEditing = false; }" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <!-- 헤더 + 편집 버튼 -->
      <div className="flex items-center justify-between">
        <p className="font-bold text-[#111827]" style="font-size: 22px;">재생될 문구</p>
        <button
          @click="ttsEditing = !ttsEditing; if(ttsEditing && !customTtsText) customTtsText = descText"
          className="font-semibold rounded-[10px] px-3 py-1.5"
          :style="ttsEditing ? 'background:#FFBC00;color:#111827;font-size:17px;' : 'background:#F3F4F6;color:#374151;font-size:17px;'"
        >{{ ttsEditing ? '완료' : '편집' }}</button>
      </div>
      <template v-if="ttsEditing">
        <textarea
          v-model="customTtsText"
          :placeholder="descText"
          rows="3"
          className="w-full rounded-[14px] border-2 border-[#FFBC00] outline-none px-4 py-3 resize-none"
          style="font-size: 15px; font-weight: 500; color: #111827; background: #FFFBEB; line-height: 1.5;"
        />
        <button @click="customTtsText = ''; ttsEditing = false" className="w-full py-2 font-medium text-[#9CA3AF]" style="font-size: 14px;">초기화 (기본 문구로 되돌리기)</button>
      </template>
      <Card className="p-5 border-2 border-[#FFBC00]">
        <p className="font-black text-[#92650A] mb-2" style="font-size: 13px;">패턴 설명 문구</p>
        <p
          className="mb-4 leading-snug"
          :style="customTtsText ? 'font-size:16px;font-weight:700;color:#111827;' : 'font-size:15px;font-weight:400;color:#9CA3AF;'"
        >"{{ customTtsText || descText }}"</p>
        <button @click="playing = !playing" className="flex items-center gap-2 font-bold text-[#2563EB]" style="font-size: 15px;">
          <div className="w-10 h-10 rounded-full bg-[#DBEAFE] flex items-center justify-center">{{ playing ? '⏸' : '▶' }}</div>
          미리 듣기
        </button>
      </Card>
      <Btn @click="ttsEditing = false; store.goBack()">이 음성으로 저장</Btn>
      <Btn variant="secondary" @click="mode = 'select'; ttsEditing = false;">취소</Btn>
    </div>
  </div>

  <!-- Family Mode -->
  <div v-else-if="mode === 'family'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="가족 음성 녹음" :onBack="() => { mode = 'select'; recState = 'idle'; ttsEditing = false; }" />
    <div className="flex-1 overflow-y-auto px-4 pt-5 pb-4 space-y-4">
      <!-- 헤더 + 편집 버튼 -->
      <div className="flex items-center justify-between">
        <p className="font-bold text-[#111827]" style="font-size: 22px;">가족 음성 녹음</p>
        <button
          @click="ttsEditing = !ttsEditing; if(ttsEditing && !customTtsText) customTtsText = descText"
          className="font-semibold rounded-[10px] px-3 py-1.5"
          :style="ttsEditing ? 'background:#FFBC00;color:#111827;font-size:17px;' : 'background:#F3F4F6;color:#374151;font-size:17px;'"
        >{{ ttsEditing ? '완료' : '편집' }}</button>
      </div>
      <template v-if="ttsEditing">
        <textarea
          v-model="customTtsText"
          :placeholder="descText"
          rows="3"
          className="w-full rounded-[14px] border-2 border-[#FFBC00] outline-none px-4 py-3 resize-none"
          style="font-size: 15px; font-weight: 500; color: #111827; background: #FFFBEB; line-height: 1.5;"
        />
        <button @click="customTtsText = ''; ttsEditing = false" className="w-full py-2 font-medium text-[#9CA3AF]" style="font-size: 14px;">초기화 (기본 문구로 되돌리기)</button>
      </template>
      <div className="rounded-[18px] border border-[#FFBC00] px-4 py-4" style="background: #FFFBEB;">
        <p className="font-bold text-[#92650A] mb-2" style="font-size: 13px;">📢 다음을 읽어주세요</p>
        <p className="font-bold text-[#111827] leading-snug" style="font-size: 17px; word-break: keep-all;">"{{ customTtsText || descText }}"</p>
      </div>

      <button
        v-if="recState === 'idle'"
        @click="recState = 'recording'"
        className="w-full rounded-[18px] bg-[#374151] text-white font-bold flex items-center justify-center gap-2"
        style="min-height: 64px; font-size: 18px;"
      >
        <Ic name="Mic" />녹음 시작
      </button>

      <div v-else-if="recState === 'recording'" className="space-y-3">
        <div className="w-full rounded-[18px] bg-[#EF4444] text-white flex items-center justify-center gap-3" style="min-height: 64px;">
          <div className="w-3 h-3 rounded-full bg-white animate-pulse" />
          <span className="font-bold" style="font-size: 18px;">녹음 중... {{ recSec }}초</span>
        </div>
        <div className="flex items-end gap-px h-7 px-2">
          <div
            v-for="(_, i) in 32"
            :key="i"
            className="wave-bar flex-1 bg-[#EF4444] rounded-sm opacity-70"
            :style="{ animationDelay: `${i * 0.06}s` }"
          />
        </div>
      </div>

      <div v-else-if="recState === 'done'" className="space-y-3">
        <div className="bg-[#F0FDF4] border border-[#86EFAC] rounded-[16px] p-4 flex items-center gap-3">
          <span style="font-size: 22px;">✅</span>
          <div>
            <p className="font-semibold text-[#15803D]" style="font-size: 16px;">녹음이 완료됐어요.</p>
            <p className="text-[#16A34A] mt-0.5" style="font-size: 13px;">00:0{{ recSec }} 녹음됨</p>
          </div>
        </div>
        <div className="flex gap-2">
          <button className="flex-1 border border-[#EBEBEA] rounded-[14px] font-bold text-[#2563EB]" style="height: 52px; font-size: 15px;">▶ 미리 듣기</button>
          <button @click="recState = 'idle'; recSec = 0;" className="flex-1 border border-[#EBEBEA] rounded-[14px] font-bold text-[#6B7280]" style="height: 52px; font-size: 15px;">다시 녹음</button>
        </div>
        <Btn @click="store.goBack">저장하기</Btn>
      </div>

      <Btn variant="secondary" @click="mode = 'select'; recState = 'idle';" className="mt-2">
        방식 변경
      </Btn>
    </div>
  </div>

  <!-- Select Mode -->
  <div v-else className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="패턴 설명 음성 수정" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-5 pb-4 space-y-4">
      <p className="font-bold text-[#111827]" style="font-size: 26px;">음성 방식 선택</p>
      <button
        @click="mode = 'tts'"
        className="w-full rounded-[20px] bg-white border border-[#E5E7EB] p-5 flex items-center gap-4 active:scale-[0.98] transition-all"
      >
        <div className="w-14 h-14 rounded-full bg-[#FFF3CC] border-2 border-[#FFBC00] flex items-center justify-center flex-shrink-0" style="font-size: 28px;">🤖</div>
        <div className="text-left">
          <p className="font-semibold text-[#111827]" style="font-size: 20px;">TTS 자동 음성</p>
          <p className="text-[#6B7280] mt-0.5" style="font-size: 14px;">등록 정보로 자동 생성된 음성을 사용해요.</p>
        </div>
      </button>
      <button
        @click="mode = 'family'; recState = 'idle'; recSec = 0;"
        className="w-full rounded-[20px] bg-white border border-[#E5E7EB] p-5 flex items-center gap-4 active:scale-[0.98] transition-all"
      >
        <div className="w-14 h-14 rounded-full bg-[#F3F4F6] border-2 border-[#E5E7EB] flex items-center justify-center flex-shrink-0" style="font-size: 28px;">🎙️</div>
        <div className="text-left">
          <p className="font-semibold text-[#111827]" style="font-size: 20px;">가족 음성 녹음</p>
          <p className="text-[#6B7280] mt-0.5" style="font-size: 14px;">가족이 직접 음성을 녹음해요.</p>
        </div>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import { generatePatternDesc } from '../constants/data';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Card from '../components/common/Card.vue';
import Btn from '../components/common/Btn.vue';
import Ic from '../components/common/Ic.vue';

const store = useAppStore();

const mode = ref("select");
const recState = ref("idle");
const recSec = ref(0);
const playing = ref(false);
const customTtsText = ref(store.activePattern?.customTtsText || "");
const ttsEditing = ref(false);
let timer = null;

const descText = computed(() => {
  return store.activePattern ? generatePatternDesc(store.activePattern, store.people, store.accountsByPerson) : "패턴 설명 음성입니다.";
});

watch(recState, (newVal) => {
  if (timer) clearInterval(timer);
  if (newVal === 'recording') {
    recSec.value = 0;
    timer = setInterval(() => { recSec.value++; }, 1000);
  }
});

watch(recSec, (newSec) => {
  if (recState.value === 'recording' && newSec >= 4) {
    if (timer) clearInterval(timer);
    recState.value = 'done';
  }
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>
