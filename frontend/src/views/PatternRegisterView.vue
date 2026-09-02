<template>
  <div className="flex flex-col h-full" style="background: #FAFAF8;">
    <template v-if="step !== 'done'">
      <SafeArea />
      <TopBar :title="editing ? '패턴 수정' : '패턴 등록'" :onBack="prev" />
      <StepBar :current="currentStepIdx + 1" :total="5" />
    </template>

    <div className="flex-1 overflow-y-auto px-4 pt-5 pb-4 space-y-4">
      <!-- Step 1: Task -->
      <template v-if="step === 'task'">
        <p className="font-bold text-[#111827]" style="font-size: 26px;">어떤 금융 업무를 등록할까요?</p>
        <Card
          v-for="t in TASK_TYPES"
          :key="t.key"
          :highlighted="selTask === t.key"
          @click="selTask = t.key"
          className="px-5 py-4"
        >
          <div className="flex items-center gap-4">
            <div className="text-[#9CA3AF]"><Ic :name="t.icon" /></div>
            <span className="flex-1 font-bold text-[#111827]" style="font-size: 18px;">{{ t.label }}</span>
            <span v-if="selTask === t.key" className="text-[#FFBC00] text-xl font-black">✓</span>
          </div>
        </Card>
        <Btn :disabled="!canNext" @click="next">다음</Btn>
      </template>

      <!-- Step 2: Shortcut Number -->
      <template v-else-if="step === 'shortcut'">
        <p className="font-bold text-[#111827]" style="font-size: 26px;">몇 번으로 기억할까요?</p>
        <div className="grid grid-cols-3 gap-2.5">
          <button
            v-for="n in 12"
            :key="n"
            @click="!usedNums.includes(n) && (selNum = n)"
            :class="['rounded-[16px] flex flex-col items-center justify-center gap-1 border-2 transition-all']"
            :style="{
              height: '72px',
              backgroundColor: usedNums.includes(n) ? '#F3F4F6' : (selNum === n ? '#FFBC00' : '#FFBC00'),
              borderColor: selNum === n ? '#111827' : 'transparent',
              opacity: usedNums.includes(n) ? 0.4 : 1
            }"
          >
            <span className="font-black text-[#111827]" style="font-size: 26px;">{{ n }}</span>
            <span className="font-bold text-[#111827]" style="font-size: 11px; opacity: 0.65;">{{ usedNums.includes(n) ? '사용 중' : '선택' }}</span>
          </button>
        </div>
        <Btn :disabled="!canNext" @click="next">다음</Btn>
      </template>

      <!-- Step 3: Person -->
      <template v-else-if="step === 'person'">
        <template v-if="addingPerson">
          <AddPersonForm @saved="onPersonSaved" @cancel="addingPerson = false" />
        </template>

        <template v-else>
          <p className="font-bold text-[#111827]" style="font-size: 26px;">{{ selTask === 'transfer' ? '누구에게 보내는 업무인가요?' : '거래 대상 설정' }}</p>
          <template v-if="selTask === 'transfer'">
            <button
              @click="addingPerson = true"
              className="w-full rounded-[18px] border-2 border-dashed border-[#FFBC00] text-[#92650A] font-bold flex items-center justify-center gap-2"
              style="min-height: 60px; font-size: 17px;"
            >
              <Ic name="Plus" />새로운 가족 등록
            </button>
            <Card
              v-for="p in store.people"
              :key="p.id"
              :highlighted="selPerson?.id === p.id"
              @click="selPerson = p"
              className="p-5"
            >
              <div className="flex items-center gap-4">
                <div className="w-13 h-13 rounded-full bg-[#FFF3CC] border border-[#FFBC00] flex items-center justify-center flex-shrink-0" style="width: 52px; height: 52px; font-size: 26px;">
                  {{ p.emoji }}
                </div>
                <div className="flex-1">
                  <p className="font-bold text-[#111827]" style="font-size: 20px;">{{ p.name }}</p>
                  <p className="text-[#6B7280]" style="font-size: 14px;">{{ p.relation }}</p>
                </div>
                <span v-if="selPerson?.id === p.id" className="text-[#FFBC00] font-black text-xl">✓</span>
                <Ic v-else name="ChevR" />
              </div>
            </Card>
          </template>
          <Card v-else className="p-5">
            <p className="text-[#6B7280]" style="font-size: 17px;">이 업무는 거래 대상이 필요하지 않아요.</p>
          </Card>
          <Btn :disabled="!canNext" @click="next">다음</Btn>
        </template>
      </template>

      <!-- Step 4: Voice -->
      <template v-else-if="step === 'voice'">
        <template v-if="voiceMode === 'none'">
          <p className="font-bold text-[#111827]" style="font-size: 26px;">패턴 설명 음성 설정</p>
          <p className="text-[#6B7280]" style="font-size: 16px;">패턴을 시작할 때 어떤 음성으로 안내할까요?</p>
          <div className="bg-[#FFF3CC] border border-[#FFBC00] rounded-[18px] p-4 mb-1">
            <p className="font-black text-[#92650A] mb-1" style="font-size: 13px;">자동 생성 문구 예시</p>
            <p className="font-bold text-[#111827]" style="font-size: 15px;">"{{ patternDesc }}"</p>
          </div>
          <Card @click="voiceMode = 'tts'" className="p-5 cursor-pointer active:bg-[#FFF3CC]">
            <div className="flex items-start gap-3">
              <div className="w-11 h-11 rounded-full bg-[#FFF3CC] border-2 border-[#FFBC00] flex items-center justify-center flex-shrink-0" style="font-size: 20px;">🤖</div>
              <div>
                <p className="font-semibold text-[#111827]" style="font-size: 19px;">TTS 자동 음성</p>
                <p className="text-[#6B7280] mt-0.5" style="font-size: 14px;">등록 정보로 자동 생성된 음성을 사용해요.</p>
              </div>
            </div>
          </Card>
          <Card @click="voiceMode = 'family'; fRecState = 'idle'; fRecSec = 0;" className="p-5 cursor-pointer active:bg-[#F3F4F6]">
            <div className="flex items-start gap-3">
              <div className="w-11 h-11 rounded-full bg-[#F3F4F6] border-2 border-[#E5E7EB] flex items-center justify-center flex-shrink-0" style="font-size: 20px;">🎙️</div>
              <div>
                <p className="font-semibold text-[#111827]" style="font-size: 19px;">가족 음성 녹음</p>
                <p className="text-[#6B7280] mt-0.5" style="font-size: 14px;">가족이 직접 음성을 녹음해요.</p>
              </div>
            </div>
          </Card>
          <button @click="next" className="w-full py-4 font-medium text-[#9CA3AF]" style="font-size: 16px;">음성 안내 없이 진행</button>
        </template>

        <template v-else-if="voiceMode === 'tts'">
          <p className="font-bold text-[#111827]" style="font-size: 26px;">TTS 자동 음성</p>
          <Card className="p-5 border-2 border-[#FFBC00]">
            <p className="font-black text-[#92650A] mb-2" style="font-size: 13px;">재생될 문구</p>
            <p className="font-bold text-[#111827] mb-4" style="font-size: 16px;">"{{ patternDesc }}"</p>
            <button @click="descPlaying = !descPlaying" className="flex items-center gap-2 font-bold text-[#2563EB]" style="font-size: 15px;">
              <div className="w-9 h-9 rounded-full bg-[#DBEAFE] flex items-center justify-center">{{ descPlaying ? '⏸' : '▶' }}</div>
              미리 듣기
            </button>
          </Card>
          <Btn @click="next">이 음성으로 저장</Btn>
          <Btn variant="secondary" @click="voiceMode = 'none'">방식 변경</Btn>
        </template>

        <template v-else-if="voiceMode === 'family'">
          <p className="font-bold text-[#111827]" style="font-size: 26px;">가족 음성 녹음</p>

          <button
            v-if="fRecState === 'idle'"
            @click="fRecState = 'recording'"
            className="w-full rounded-[18px] bg-[#374151] text-white font-bold flex items-center justify-center gap-2"
            style="min-height: 64px; font-size: 18px;"
          >
            <Ic name="Mic" />녹음 시작
          </button>
          <div v-else-if="fRecState === 'recording'" className="space-y-3">
            <div className="w-full rounded-[18px] bg-[#EF4444] text-white flex items-center justify-center gap-3" style="min-height: 64px;">
              <div className="w-3 h-3 rounded-full bg-white animate-pulse" />
              <span className="font-bold" style="font-size: 18px;">녹음 중... {{ fRecSec }}초</span>
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
          <div v-else-if="fRecState === 'done'" className="space-y-3">
            <div className="bg-[#F0FDF4] border border-[#86EFAC] rounded-[16px] p-4 flex items-center gap-3">
              <span style="font-size: 22px;">✅</span>
              <div>
                <p className="font-semibold text-[#15803D]" style="font-size: 16px;">녹음이 완료됐어요.</p>
                <p className="text-[#16A34A] mt-0.5" style="font-size: 13px;">00:0{{ fRecSec }} 녹음됨</p>
              </div>
            </div>
            <div className="flex gap-2">
              <button className="flex-1 border border-[#EBEBEA] rounded-[14px] font-bold text-[#2563EB]" style="height: 52px; font-size: 15px;">▶ 미리 듣기</button>
              <button @click="fRecState = 'idle'; fRecSec = 0;" className="flex-1 border border-[#EBEBEA] rounded-[14px] font-bold text-[#6B7280]" style="height: 52px; font-size: 15px;">다시 녹음</button>
            </div>
            <Btn @click="next">저장하기</Btn>
          </div>

          <Btn variant="secondary" @click="voiceMode = 'none'; fRecState = 'idle';" className="mt-2">
            방식 변경
          </Btn>
        </template>
      </template>

      <!-- Step 5: Confirm -->
      <template v-else-if="step === 'confirm'">
        <p className="font-bold text-[#111827]" style="font-size: 26px;">이렇게 등록할까요?</p>
        <Card className="overflow-hidden">
          <div className="px-5 py-4 flex items-center gap-4 border-b border-[#F3F4F6]" style="background: #FFF3CC;">
            <div className="w-14 h-14 rounded-2xl flex items-center justify-center flex-shrink-0" style="background: #FFBC00;">
              <span className="font-bold text-[#111827]" style="font-size: 24px;">{{ selNum }}</span>
            </div>
            <span className="font-semibold text-[#111827]" style="font-size: 20px;">{{ taskLabelName }}</span>
          </div>
          <div
            v-for="(r, i) in confirmRows"
            :key="i"
            :class="['flex items-center justify-between px-5 py-4', i < confirmRows.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
          >
            <span className="text-[#6B7280]" style="font-size: 15px;">{{ r.l }}</span>
            <span className="font-bold text-[#111827]" style="font-size: 17px;">{{ r.v }}</span>
          </div>
        </Card>
        <Btn @click="saveAndFinish">{{ editing ? '수정 완료' : '등록하기' }}</Btn>
        <Btn variant="secondary" @click="prev">다시 확인</Btn>
      </template>

      <!-- Step Done -->
      <template v-else-if="step === 'done'">
        <SafeArea />
        <div className="flex flex-col items-center gap-6 py-10">
          <div className="w-24 h-24 rounded-full flex items-center justify-center" style="background: #22C55E;">
            <Ic name="Check" />
          </div>
          <div className="text-center">
            <p className="font-bold text-[#111827]" style="font-size: 26px;">{{ editing ? '수정 완료!' : '등록 완료!' }}</p>
            <p className="text-[#374151] mt-2" style="font-size: 18px;">
              <span className="font-bold text-[#15803D]">{{ selNum }}번</span> 단축번호에 반영됐어요.
            </p>
          </div>
          <Btn @click="store.navigate('patterns')">패턴 목록으로</Btn>
          <Btn variant="secondary" @click="store.navigate('home')">홈으로</Btn>
        </div>
      </template>
    </div>

    <!-- Swap Confirm Sheet -->
    <BottomSheet :open="!!swapConfirm" @close="swapConfirm = null">
      <template v-if="swapConfirm">
        <p className="font-bold text-[#111827] px-1 pb-1" style="font-size: 20px;">{{ swapConfirm.targetNum }}번에 이미 다른 패턴이 있어요.</p>
        <p className="text-[#6B7280] px-2 pb-3" style="font-size: 14px;">두 패턴의 번호를 바꿀 수 있어요.</p>
        <SheetRow label="두 패턴의 번호 바꾸기" color="#2563EB" @click="confirmSwapSave" />
        <SheetRow label="다른 번호 선택" @click="swapConfirm = null; step = 'shortcut';" />
      </template>
    </BottomSheet>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import { TASK_COLORS, VOICE_SCRIPTS, generatePatternDesc } from '../constants/data';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import StepBar from '../components/common/StepBar.vue';
import Card from '../components/common/Card.vue';
import Btn from '../components/common/Btn.vue';
import Ic from '../components/common/Ic.vue';
import BottomSheet from '../components/common/BottomSheet.vue';
import SheetRow from '../components/common/SheetRow.vue';
import AddPersonForm from '../components/common/AddPersonForm.vue';

const store = useAppStore();

const REG_STEPS = ["task", "shortcut", "person", "voice", "confirm"];

const TASK_TYPES = [
  { key: "transfer", label: "송금", icon: "Transfer" },
  { key: "pension", label: "연금 / 입금 확인", icon: "Pension" },
  { key: "building", label: "관리비 확인", icon: "Building" },
  { key: "balance", label: "잔액 확인", icon: "Wallet" },
  { key: "history", label: "거래내역 조회", icon: "History" },
  { key: "support", label: "고객센터 연결", icon: "Headset" },
  { key: "utility", label: "공과금 확인", icon: "Building" },
  { key: "autotransfer", label: "자동이체 확인", icon: "Repeat" },
  { key: "cardhistory", label: "카드 이용내역", icon: "CreditCard" },
  { key: "deposit", label: "예금 만기 확인", icon: "Safe" },
  { key: "exchange", label: "환율 확인", icon: "Globe" },
];

const editing = computed(() => (store.editingId ? store.patterns.find((p) => p.id === store.editingId) : null));
const usedNums = computed(() => store.patterns.filter((p) => !editing.value || p.id !== editing.value.id).map((p) => p.num));

const step = ref("task");
const selTask = ref(editing.value?.taskType || "");
const selNum = ref(editing.value?.num || null);
const selPerson = ref(editing.value?.personId ? (store.people.find((p) => p.id === editing.value.personId) || null) : null);
const amountOpt = ref(editing.value?.amountOpt || "");
const voices = ref({ ...VOICE_SCRIPTS.reduce((a, v, i) => ({ ...a, [i]: v }), {}), ...(editing.value?.voices || {}) });
const swapConfirm = ref(null);

const addingPerson = ref(false);
const voiceMode = ref("none");
const fRecState = ref("idle");
const fRecSec = ref(0);
const descPlaying = ref(false);
let fRecTimer = null;

watch(fRecState, (newVal) => {
  if (fRecTimer) clearInterval(fRecTimer);
  if (newVal === 'recording') {
    fRecSec.value = 0;
    fRecTimer = setInterval(() => { fRecSec.value++; }, 1000);
  }
});

watch(fRecSec, (newSec) => {
  if (fRecState.value === 'recording' && newSec >= 3) {
    if (fRecTimer) clearInterval(fRecTimer);
    fRecState.value = 'done';
  }
});

onUnmounted(() => {
  if (fRecTimer) clearInterval(fRecTimer);
});

const currentStepIdx = computed(() => REG_STEPS.indexOf(step.value));

const canNext = computed(() => {
  if (step.value === "task") return !!selTask.value;
  if (step.value === "shortcut") return selNum.value !== null;
  if (step.value === "person" && selTask.value === "transfer") return !!selPerson.value;
  return true;
});

const patternDesc = computed(() => {
  const tmpPat = { id: "_", num: selNum.value || 1, label: "", color: "", sub: "", taskType: selTask.value, personId: selPerson.value?.id };
  return generatePatternDesc(tmpPat, store.people, store.accountsByPerson);
});

const taskLabelName = computed(() => TASK_TYPES.find((t) => t.key === selTask.value)?.label || selTask.value);

const confirmRows = computed(() => [
  { l: "단축번호", v: `${selNum.value}번` },
  selPerson.value ? { l: "받는 사람", v: `${selPerson.value.emoji} ${selPerson.value.name}` } : null
].filter(Boolean));

function prev() {
  if (addingPerson.value) { addingPerson.value = false; return; }
  if (currentStepIdx.value > 0) {
    step.value = REG_STEPS[currentStepIdx.value - 1];
  } else {
    store.editingId = null;
    store.goBack();
  }
}

function next() {
  if (currentStepIdx.value < REG_STEPS.length - 1) {
    step.value = REG_STEPS[currentStepIdx.value + 1];
  }
}

function onPersonSaved(id) {
  const p = store.people.find((person) => person.id === id);
  if (p) selPerson.value = p;
  addingPerson.value = false;
}

function saveAndFinish() {
  const num = selNum.value;
  const conflict = store.patterns.find((p) => p.num === num && (!editing.value || p.id !== editing.value.id));
  if (conflict && !swapConfirm.value) {
    swapConfirm.value = { targetNum: num };
    return;
  }
  const taskLabel = TASK_TYPES.find((t) => t.key === selTask.value)?.label || selTask.value;
  const newPat = {
    id: editing.value?.id || `p${Date.now()}`,
    num,
    label: taskLabel,
    color: TASK_COLORS[selTask.value] || "#FFBC00",
    sub: selPerson.value ? `${selPerson.value.emoji} ${selPerson.value.name} · ${selPerson.value.relation}` : taskLabel,
    taskType: selTask.value,
    personId: selPerson.value?.id,
    amountOpt: amountOpt.value,
    voices: voices.value
  };

  store.setPatterns((prevPatterns) => {
    let updated = prevPatterns.filter((p) => p.id !== newPat.id);
    if (conflict) {
      updated = updated.map((p) => p.id === conflict.id ? { ...p, num: editing.value?.num || -1 } : p);
    }
    return [...updated, newPat];
  });

  store.editingId = null;
  step.value = "done";
}

function confirmSwapSave() {
  swapConfirm.value = null;
  saveAndFinish();
}
</script>
