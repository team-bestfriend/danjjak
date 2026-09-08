<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />
    <TopBar title="안내 문구 쉽게 바꾸기" :on-back="back" :back-disabled="saving" />
    <main class="min-h-0 flex-1 space-y-5 overflow-y-auto px-5 py-6">
      <p class="text-[15px] text-[#6B7280]">{{ period.from }} ~ {{ period.to }} 이용 기록 기준</p>
      <template v-if="loading || loadError || suggestion">
        <div v-if="suggestion">
          <h1 class="text-[25px] font-bold text-[#111827]">{{ suggestion.stepName }}</h1>
          <p class="mt-2 text-[16px] text-[#6B7280]">{{ suggestion.patternTitle }} · {{ suggestion.stepOrder }}단계</p>
        </div>
        <Card class="space-y-3 p-5">
          <h2 class="text-[17px] font-bold">현재 저장된 문구</h2>
          <p v-if="suggestion" class="break-words text-[19px] leading-relaxed">{{ suggestion.currentText }}</p>
          <div v-else class="h-14" aria-hidden="true"></div>
        </Card>
        <Card class="space-y-3 border-2 border-[#FFBC00] bg-[#FFFBEB] p-5">
          <h2 class="text-[17px] font-bold text-[#92650A]">더 쉽게 설명한 제안</h2>
          <div v-if="loading" class="flex min-h-14 items-center gap-3 text-[17px] text-[#6B7280]" role="status">
            <span class="h-5 w-5 shrink-0 animate-spin rounded-full border-2 border-[#F3D582] border-t-[#92650A]" aria-hidden="true"></span>
            <span>더 쉬운 말을 생각하고 있어요...</span>
          </div>
          <div v-else-if="loadError" class="space-y-4">
            <p class="text-[17px] text-[#B91C1C]" role="alert">{{ loadError }}</p>
            <Btn variant="secondary" @click="load">다시 불러오기</Btn>
          </div>
          <p v-else class="break-words text-[19px] leading-relaxed">{{ suggestion.suggestedText }}</p>
        </Card>
        <template v-if="suggestion">
          <p class="text-[15px] text-[#6B7280]">단계에 맞춰 준비한 문구예요. 비교한 뒤 적용을 눌러야 다음 안내가 바뀌어요.</p>
          <p v-if="suggestion.hasFamilyAudio" class="rounded-[16px] bg-[#FFF3CC] p-4 text-[16px] text-[#92650A]">저장된 가족 녹음은 그대로예요. 문구를 바꾼 뒤 새 대본으로 다시 녹음할 수 있어요.</p>
          <p v-if="saveError" class="text-[#B91C1C]" role="alert">{{ saveError }}</p>
          <Btn v-if="stale" variant="secondary" :disabled="saving || loading" @click="load">바뀐 문구 다시 비교하기</Btn>
          <Btn :disabled="loading || loadError || saving || applied || sameText || stale" @click="apply">{{ saving ? '적용 중…' : applied ? '적용 완료' : sameText ? '이미 같은 문구예요' : '이 제안 문구 적용하기' }}</Btn>
          <p v-if="applied" class="text-[16px] text-[#166534]" role="status">저장했어요. 다음 안내부터 이 문구를 사용해요.</p>
          <p v-if="applied && suggestion.voiceScriptOutdated" class="text-[15px] text-[#92650A]">기존 녹음은 수정 전 문구일 수 있어요.</p>
          <Btn v-if="applied || sameText" variant="secondary" @click="edit(true)">이 단계 가족 음성 {{ suggestion.hasFamilyAudio ? '다시 녹음하기' : '녹음하기' }}</Btn>
          <Btn variant="secondary" :disabled="saving || loading" @click="edit(false)">직접 문구와 음성 편집하기</Btn>
        </template>
      </template>
      <template v-else>
        <p class="text-[20px] font-bold">{{ emptyMessage }}</p>
        <Btn v-if="result?.status === 'CONSENT_REQUIRED'" @click="store.navigate('consent')">동의 선택하기</Btn>
        <Btn v-else-if="result?.status === 'CONSENT_DECLINED'" @click="store.navigate('settings')">설정에서 확인하기</Btn>
        <p v-else class="text-[16px] text-[#6B7280]">이용 기록이 쌓이면 해당 단계의 문구를 비교할 수 있어요.</p>
      </template>
      <Btn variant="secondary" :disabled="saving" @click="back">이용 분석으로 돌아가기</Btn>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { onBeforeRouteLeave, useRoute } from 'vue-router';
import { instructionSuggestionApi } from '../api/instructionSuggestionApi.js';
import { useAppStore } from '../stores/appStore.js';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Card from '../components/common/Card.vue';
import Btn from '../components/common/Btn.vue';

const route = useRoute();
const store = useAppStore();
const dateText = (date) => [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-');
const today = new Date();
const start = new Date(today);
start.setDate(start.getDate() - 6);
const period = { from: String(route.query.from ?? dateText(start)), to: String(route.query.to ?? dateText(today)) };
const result = ref(null);
const loading = ref(false);
const saving = ref(false);
const loadError = ref('');
const saveError = ref('');
const applied = ref(false);
const stale = ref(false);
const suggestion = computed(() => result.value?.suggestion);
const sameText = computed(() => suggestion.value?.currentText === suggestion.value?.suggestedText);
const emptyMessage = computed(() => ({
  CONSENT_REQUIRED: '먼저 이용 기록 동의를 선택해 주세요.',
  CONSENT_DECLINED: '이용 기록 수집을 사용하지 않고 있어요.',
}[result.value?.status] ?? '현재 제안할 단계가 없어요.'));

async function load() {
  if (loading.value || saving.value) return;
  loading.value = true;
  loadError.value = saveError.value = '';
  applied.value = stale.value = false;
  try { result.value = await instructionSuggestionApi.get(period.from, period.to); }
  catch (error) { loadError.value = error?.message ?? '문구 제안을 불러오지 못했어요.'; }
  finally { loading.value = false; }
}

async function apply() {
  if (saving.value || !suggestion.value || applied.value || sameText.value || stale.value) return;
  saving.value = true;
  saveError.value = '';
  const target = suggestion.value;
  try {
    const saved = await instructionSuggestionApi.apply(target.patternId, target.stepId, target.currentText, target.suggestedText);
    target.currentText = saved.text;
    target.voiceScriptOutdated = saved.voiceScriptOutdated;
    target.hasFamilyAudio = Boolean(saved.audioUrl);
    applied.value = true;
  } catch (error) {
    stale.value = error?.code === 'INSTRUCTION_CHANGED';
    saveError.value = error?.message ?? '적용하지 못했어요. 기존 문구와 제안을 유지했으니 다시 시도해 주세요.';
  } finally { saving.value = false; }
}

function edit(rerecord) {
  if (saving.value || !suggestion.value) return;
  store.navigate('step-voice-edit', {
    params: { patternId: suggestion.value.patternId, stepOrder: suggestion.value.stepOrder },
    query: rerecord ? { rerecord: '1' } : {},
  });
}
function back() { if (!saving.value) store.navigate('analysis'); }
onMounted(load);
onBeforeRouteLeave(() => !saving.value);
</script>
