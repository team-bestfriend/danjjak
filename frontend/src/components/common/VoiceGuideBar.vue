<template>
  <div
    class="z-20 flex-shrink-0 bg-[#FAFAF8] px-4 pt-3 pb-5"
    aria-label="현재 단계 안내"
  >
    <div
      class="flex min-h-[68px] items-center gap-4 rounded-full bg-white px-4 py-3 shadow-[0_4px_18px_rgba(0,0,0,0.12)]"
      style="border: 1px solid #eeeeed"
    >
      <button
        type="button"
        class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full transition-transform active:scale-95 disabled:opacity-50"
        style="background: #ffbc00"
        :disabled="loading"
        :aria-label="playing ? '음성 안내 일시 정지' : '음성 안내 재생'"
        @click="togglePlay"
      >
        <span style="font-size: 18px; line-height: 1; color: #111827">
          {{ loading ? "…" : playing ? "⏸" : "▶" }}
        </span>
      </button>

      <span id="step-guidance-caption" class="sr-only">
        {{ text }}
      </span>

      <div
        class="flex h-[24px] min-w-0 flex-1 items-center gap-[3px]"
        aria-hidden="true"
      >
        <div
          v-for="(height, index) in WAVE"
          :key="index"
          class="w-[3px] rounded-full"
          :style="{
            height: playing
              ? `${height}px`
              : `${Math.max(4, Math.round(height * 0.55))}px`,
            background: playing ? '#FFBC00' : '#D1D5DB',
            transformOrigin: 'center',
            animation: playing
              ? `voice-wave 0.75s ease-in-out ${(index * 0.048).toFixed(3)}s infinite alternate`
              : 'none',
            transition: 'background 0.4s, height 0.3s',
          }"
        />
      </div>

      <button
        type="button"
        class="flex min-h-12 flex-shrink-0 items-center rounded-full px-4 text-[16px] font-bold text-[#374151] disabled:opacity-50"
        :disabled="loading"
        @click="replay"
      >
        다시 듣기
      </button>
    </div>

    <p
      v-if="fallbackNotice"
      class="px-3 pt-2 text-[13px] text-[#6B7280]"
      role="status"
    >
      {{ fallbackNotice }}
    </p>
    <p
      v-if="errorText"
      class="px-3 pt-2 text-[13px] text-[#B91C1C]"
      role="alert"
    >
      {{ errorText }} 화면 안내와 금융 기능은 계속 사용할 수 있어요.
    </p>
    <p v-if="notice" class="px-3 pt-2 text-[14px] text-[#92650A]" role="status">
      {{ notice }}
    </p>
    <p
      v-if="scriptOutdated && voiceMode === 'FAMILY'"
      class="px-3 pt-2 text-[13px] text-[#92650A]"
    >
      문구 수정 전 녹음이에요. 현재 안내는 화면에서 확인해 주세요.
    </p>
  </div>
</template>

<script setup>
import { useGuidanceAudio } from "../../composables/useGuidanceAudio.js";

// const collapsed = defineModel('collapsed', { type: Boolean, default: false });

const props = defineProps({
  text: { type: String, required: true },
  speed: { type: String, default: "NORMAL" },
  voiceMode: { type: String, default: "TTS" },
  familyAudioUrl: { type: String, default: "" },
  scriptOutdated: Boolean,
  notice: { type: String, default: "" },
  guided: { type: Boolean, default: false },
});

const WAVE = [
  6, 12, 22, 8, 18, 26, 10, 5, 23, 26, 14, 19, 26, 5, 16, 24, 9, 21, 12, 26, 7,
  18, 26, 10, 22, 14, 25, 15, 8, 20,
];
const {
  playing,
  loading,
  error: errorText,
  notice: fallbackNotice,
  toggle: togglePlay,
  replay,
} = useGuidanceAudio(() => props.text, {
  speed: () => props.speed,
  voiceMode: () => props.voiceMode,
  familyAudioUrl: () => props.familyAudioUrl,
});
</script>
