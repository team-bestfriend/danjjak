<template>
  <div
    className="absolute bottom-0 left-0 right-0 z-20 px-4 pb-4 pt-6"
    style="background: linear-gradient(to bottom, transparent 0%, rgba(250,250,248,0.92) 28%, #FAFAF8 52%);"
  >
    <div
      className="rounded-[22px] bg-white overflow-hidden"
      style="box-shadow: 0 -2px 24px rgba(0,0,0,0.08), 0 4px 16px rgba(0,0,0,0.07); border: 1px solid #EEEEED;"
    >
      <!-- 상단: 재생 버튼 + 텍스트 -->
      <div className="flex items-center gap-3 px-4 pt-4 pb-2">
        <button
          @click="togglePlay"
          :disabled="loading"
          :aria-label="playing ? '음성 안내 일시정지' : '음성 안내 재생'"
          className="flex-shrink-0 w-11 h-11 rounded-full flex items-center justify-center active:scale-95 transition-transform"
          style="background: #FFBC00;"
        >
          <span style="font-size: 17px; line-height: 1; color: #111827;">{{ loading ? '…' : playing ? '⏸' : '▶' }}</span>
        </button>
        <p
          className="font-bold text-[#111827] flex-1 leading-snug"
          style="font-size: 15px; word-break: keep-all;"
        >"{{ text }}"</p>
      </div>

      <!-- 하단: 파형 + 다시 듣기 -->
      <div className="flex items-center gap-3 px-4 pb-3">
        <!-- 파형 바 -->
        <div className="flex-1 flex items-end gap-[2.5px]" style="height: 26px;">
          <div
            v-for="(h, i) in WAVE"
            :key="i"
            className="flex-1 rounded-full"
            :style="{
              height: playing ? `${h}px` : '3px',
              background: playing ? '#FFBC00' : '#E5E7EB',
              transformOrigin: 'bottom center',
              animation: playing ? `voice-wave 0.75s ease-in-out ${(i * 0.048).toFixed(3)}s infinite alternate` : 'none',
              transition: 'background 0.4s, height 0.3s',
            }"
          />
        </div>

        <!-- 다시 듣기 버튼 -->
        <button
          @click="replay"
          :disabled="loading"
          className="flex-shrink-0 flex items-center gap-1 font-semibold rounded-full px-3 py-1.5"
          style="font-size: 13px; color: #92650A; background: #FFF3CC; border: 1px solid #FFBC00;"
        >
          <span style="font-size: 15px;">↺</span> 다시 듣기
        </button>
      </div>
      <p v-if="error" className="px-4 pb-3 text-sm font-semibold text-red-600" role="alert">
        {{ error }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { toRef } from 'vue';
import { useTtsAudio } from '../../composables/useTtsAudio';

const props = defineProps({
  text: { type: String, required: true }
});

// 파형 높이 (픽셀, 최대 26px)
const WAVE = [6,12,22,8,18,26,10,5,23,26,14,19,26,5,16,24,9,21,12,26,7,18,26,10,22,14,25,15,8,20];

const { playing, loading, error, toggle: togglePlay, replay } = useTtsAudio(toRef(props, 'text'));
</script>
