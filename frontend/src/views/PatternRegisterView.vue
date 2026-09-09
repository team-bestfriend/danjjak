<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />
    <TopBar :title="editing ? '금융 패턴 수정' : '금융 패턴 만들기'" :on-back="back" :back-disabled="submitting" />

    <div v-if="loading" class="flex flex-1 items-center justify-center text-[#6B7280]" role="status">
      패턴 정보를 불러오고 있어요…
    </div>
    <div v-else-if="loadError" class="flex flex-1 flex-col items-center justify-center gap-4 px-6 text-center">
      <p class="text-[#B91C1C]" role="alert">{{ loadError }}</p>
      <Btn variant="secondary" @click="initialize">다시 시도</Btn>
    </div>

    <template v-else>
      <div class="border-b border-[#EEEEED] bg-white px-5 py-3">
        <p class="text-[14px] font-bold text-[#92650A]">{{ stageIndex + 1 }} / {{ stages.length }}</p>
        <div class="mt-2 h-2 overflow-hidden rounded-full bg-[#F3F4F6]">
          <div class="h-full rounded-full bg-[#FFBC00] transition-all" :style="{ width: `${((stageIndex + 1) / stages.length) * 100}%` }" />
        </div>
      </div>

      <main class="flex-1 overflow-y-auto px-5 py-6">
        <section v-if="stage === 'template'" class="space-y-4">
          <div>
            <h1 class="text-[26px] font-bold text-[#111827]">어떤 금융 업무인가요?</h1>
            <p class="mt-2 text-[15px] text-[#6B7280]">서버에서 제공하는 정해진 업무만 등록할 수 있어요.</p>
          </div>
          <button
            v-for="template in store.patternTemplates"
            :key="template.patternType"
            type="button"
            :disabled="Boolean(persistedId) || !template.available"
            :class="[
              'w-full rounded-[18px] border-2 bg-white p-4 text-left disabled:cursor-not-allowed',
              selectedType === template.patternType ? 'border-[#FFBC00]' : 'border-[#E5E7EB]',
              template.available ? '' : 'opacity-50',
            ]"
            @click="selectTemplate(template)"
          >
            <span class="text-[18px] font-bold text-[#111827]">{{ typeLabel(template.patternType) }}</span>
            <span class="mt-1 block text-[14px] text-[#6B7280]">{{ template.defaultDescription }}</span>
            <span v-if="!template.available" class="mt-2 inline-block rounded-full bg-[#F3F4F6] px-2 py-1 text-[12px] font-bold text-[#6B7280]">준비 중</span>
          </button>
        </section>

        <section v-else-if="stage === 'shortcut'" class="space-y-5">
          <div>
            <h1 class="text-[26px] font-bold text-[#111827]">몇 번으로 기억할까요?</h1>
            <p class="mt-2 text-[15px] text-[#6B7280]">비어 있는 1~12번 중 하나를 골라 주세요.</p>
          </div>
          <div class="grid grid-cols-3 gap-3">
            <button
              v-for="number in 12"
              :key="number"
              type="button"
              :disabled="Boolean(persistedId) || isUsed(number)"
              :class="[
                'h-[76px] rounded-[16px] border-2 text-[26px] font-black disabled:opacity-35',
                shortcutNumber === number ? 'border-[#FFBC00] bg-[#FFF3CC]' : 'border-[#E5E7EB] bg-white',
              ]"
              @click="shortcutNumber = number"
            >{{ number }}</button>
          </div>
        </section>

        <section v-else-if="stage === 'details'" class="space-y-5">
          <h1 class="text-[26px] font-bold text-[#111827]">시작 전에 보여줄 내용을 확인해요</h1>

          <label class="block space-y-2">
            <span class="text-[17px] font-bold text-[#374151]">패턴 이름</span>
            <input v-model.trim="title" maxlength="100" class="h-[60px] w-full rounded-[16px] border-2 border-[#E5E7EB] bg-white px-4 text-[18px] outline-none focus:border-[#FFBC00]" />
          </label>
          <label class="block space-y-2">
            <span class="text-[17px] font-bold text-[#374151]">시작 전 설명</span>
            <textarea v-model.trim="description" maxlength="500" rows="4" class="w-full resize-none rounded-[16px] border-2 border-[#E5E7EB] bg-white p-4 text-[16px] leading-relaxed outline-none focus:border-[#FFBC00]" />
          </label>

          <div v-if="selectedType === 'TRANSFER'" class="space-y-3">
            <p class="text-[17px] font-bold text-[#374151]">받는 사람과 계좌</p>
            <p v-if="recipientAccountOptions.length === 0" class="rounded-[16px] bg-[#F9FAFB] p-4 text-[15px] text-[#6B7280]">
              등록된 받는 계좌가 없어요. 설정에서 사람 및 계좌를 먼저 등록해 주세요.
            </p>
            <button
              v-for="option in recipientAccountOptions"
              :key="option.accountId"
              type="button"
              :aria-pressed="linkedBankAccountId === option.accountId"
              :class="[
                'flex w-full items-center gap-3 rounded-[16px] border-2 bg-white p-4 text-left',
                linkedBankAccountId === option.accountId ? 'border-[#FFBC00]' : 'border-[#E5E7EB]',
              ]"
              @click="linkedBankAccountId = option.accountId"
            >
              <span class="min-w-0 flex-1">
                <span class="text-[18px] font-bold text-[#111827]">{{ option.personEmoji }} {{ option.personName }} · {{ option.personRelation }}</span>
                <span class="mt-1 block text-[14px] text-[#6B7280]">{{ option.bankName }} · {{ option.masked }}<template v-if="option.accountAlias"> · {{ option.accountAlias }}</template></span>
              </span>
              <span v-if="linkedBankAccountId === option.accountId" class="whitespace-nowrap text-[15px] font-bold text-[#92650A]">✓ 선택됨</span>
            </button>
          </div>
        </section>

        <PatternVoiceEditor
          v-else-if="stage === 'voice'"
          key="description"
          title="패턴 시작 안내 음성 설정"
          :text="description"
          :default-text="selectedTemplate?.defaultDescription"
          :speed="store.currentUser?.settings?.voiceSpeed"
          :voice-mode="descriptionVoice.voiceMode"
          :default-mode="store.currentUser?.settings?.guideVoiceType"
          :audio-url="descriptionVoice.audioUrl"
          :voice-script-outdated="descriptionVoice.voiceScriptOutdated || Boolean(descriptionVoice.audioUrl && descriptionVoice.text !== description)"
          :recording-draft="descriptionVoice.recording"
          action-label="이 음성으로 다음 · 마지막에 저장"
          @dirty="voiceDirty = $event"
          @confirm="acceptDescription"
          @cancel="voiceDirty = false; stageIndex--"
        />

        <section v-else-if="stage === 'steps'" class="space-y-5">
          <PatternVoiceEditor
            v-if="selectedStep"
            :key="selectedStep.stepCode"
            :title="selectedStep.stepName"
            :text="selectedStep.instructionText"
            :default-text="selectedTemplate?.steps.find((item) => item.stepCode === selectedStep.stepCode)?.instructionText"
            :speed="store.currentUser?.settings?.voiceSpeed"
            :voice-mode="selectedStep.guidance?.voiceMode"
            :default-mode="store.currentUser?.settings?.guideVoiceType"
            :audio-url="selectedStep.guidance?.audioUrl"
            :voice-script-outdated="selectedStep.guidance?.voiceScriptOutdated"
            :recording-draft="selectedStep.guidance?.recording"
            action-label="이 음성 사용 · 마지막에 저장"
            @dirty="voiceDirty = $event"
            @confirm="acceptStep"
            @cancel="voiceDirty = false; selectedStep = null"
          />
          <template v-else>
          <div>
            <h1 class="text-[26px] font-bold text-[#111827]">단계별 음성 안내 설정</h1>
            <p class="mt-2 text-[15px] text-[#6B7280]">각 단계를 눌러 문구를 편집하고 미리 들어보세요.</p>
          </div>
          <button v-for="step in stepInstructions" :key="step.stepCode" type="button" class="flex min-h-[100px] w-full items-center gap-4 rounded-[20px] border border-[#E5E7EB] bg-white p-4 text-left" @click="selectedStep = step">
            <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[#FFBC00] text-[20px] font-bold">{{ step.stepOrder }}</span>
            <span class="min-w-0 flex-1"><strong class="block text-[18px]">{{ step.stepName }}</strong><span class="mt-1 block break-words text-[15px] text-[#6B7280]">“{{ step.instructionText }}”</span><span class="block text-[14px] text-[#92650A]">{{ voiceLabel(step.guidance, store.currentUser?.settings?.guideVoiceType) }}</span></span>
            <span aria-hidden="true">›</span>
          </button>
          <p class="text-[14px] text-[#6B7280]">변경한 문구는 마지막 저장하기를 눌러야 반영돼요. 수정하지 않고 다음으로 진행해도 괜찮아요.</p>
          </template>
        </section>

        <section v-else class="space-y-5">
          <h1 class="text-[26px] font-bold text-[#111827]">이 내용으로 저장할까요?</h1>
          <Card class="overflow-hidden">
            <div v-for="row in summaryRows" :key="row.label" class="flex justify-between gap-4 border-b border-[#F3F4F6] px-5 py-4 last:border-0">
              <span class="text-[15px] text-[#6B7280]">{{ row.label }}</span>
              <span class="whitespace-pre-line text-right text-[16px] font-bold text-[#111827]">{{ row.value }}</span>
            </div>
          </Card>
          <p v-if="submitError" class="rounded-[14px] bg-[#FEF2F2] p-4 text-[15px] text-[#B91C1C]" role="alert">{{ submitError }}</p>
          <p v-if="savedTargets.length" class="text-[15px] text-[#6B7280]">저장 완료한 음성: {{ savedTargets.join(', ') }}</p>
        </section>
      </main>

      <div v-if="stage !== 'voice' && !selectedStep" class="flex gap-3 border-t border-[#EEEEED] bg-white px-5 pb-8 pt-4">
        <Btn v-if="stageIndex > 0" variant="secondary" class="flex-1" :disabled="submitting" @click="back">이전</Btn>
        <Btn class="flex-1" :disabled="!canContinue || submitting" @click="continueOrSubmit">
          {{ stage === 'confirm' ? (submitting ? '저장 중…' : '저장하기') : '다음' }}
        </Btn>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { onBeforeRouteLeave, useRoute } from 'vue-router';
import PatternVoiceEditor from '../components/common/PatternVoiceEditor.vue';
import Btn from '../components/common/Btn.vue';
import Card from '../components/common/Card.vue';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import { useAppStore } from '../stores/appStore';
import { patternApi } from '../api/patternApi.js';
import { saveGuidanceDraft, voiceLabel } from '../api/guidanceApi.js';
import { apiUrl } from '../api/httpClient.js';

const route = useRoute();
const store = useAppStore();
const editing = computed(() => Number.isInteger(Number(route.query.edit)) && Number(route.query.edit) > 0);
const stages = computed(() => editing.value
  ? ['template', 'details', 'voice', 'steps', 'confirm']
  : ['template', 'shortcut', 'details', 'voice', 'steps', 'confirm']);
const stageIndex = ref(0);
const stage = computed(() => stages.value[stageIndex.value]);
const loading = ref(true);
const loadError = ref('');
const submitting = ref(false);
const saved = ref(false);
const submitError = ref('');
const selectedType = ref('');
const shortcutNumber = ref(null);
const title = ref('');
const description = ref('');
const descriptionVoice = ref({});
const persistedId = ref(editing.value ? Number(route.query.edit) : null);
const savedTargets = ref([]);
const initialSignature = ref('');
const draftSignature = computed(() => JSON.stringify([selectedType.value, shortcutNumber.value, title.value, description.value, linkedBankAccountId.value]));
const linkedBankAccountId = ref(null);
const stepInstructions = ref([]);
const selectedStep = ref(null);
const voiceDirty = ref(false);
const voiceDraftChanged = ref(false);

const selectedTemplate = computed(() => store.patternTemplates.find((item) => item.patternType === selectedType.value));
/*
 * 한 사람에게 등록된 모든 받는 계좌를 계좌 단위로 펼쳐 보여 준다.
 * 사람 단위로 첫 계좌만 노출하면 두 번째 계좌를 패턴에 연결할 수 없다.
 */
const recipientAccountOptions = computed(() => store.people.flatMap((person) => (
  (store.accountsByPerson[person.id] ?? []).map((account) => ({
    ...account,
    personId: person.id,
    personName: person.name,
    personRelation: person.relation,
    personEmoji: person.emoji,
  }))
)));
const selectedRecipientOption = computed(() => (
  recipientAccountOptions.value.find((option) => option.accountId === linkedBankAccountId.value) ?? null
));
const selectedPerson = computed(() => (
  selectedRecipientOption.value
    ? store.people.find((person) => person.id === selectedRecipientOption.value.personId) ?? null
    : null
));
const canContinue = computed(() => {
  if (stage.value === 'template') return Boolean(selectedTemplate.value?.available);
  if (stage.value === 'shortcut') return Number.isInteger(shortcutNumber.value) && !isUsed(shortcutNumber.value);
  if (stage.value === 'details') {
    return title.value.length > 0
      && description.value.length > 0
      && (selectedType.value !== 'TRANSFER' || Boolean(linkedBankAccountId.value));
  }
  if (stage.value === 'steps') return stepInstructions.value.length > 0
    && stepInstructions.value.every((step) => step.instructionText.length > 0);
  return true;
});
const summaryRows = computed(() => [
  { label: '단축번호', value: `${shortcutNumber.value}번` },
  { label: '금융 업무', value: typeLabel(selectedType.value) },
  { label: '패턴 이름', value: title.value },
  { label: '시작 전 설명', value: description.value },
  { label: '시작 안내 음성', value: voiceLabel(descriptionVoice.value, store.currentUser?.settings?.guideVoiceType) },
  ...(selectedPerson.value ? [{ label: '받는 사람', value: `${selectedPerson.value.name} · ${selectedPerson.value.relation}` }] : []),
  ...(selectedRecipientOption.value ? [{ label: '받는 계좌', value: `${selectedRecipientOption.value.bankName} · ${selectedRecipientOption.value.masked}` }] : []),
  { label: '안내 단계', value: `${stepInstructions.value.length}단계` },
  ...stepInstructions.value.map((step) => ({ label: `${step.stepOrder}. ${step.stepName}`, value: `${step.instructionText}\n${voiceLabel(step.guidance, store.currentUser?.settings?.guideVoiceType)}` })),
]);

const TYPE_LABELS = {
  TRANSFER: '등록한 사람에게 송금',
  PENSION_CHECK: '연금 입금 확인',
  MANAGEMENT_FEE_CHECK: '관리비 확인',
  BALANCE_CHECK: '잔액 확인',
  TRANSACTION_HISTORY: '거래내역 조회',
  CUSTOMER_CENTER: '고객센터 연결',
  UTILITY_BILL_CHECK: '공과금 확인',
  AUTO_TRANSFER_CHECK: '자동이체 확인',
  CARD_HISTORY: '카드 이용내역',
  DEPOSIT_MATURITY_CHECK: '예금 만기 확인',
};

function typeLabel(type) {
  return TYPE_LABELS[type] ?? type;
}

function isUsed(number) {
  return store.patterns.some((pattern) => pattern.num === number && pattern.patternId !== persistedId.value);
}

function selectTemplate(template) {
  if (!template.available || persistedId.value) return;
  if (voiceDraftChanged.value && !window.confirm('음성 초안을 버리고 다른 업무를 선택할까요?')) return;
  selectedType.value = template.patternType;
  title.value = template.defaultTitle;
  description.value = template.defaultDescription;
  descriptionVoice.value = {};
  voiceDraftChanged.value = false;
  linkedBankAccountId.value = null;
  stepInstructions.value = [...template.steps].sort((a, b) => a.stepOrder - b.stepOrder).map((step) => ({ ...step }));
}

async function initialize() {
  loading.value = true;
  loadError.value = '';
  try {
    await Promise.all([
      store.loadPatternTemplates(true),
      store.loadPatterns(),
      store.loadFinancialData(),
    ]);
    if (editing.value) {
      const detail = await store.loadPatternDetail(Number(route.query.edit));
      selectedType.value = detail.patternType;
      shortcutNumber.value = detail.shortcutNumber;
      title.value = detail.title;
      description.value = detail.description;
      descriptionVoice.value = { ...detail.guidance };
      linkedBankAccountId.value = detail.linkedAccount?.accountId ?? null;
      stepInstructions.value = [...detail.steps].sort((a, b) => a.stepOrder - b.stepOrder).map((step) => ({ ...step }));
    }
    initialSignature.value = draftSignature.value;
  } catch (error) {
    loadError.value = error?.message ?? '패턴 정보를 불러오지 못했습니다.';
  } finally {
    loading.value = false;
  }
}

async function continueOrSubmit() {
  if (!canContinue.value || submitting.value) return;
  if (stage.value !== 'confirm') {
    stageIndex.value += 1;
    return;
  }

  submitting.value = true;
  submitError.value = '';
  const instructions = stepInstructions.value.map((step) => ({
    stepCode: step.stepCode,
    instructionText: step.instructionText,
  }));
  try {
    const detail = persistedId.value
      ? await patternApi.updatePattern(persistedId.value, {
        title: title.value,
        description: description.value,
        linkedBankAccountId: linkedBankAccountId.value,
        stepInstructions: instructions,
      })
      : await patternApi.createPattern({
        patternType: selectedType.value,
        shortcutNumber: shortcutNumber.value,
        title: title.value,
        description: description.value,
        linkedBankAccountId: linkedBankAccountId.value,
        stepInstructions: instructions,
      });
    // 이후 조회/업로드가 실패해도 성공한 생성 ID로 다시 저장한다.
    persistedId.value = detail.patternId;
    const targets = [
      { target: 'start', name: '시작 안내', text: description.value, voice: descriptionVoice.value },
      ...stepInstructions.value.map((step) => ({ target: step.stepCode, name: step.stepName, text: step.instructionText, voice: step.guidance })),
    ];
    for (const target of targets.filter((item) => item.voice?.changed)) {
      const result = await saveGuidanceDraft(detail.patternId, target.target, { ...target.voice, text: target.text });
      Object.assign(target.voice, result, { audioUrl: result.audioUrl ? apiUrl(result.audioUrl) : null, recording: null, changed: false });
      if (!savedTargets.value.includes(target.name)) savedTargets.value.push(target.name);
    }
    await store.loadPatternDetail(detail.patternId);
    await store.loadPatterns(true);
    store.showToast(editing.value ? '패턴을 수정했어요.' : '새 패턴을 등록했어요.');
    saved.value = true;
    await store.navigate('pattern-detail', { params: { patternId: detail.patternId }, replace: true });
  } catch (error) {
    submitError.value = error?.message ?? '패턴을 저장하지 못했습니다.';
    if (persistedId.value) {
      try {
        await store.loadPatternDetail(persistedId.value);
        submitError.value += ' 서버에 저장된 상태를 다시 확인했어요. 패턴은 유지되며 남은 음성 초안도 보관했어요. 다시 저장하면 이어서 처리해요.';
      } catch { submitError.value += ' 저장 상태를 다시 확인하지 못했어요. 초안을 유지했으니 연결을 확인하고 다시 저장해 주세요.'; }
    }
  } finally {
    submitting.value = false;
  }
}

onMounted(initialize);

function acceptDescription(draft) {
  voiceDraftChanged.value = true;
  description.value = draft.text;
  descriptionVoice.value = { ...descriptionVoice.value, ...draft, changed: true,
    voiceScriptOutdated: descriptionVoice.value.voiceScriptOutdated || Boolean(descriptionVoice.value.audioUrl && descriptionVoice.value.text !== draft.text) };
  voiceDirty.value = false;
  stageIndex.value++;
}

function acceptStep(draft) {
  voiceDraftChanged.value = true;
  selectedStep.value.instructionText = draft.text;
  const previous = selectedStep.value.guidance;
  selectedStep.value.guidance = { ...previous, ...draft, changed: true,
    voiceScriptOutdated: previous?.voiceScriptOutdated || Boolean(previous?.audioUrl && previous.text !== draft.text) };
  selectedStep.value = null;
  voiceDirty.value = false;
}

function back() {
  if (submitting.value) return;
  if (voiceDirty.value && !window.confirm('편집 중인 문구와 녹음을 버리고 돌아갈까요?')) return;
  voiceDirty.value = false;
  if (selectedStep.value) selectedStep.value = null;
  else if (stageIndex.value > 0) stageIndex.value--;
  else store.goBack();
}

onBeforeRouteLeave(() => saved.value || (!submitting.value && (!(voiceDirty.value || voiceDraftChanged.value || draftSignature.value !== initialSignature.value) || window.confirm('저장하지 않은 문구와 녹음을 버리고 이동할까요?'))));
</script>
