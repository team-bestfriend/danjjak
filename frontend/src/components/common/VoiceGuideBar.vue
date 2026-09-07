<template>
  <div
    class="z-20 flex-shrink-0 overflow-y-auto bg-[#FAFAF8] px-4 py-3"
    :class="collapsed ? 'max-h-[24%]' : 'max-h-[40%]'"
    aria-label="현재 단계 안내"
  >
    <div
      class="overflow-hidden rounded-[22px] bg-white"
      style="box-shadow: 0 -2px 24px rgba(0,0,0,0.08), 0 4px 16px rgba(0,0,0,0.07); border: 1px solid #EEEEED;"
    >
      <div class="sticky top-0 z-10 flex items-center gap-3 bg-white px-4 pb-2 pt-3">
        <button
          type="button"
          class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full transition-transform active:scale-95 disabled:opacity-50"
          style="background: #FFBC00;"
          :disabled="loading"
          :aria-label="playing ? '음성 안내 일시 정지' : '음성 안내 재생'"
          @click="togglePlay"
        >
          <span style="font-size: 17px; line-height: 1; color: #111827;">{{ loading ? '…' : playing ? '⏸' : '▶' }}</span>
        </button>
        <span class="flex-1 text-[15px] font-bold text-[#92650A]">{{ guided ? '지금 할 일' : '음성 안내' }}</span>
        <button type="button" class="min-h-12 rounded-[12px] bg-[#FFF3CC] px-3 text-[15px] font-bold text-[#92650A]" :aria-expanded="!collapsed" @click="collapsed = !collapsed">{{ collapsed ? '펼치기' : '접기' }}</button>
      </div>
      <p
          id="step-guidance-caption"
          role="status"
          aria-live="polite"
          aria-atomic="true"
          class="px-4 pb-3 font-bold leading-snug text-[#111827]"
          style="font-size: 19px; word-break: keep-all; overflow-wrap: anywhere;"
        >“{{ text }}”</p>

      <div v-if="!collapsed" class="flex items-center gap-3 px-4 pb-3">
        <div class="flex h-[26px] flex-1 items-end gap-[2.5px]" aria-hidden="true">
          <div
            v-for="(height, index) in WAVE"
            :key="index"
            class="flex-1 rounded-full"
            :style="{
              height: playing ? `${height}px` : '3px',
              background: playing ? '#FFBC00' : '#E5E7EB',
              transformOrigin: 'bottom center',
              animation: playing ? `voice-wave 0.75s ease-in-out ${(index * 0.048).toFixed(3)}s infinite alternate` : 'none',
              transition: 'background 0.4s, height 0.3s',
            }"
          />
        </div>
        <button
          type="button"
          class="flex min-h-12 flex-shrink-0 items-center gap-1 rounded-full px-3 font-semibold disabled:opacity-50"
          style="font-size: 13px; color: #92650A; background: #FFF3CC; border: 1px solid #FFBC00;"
          :disabled="loading"
          @click="replay"
        >
          <span aria-hidden="true" style="font-size: 15px;">↺</span> 다시 듣기
        </button>
      </div>

      <p v-if="fallbackNotice" class="px-4 pb-3 text-[13px] text-[#6B7280]" role="status">
        {{ fallbackNotice }}
      </p>
      <p v-if="errorText" class="px-4 pb-3 text-[13px] text-[#B91C1C]" role="alert">
        {{ errorText }} 화면 안내와 금융 기능은 계속 사용할 수 있어요.
      </p>
      <p v-if="notice" class="px-4 pb-3 text-[15px] text-[#92650A]" role="status">{{ notice }}</p>
      <p v-if="scriptOutdated && voiceMode === 'FAMILY'" class="px-4 pb-3 text-[14px] text-[#92650A]">문구 수정 전 녹음이에요. 현재 안내는 화면에서 확인해 주세요.</p>
    </div>
  </div>
</template>

<script setup>
import { useGuidanceAudio } from '../../composables/useGuidanceAudio.js';

const collapsed = defineModel('collapsed', { type: Boolean, default: false });

const props = defineProps({
  text: { type: String, required: true },
  speed: { type: String, default: 'NORMAL' },
  voiceMode: { type: String, default: 'TTS' },
  familyAudioUrl: { type: String, default: '' },
  scriptOutdated: Boolean,
  notice: { type: String, default: '' },
  guided: { type: Boolean, default: false },
});

const WAVE = [6, 12, 22, 8, 18, 26, 10, 5, 23, 26, 14, 19, 26, 5, 16, 24, 9, 21, 12, 26, 7, 18, 26, 10, 22, 14, 25, 15, 8, 20];
const { playing, loading, error: errorText, notice: fallbackNotice, toggle: togglePlay, replay } = useGuidanceAudio(
  () => props.text,
  { speed: () => props.speed, voiceMode: () => props.voiceMode, familyAudioUrl: () => props.familyAudioUrl },
);
</script>
