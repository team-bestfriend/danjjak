<template>
  <section class="space-y-5">
    <template v-if="mode === 'select'">
      <h2 class="text-[26px] font-bold text-[#111827]">{{ title }}</h2>
      <p class="text-[16px] text-[#6B7280]">안내 문구와 음성을 확인해 주세요.</p>
      <button type="button" class="flex min-h-[112px] w-full items-center gap-4 rounded-[20px] border-2 border-[#FFBC00] bg-white p-5 text-left" @click="chooseMode('TTS')">
        <span class="flex h-16 w-16 shrink-0 items-center justify-center rounded-[18px] bg-[#EFFCFB]" aria-hidden="true">
          <img :src="robotIcon" alt="" class="h-14 w-14 object-contain" />
        </span>
        <span class="min-w-0"><strong class="block text-[20px]">AI 음성</strong><span class="text-[15px] text-[#6B7280]">안내 문구를 편집하고 미리 들어요.</span></span>
      </button>
      <button type="button" class="flex min-h-[112px] w-full items-center gap-4 rounded-[20px] border border-[#E5E7EB] bg-white p-5 text-left" @click="chooseMode('FAMILY')">
        <span class="flex h-16 w-16 shrink-0 items-center justify-center rounded-[18px] bg-[#FFF7ED]" aria-hidden="true">
          <img :src="micIcon" alt="" class="h-14 w-14 object-contain" />
        </span>
        <span class="min-w-0"><strong class="block text-[20px]">가족 음성</strong><span class="text-[15px] text-[#6B7280]">가족이 읽어 주는 안내를 녹음해요.</span></span>
      </button>
      <Btn variant="secondary" @click="chooseMode(null)">전체 음성 설정 따르기</Btn>
      <p class="text-[15px] text-[#6B7280]">현재 선택: {{ voiceLabel({ voiceMode: selectedMode, audioUrl }, defaultMode) }}</p>
      <Btn variant="secondary" @click="cancel">돌아가기</Btn>
    </template>

    <template v-else>
      <div class="flex items-center justify-between gap-3">
        <h2 class="text-[25px] font-bold text-[#111827]">{{ mode === 'tts' ? 'AI 음성' : '가족 음성 녹음' }}</h2>
        <button type="button" :disabled="busy" class="min-h-12 min-w-16 rounded-[12px] bg-[#FFF3CC] px-3 text-[17px] font-bold disabled:opacity-50" @click="editing = !editing">
          {{ editing ? '완료' : '편집' }}
        </button>
      </div>
      <p class="text-[16px] font-semibold text-[#6B7280]">{{ title }}</p>
      <div v-if="editing" class="space-y-2">
        <label class="block text-[16px] font-bold" :for="inputId">안내 문구</label>
        <textarea :id="inputId" v-model="draft" :disabled="busy" maxlength="500" rows="4" class="w-full resize-none rounded-[18px] border-2 border-[#FFBC00] bg-[#FFFBEB] p-4 text-[18px] leading-relaxed outline-none" :aria-invalid="!valid" />
        <p class="text-right text-[13px] text-[#6B7280]">{{ draft.length }} / 500자</p>
        <button type="button" :disabled="busy || !defaultText" class="min-h-12 w-full text-[15px] text-[#6B7280] disabled:opacity-40" @click="draft = defaultText">초기화 (기본 문구로 되돌리기)</button>
      </div>
      <p v-if="!valid" class="text-[#B91C1C]" role="alert">안내 문구를 1~500자로 입력해 주세요.</p>
      <div class="space-y-4 rounded-[22px] border-2 border-[#FFBC00] bg-white p-5">
        <p class="text-[15px] font-bold text-[#92650A]">{{ mode === 'tts' ? '재생될 문구' : '가족이 읽을 문구' }}</p>
        <p class="whitespace-pre-wrap break-words text-[18px] leading-relaxed text-[#111827]">“{{ draft }}”</p>
        <button type="button" :disabled="!valid || busy || loading" class="flex min-h-12 items-center gap-3 text-[17px] font-bold text-[#2563EB] disabled:opacity-40" @click="preview">
          <span class="flex h-12 w-12 items-center justify-center rounded-full bg-[#DBEAFE]" aria-hidden="true">{{ playing ? '⏸' : '▶' }}</span>
          {{ loading ? '음성을 불러오는 중…' : playing ? '일시 정지' : mode === 'family' ? '안내 문구를 AI 음성으로 미리 듣기' : '미리 듣기' }}
        </button>
        <p v-if="error" class="text-[15px] text-[#B91C1C]" role="alert">{{ error }} 문구는 계속 편집하고 저장할 수 있어요.</p>
      </div>
      <template v-if="mode === 'family'">
        <p class="text-[15px] text-[#6B7280]">위 문구를 가족이 읽어 주세요. 녹음 시작을 누르면 마이크 권한을 요청해요. 최대 90초이며, 저장을 눌러야 서버에 올라가요.</p>
        <Btn v-if="recordState === 'recording'" variant="secondary" @click="stopRecording">녹음 마치기 · {{ seconds }}초</Btn>
        <Btn v-else variant="secondary" :disabled="!valid || busy" @click="beginRecording">{{ recordState === 'requesting' ? '마이크 권한 확인 중…' : recordState === 'stopping' ? '녹음 정리 중…' : recording || audioUrl ? '다시 녹음' : '녹음 시작' }}</Btn>
        <p v-if="recordError" class="text-[15px] text-[#B91C1C]" role="alert">{{ recordError }}</p>
        <template v-if="previewUrl">
          <p class="text-[15px] font-bold">{{ recording ? '새 녹음 미리듣기 · 아직 저장 전' : '저장된 가족 음성' }}</p>
          <audio ref="recordedAudio" :src="previewUrl" controls class="h-14 w-full" @play="stopPreview" @error="playbackError = '녹음을 재생하지 못했어요. 다시 녹음하거나 AI 음성을 선택해 주세요.'" />
          <p v-if="playbackError" class="text-[15px] text-[#B91C1C]" role="alert">{{ playbackError }}</p>
        </template>
        <Btn v-if="recording" variant="secondary" :disabled="busy" @click="recording = null">새 녹음 취소 · 기존 녹음 유지</Btn>
        <p v-if="!recording && !audioUrl" class="text-[15px] text-[#6B7280]">녹음 없이 저장하면 같은 문구를 AI 음성으로 안내해요.</p>
      </template>
      <p v-if="mismatch" class="rounded-[16px] bg-[#FFF3CC] p-4 text-[15px] text-[#92650A]" role="status">문구가 바뀌었지만 녹음은 바뀌지 않았어요. 다시 녹음하거나 방식을 AI 음성으로 바꿀 수 있어요. 저장된 이전 녹음을 그대로 사용하는 것도 가능해요.</p>
      <p v-if="invalidRecording" class="text-[15px] text-[#B91C1C]" role="alert">새 녹음 후 문구가 바뀌었어요. 다시 녹음하거나 새 녹음을 취소한 뒤 저장해 주세요.</p>
      <p v-if="!selectedMode" class="text-[14px] text-[#6B7280]">전체 음성 설정을 따라요.</p>
      <p v-if="dirty" class="text-[14px] text-[#92650A]" role="status">아직 저장하지 않은 변경이 있어요.</p>
      <Btn :disabled="!valid || busy || invalidRecording" @click="confirm">{{ saving ? '저장 중…' : actionLabel }}</Btn>
      <Btn variant="secondary" :disabled="busy" @click="changeMode">방식 변경</Btn>
      <Btn variant="secondary" :disabled="saving" @click="cancel">취소</Btn>
    </template>
    <DiscardChangesDialog v-if="discardDialog" v-bind="discardDialog" @resolve="resolveDiscard" />
  </section>
</template>

<script setup>
import { computed, onUnmounted, ref, useId, watch } from 'vue';
import Btn from './Btn.vue';
import DiscardChangesDialog from './DiscardChangesDialog.vue';
import { useDiscardConfirmation } from '../../composables/useDiscardConfirmation.js';
import { useTtsAudio } from '../../composables/useTtsAudio.js';
import { useFamilyRecorder } from '../../composables/useFamilyRecorder.js';
import { voiceLabel } from '../../api/guidanceApi.js';
import robotIcon from '../../assets/icons/robot.png';
import micIcon from '../../assets/icons/mic.png';

const props = defineProps({
  title: { type: String, required: true },
  text: { type: String, default: '' },
  defaultText: { type: String, default: '' },
  speed: { type: String, default: 'NORMAL' },
  saving: Boolean,
  voiceMode: { type: String, default: null },
  defaultMode: { type: String, default: 'TTS' },
  audioUrl: { type: String, default: null },
  voiceScriptOutdated: Boolean,
  recordingDraft: { type: Object, default: null },
  openFamily: Boolean,
  actionLabel: { type: String, default: '이 음성 저장' },
});
const emit = defineEmits(['confirm', 'cancel', 'dirty']);
const { discardDialog, confirmDiscard, resolveDiscard } = useDiscardConfirmation();
const inputId = useId();
const mode = ref(props.openFamily ? 'family' : 'select');
const editing = ref(false);
const draft = ref(props.text);
const selectedMode = ref(props.openFamily ? 'FAMILY' : props.voiceMode);
const recording = ref(props.recordingDraft);
const recordedAudio = ref(null);
const playbackError = ref('');
const localUrl = ref('');
const previewUrl = computed(() => localUrl.value || props.audioUrl);
const { state: recordState, seconds, error: recordError, start: startRecording, stop: stopRecording } = useFamilyRecorder((value) => { recording.value = value; });
const busy = computed(() => props.saving || recordState.value !== 'idle');
const dirty = computed(() => draft.value !== props.text || selectedMode.value !== props.voiceMode || recording.value !== props.recordingDraft || recordState.value !== 'idle');
const invalidRecording = computed(() => Boolean(recording.value && recording.value.text !== draft.value.trim()));
const mismatch = computed(() => invalidRecording.value || (!recording.value && props.audioUrl && (props.voiceScriptOutdated || draft.value !== props.text)));
const valid = computed(() => draft.value.trim().length > 0 && draft.value.length <= 500);
const previewEnabled = ref(false);
// 입력할 때마다 TTS를 생성하지 않고 미리듣기를 누른 문구만 요청한다.
const { playing, loading, error, toggle, cleanup } = useTtsAudio(
  () => draft.value.trim(),
  { speed: () => props.speed, autoplay: true, enabled: previewEnabled },
);
watch(dirty, (value) => emit('dirty', value), { immediate: true });
watch(draft, () => { stopPreview(); recordedAudio.value?.pause(); });
watch(recording, (value) => {
  if (localUrl.value) URL.revokeObjectURL(localUrl.value);
  localUrl.value = value ? URL.createObjectURL(value.blob) : '';
  playbackError.value = '';
}, { immediate: true });
onUnmounted(() => { if (localUrl.value) URL.revokeObjectURL(localUrl.value); });

function stopPreview() { previewEnabled.value = false; cleanup(); }
function chooseMode(value) {
  selectedMode.value = value;
  mode.value = (value ?? props.defaultMode) === 'FAMILY' ? 'family' : 'tts';
}
function beginRecording() {
  stopPreview();
  recordedAudio.value?.pause();
  void startRecording(draft.value.trim());
}

function preview() {
  if (!valid.value) return;
  recordedAudio.value?.pause();
  if (!previewEnabled.value) {
    previewEnabled.value = true;
    // enabled 감시자가 최초 요청을 시작하므로 같은 클릭에서 중복 요청하지 않는다.
    return;
  }
  void toggle();
}

function changeMode() {
  cleanup();
  previewEnabled.value = false;
  mode.value = 'select';
  editing.value = false;
  recordedAudio.value?.pause();
}

function confirm() {
  if (!valid.value || busy.value || invalidRecording.value) return;
  cleanup();
  previewEnabled.value = false;
  recordedAudio.value?.pause();
  emit('confirm', { text: draft.value.trim(), voiceMode: selectedMode.value, recording: recording.value });
}

async function cancel() {
  if (props.saving) return;
  if (dirty.value && !await confirmDiscard({
    title: '편집을 그만둘까요?',
    description: '저장하지 않은 문구와 녹음은 사라져요.',
    confirmLabel: '돌아가기',
  })) return;
  cleanup();
  emit('dirty', false);
  emit('cancel');
}
</script>
