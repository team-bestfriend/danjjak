<template>
  <div className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="단계별 음성 안내 수정" :onBack="handleBack" />

    <div className="flex-1 overflow-y-auto px-4 pt-5 pb-6 space-y-4">

      <!-- 단계 목록 -->
      <template v-if="activeIdx === null">
        <p className="font-bold text-[#111827]" style="font-size: 22px;">각 단계의 음성을 수정해 주세요.</p>
        <div
          v-for="(s, i) in flowSteps"
          :key="i"
          @click="activeIdx = i"
          className="bg-white rounded-[18px] border border-[#EEEEED] p-4 flex items-center gap-4 cursor-pointer active:bg-[#FFF3CC] transition-colors"
        >
          <div
            className="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 font-bold"
            style="background: #FFBC00; font-size: 16px; color: #111827;"
          >
            {{ i + 1 }}
          </div>
          <div className="flex-1">
            <p className="font-semibold text-[#111827]" style="font-size: 17px;">{{ s.label }}</p>
            <p className="text-[#6B7280] mt-0.5 truncate" style="font-size: 13px;">"{{ customText[i] || s.script }}"</p>
          </div>
          <div>
            <span v-if="savedMode[i] === 'tts'" className="font-bold px-2 py-1 rounded-full text-[#15803D] bg-green-50 border border-green-200" style="font-size: 12px;">TTS ✓</span>
            <span v-else-if="savedMode[i] === 'family'" className="font-bold px-2 py-1 rounded-full text-[#2563EB] bg-blue-50 border border-blue-200" style="font-size: 12px;">녹음 ✓</span>
            <span v-else className="text-[#9CA3AF]" style="font-size: 20px;">›</span>
          </div>
        </div>
        <Btn @click="store.goBack">저장 완료</Btn>
      </template>

      <!-- 개별 단계 편집 -->
      <template v-else>
        <!-- 헤더 (선택 화면은 편집 버튼 없음) -->
        <div className="flex items-center justify-between">
          <p className="font-bold text-[#111827]" style="font-size: 22px;">{{ flowSteps[activeIdx]?.label }}</p>
          <button
            v-if="editMode !== 'select'"
            @click="ttsEditing = !ttsEditing; if(ttsEditing && !customText[activeIdx]) customText[activeIdx] = flowSteps[activeIdx]?.script"
            className="font-semibold rounded-[10px] px-3 py-1.5"
            :style="ttsEditing ? 'background:#FFBC00;color:#111827;font-size:17px;' : 'background:#F3F4F6;color:#374151;font-size:17px;'"
          >{{ ttsEditing ? '완료' : '편집' }}</button>
        </div>

        <!-- 대사 편집 인라인 (TTS/family 모드에서만) -->
        <template v-if="ttsEditing && editMode !== 'select'">
          <textarea
            v-model="customText[activeIdx]"
            :placeholder="flowSteps[activeIdx]?.script"
            rows="3"
            className="w-full rounded-[14px] border-2 border-[#FFBC00] outline-none px-4 py-3 resize-none"
            style="font-size: 15px; font-weight: 500; color: #111827; background: #FFFBEB; line-height: 1.5;"
          />
          <button @click="customText[activeIdx] = ''; ttsEditing = false" className="w-full py-2 font-medium text-[#9CA3AF]" style="font-size: 14px;">초기화 (기본 문구로 되돌리기)</button>
        </template>

        <!-- 방식 선택 -->
        <template v-if="editMode === 'select'">
          <button
            @click="editMode = 'tts'"
            className="w-full rounded-[20px] bg-white border border-[#E5E7EB] p-5 flex items-center gap-4 active:scale-[0.98] transition-all"
          >
            <div className="w-14 h-14 rounded-full bg-[#FFF3CC] border-2 border-[#FFBC00] flex items-center justify-center flex-shrink-0" style="font-size: 28px;">🤖</div>
            <div className="text-left">
              <p className="font-semibold text-[#111827]" style="font-size: 20px;">TTS 자동 음성</p>
              <p className="text-[#6B7280] mt-0.5" style="font-size: 14px;">자동 생성된 음성을 사용해요.</p>
            </div>
          </button>
          <button
            @click="editMode = 'family'; recState = 'idle'; recSec = 0;"
            className="w-full rounded-[20px] bg-white border border-[#E5E7EB] p-5 flex items-center gap-4 active:scale-[0.98] transition-all"
          >
            <div className="w-14 h-14 rounded-full bg-[#F3F4F6] border-2 border-[#E5E7EB] flex items-center justify-center flex-shrink-0" style="font-size: 28px;">🎙️</div>
            <div className="text-left">
              <p className="font-semibold text-[#111827]" style="font-size: 20px;">가족 음성 녹음</p>
              <p className="text-[#6B7280] mt-0.5" style="font-size: 14px;">가족이 직접 음성을 녹음해요.</p>
            </div>
          </button>
          <Btn variant="secondary" @click="activeIdx = null">목록으로 돌아가기</Btn>
        </template>

        <!-- TTS -->
        <template v-else-if="editMode === 'tts'">
          <div className="rounded-[18px] border-2 border-[#FFBC00] p-5" style="background: white;">
            <p className="font-black text-[#92650A] mb-2" style="font-size: 13px;">재생될 문구</p>
            <p
              className="mb-4 leading-snug"
              :style="customText[activeIdx] ? 'font-size:16px;font-weight:700;color:#111827;' : 'font-size:15px;font-weight:400;color:#9CA3AF;'"
            >"{{ customText[activeIdx] || flowSteps[activeIdx]?.script }}"</p>
            <button className="flex items-center gap-2 font-bold text-[#2563EB]" style="font-size: 15px;">
              <div className="w-9 h-9 rounded-full bg-[#DBEAFE] flex items-center justify-center">▶</div>
              미리 듣기
            </button>
          </div>
          <Btn @click="ttsEditing = false; saveAndBack('tts')">이 음성으로 저장</Btn>
          <Btn variant="secondary" @click="editMode = 'select'; ttsEditing = false;">방식 변경</Btn>
          <Btn variant="secondary" @click="activeIdx = null; ttsEditing = false;">목록으로 돌아가기</Btn>
        </template>

        <!-- 가족 녹음 -->
        <template v-else-if="editMode === 'family'">
          <div className="rounded-[18px] border border-[#FFBC00] px-4 py-4" style="background: #FFFBEB;">
            <p className="font-bold text-[#92650A] mb-2" style="font-size: 13px;">📢 다음을 읽어주세요</p>
            <p className="font-bold text-[#111827] leading-snug" style="font-size: 17px; word-break: keep-all;">"{{ customText[activeIdx] || flowSteps[activeIdx]?.script }}"</p>
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
              <div v-for="(_, i) in 32" :key="i" className="wave-bar flex-1 bg-[#EF4444] rounded-sm opacity-70" :style="{ animationDelay: `${i * 0.06}s` }" />
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
            <Btn @click="saveAndBack('family')">저장하기</Btn>
          </div>

          <template v-if="recState !== 'done'">
            <Btn variant="secondary" @click="editMode = 'select'; recState = 'idle';">방식 변경</Btn>
            <Btn variant="secondary" @click="activeIdx = null">목록으로 돌아가기</Btn>
          </template>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onUnmounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Btn from '../components/common/Btn.vue';
import Ic from '../components/common/Ic.vue';

const store = useAppStore();

const FLOW_STEPS_MAP = {
  transfer:     [
    { label: "계좌 선택",    script: "어느 계좌로 보낼지 선택해 주세요." },
    { label: "금액 입력",    script: "보내실 금액을 입력해 주세요." },
    { label: "비밀번호 입력", script: "계좌 비밀번호 네 자리를 입력해 주세요." },
  ],
  pension:      [{ label: "잔액 확인",      script: "연금 입금 내역을 확인해 주세요." }],
  balance:      [{ label: "잔액 확인",      script: "현재 잔액을 확인해 주세요." }],
  history:      [{ label: "거래 내역",      script: "최근 거래 내역을 확인해 주세요." }],
  building:     [{ label: "관리비 확인",    script: "이번 달 관리비를 확인해 주세요." }],
  utility:      [{ label: "공과금 확인",    script: "공과금 납부 내역을 확인해 주세요." }],
  autotransfer: [{ label: "자동이체 확인",  script: "자동이체 내역을 확인해 주세요." }],
  cardhistory:  [{ label: "카드 내역",      script: "카드 이용 내역을 확인해 주세요." }],
  deposit:      [{ label: "예금 만기",      script: "예금 만기 정보를 확인해 주세요." }],
  exchange:     [{ label: "환율 확인",      script: "오늘의 환율을 확인해 주세요." }],
  support:      [{ label: "고객센터 연결",  script: "상담사 연결을 시작할게요." }],
};

const flowSteps = computed(() => FLOW_STEPS_MAP[store.activePattern?.taskType] || []);

const activeIdx = ref(null);
const editMode = ref('select');
const savedMode = ref({});
const customText = ref({});
const ttsEditing = ref(false);

const recState = ref('idle');
const recSec = ref(0);
let recTimer = null;

watch(activeIdx, () => { editMode.value = 'select'; recState.value = 'idle'; recSec.value = 0; ttsEditing.value = false; });

watch(recState, (val) => {
  if (recTimer) clearInterval(recTimer);
  if (val === 'recording') {
    recSec.value = 0;
    recTimer = setInterval(() => { recSec.value++; }, 1000);
  }
});

watch(recSec, (s) => {
  if (recState.value === 'recording' && s >= 4) {
    clearInterval(recTimer);
    recState.value = 'done';
  }
});

onUnmounted(() => { if (recTimer) clearInterval(recTimer); });

function saveAndBack(mode) {
  savedMode.value = { ...savedMode.value, [activeIdx.value]: mode };
  activeIdx.value = null;
}

function handleBack() {
  if (activeIdx.value !== null) {
    activeIdx.value = null;
  } else {
    store.goBack();
  }
}
</script>
