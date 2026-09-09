<template>
  <div
    class="mx-5 flex max-h-[calc(100%-40px)] w-full max-w-[340px] flex-col overflow-hidden rounded-[28px] bg-white"
    style="box-shadow: 0 8px 40px rgba(0,0,0,0.22);"
    @click.stop
  >
    <div class="min-h-0 flex-1 overflow-y-auto">
    <div class="flex flex-col items-center justify-center gap-2 px-8 pb-8 pt-10" style="background: #FFBC00;">
      <span class="font-black leading-none text-[#111827]" style="font-size: 76px;">{{ pat.num }}</span>
      <span class="text-center font-black text-[#111827]" style="font-size: 22px; margin-top: 4px;">{{ pat.label }}</span>
      <span v-if="person" class="text-[15px] font-bold" style="color: rgba(0,0,0,0.55);">
        {{ person.emoji }} {{ person.name }} · {{ person.relation }}
      </span>
      <span v-if="linkedAccountLabel" class="text-center text-[14px] font-bold" style="color: rgba(0,0,0,0.55);">
        {{ linkedAccountLabel }}
      </span>
    </div>

    <div class="space-y-4 p-5">
      <div>
        <p class="mb-2 text-[13px] font-bold uppercase tracking-wide text-[#9CA3AF]">업무 안내 · {{ voiceMode === 'FAMILY' ? '가족 음성' : '자동 TTS' }}</p>
        <div class="flex items-start gap-3">
          <button
            type="button"
            class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full text-[14px] font-bold disabled:opacity-50"
            style="background: #FFF3CC; color: #92650A;"
            :disabled="loading"
            :aria-label="playing ? '업무 안내 일시 정지' : '업무 안내 재생'"
            @click="toggle"
          >
            {{ loading ? '…' : playing ? '⏸' : '▶' }}
          </button>
          <div class="flex-1">
            <p class="mb-1.5 text-[18px] font-bold leading-relaxed text-[#111827]" style="word-break: keep-all; overflow-wrap: anywhere;">“{{ quote }}”</p>
            <div class="flex h-5 items-end gap-px" aria-hidden="true">
              <div
                v-for="(_, index) in 28"
                :key="index"
                class="flex-1 rounded-sm"
                :style="{
                  height: `${Math.max(3, Math.abs(Math.sin(index * 0.9)) * 12 + 3)}px`,
                  backgroundColor: playing ? '#FFBC00' : '#F3F4F6',
                }"
              />
            </div>
          </div>
        </div>
        <p v-if="notice" class="mt-2 text-[13px] text-[#6B7280]" role="status">
          {{ notice }}
        </p>
        <p v-if="pat.guidance?.voiceScriptOutdated && voiceMode === 'FAMILY'" class="mt-2 text-[14px] text-[#92650A]">문구 수정 전 녹음이에요. 현재 안내는 화면에서 확인해 주세요.</p>
        <p v-if="error" class="mt-2 text-[13px] text-[#B91C1C]" role="alert">
          {{ error }} 화면의 업무 설명은 계속 확인할 수 있어요.
        </p>
      </div>

    </div>
    </div>
      <div class="flex flex-shrink-0 gap-2 border-t border-[#EEEEED] bg-white p-5">
        <button
          type="button"
          class="h-14 flex-1 rounded-[14px] border-2 border-[#E5E7EB] font-bold text-[#374151]"
          style="font-size: 17px;"
          @click="$emit('cancel')"
        >취소하기</button>
        <button
          type="button"
          class="h-14 flex-1 rounded-[14px] font-black text-[#111827]"
          style="font-size: 17px; background: #FFBC00;"
          @click="$emit('start')"
        >시작하기</button>
      </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useGuidanceAudio } from '../../composables/useGuidanceAudio.js';
import { generatePatternDesc } from '../../constants/data.js';
import { useAppStore } from '../../stores/appStore.js';

const props = defineProps({ pat: { type: Object, required: true } });
defineEmits(['cancel', 'start']);
const store = useAppStore();

const person = computed(() => {
  if (!props.pat.personId) return null;
  return store.people.find((item) => item.id === props.pat.personId) ?? {
    name: props.pat.linkedAccount?.registeredPersonName,
    relation: props.pat.linkedAccount?.relationship,
    emoji: props.pat.linkedAccount?.relationship === '아들' ? '👨' : '👩',
  };
});
const linkedAccountLabel = computed(() => {
  if (props.pat.taskType !== 'transfer') return '';
  return [
    props.pat.linkedAccount?.bankName,
    props.pat.linkedAccount?.accountAlias,
    props.pat.linkedAccount?.masked,
  ].filter(Boolean).join(' · ');
});
const quote = computed(() => (
  props.pat.description || generatePatternDesc(props.pat, store.people, store.accountsByPerson)
));
const speed = computed(() => store.currentUser?.settings?.voiceSpeed ?? 'NORMAL');
const voiceMode = computed(() => props.pat.guidance?.voiceMode ?? store.currentUser?.settings?.guideVoiceType ?? 'TTS');
const { playing, loading, error, notice, toggle } = useGuidanceAudio(quote, {
  speed, voiceMode, familyAudioUrl: () => props.pat.guidance?.audioUrl,
});
</script>
