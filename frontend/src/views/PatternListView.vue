<template>
  <!-- Patterns List -->
  <div v-if="viewMode === 'patterns'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <div className="bg-white px-5 border-b border-[#EEEEED] flex-shrink-0" style="padding-top: 18px; padding-bottom: 16px;">
      <h1 className="font-bold text-[#111827]" style="font-size: 26px;">등록된 금융 패턴</h1>
      <p className="font-normal text-[#9CA3AF] mt-1" style="font-size: 15px;">{{ store.patterns.length }} / 12개 · 꾹 누르면 순서를 바꿀 수 있어요</p>
    </div>
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-3">
      <div v-if="sortedPatterns.length === 0" className="text-center py-12 text-[#9CA3AF]">
        <p className="font-normal" style="font-size: 18px;">등록된 패턴이 없어요.</p>
      </div>
      <div
        v-for="p in sortedPatterns"
        :key="p.id"
        :data-slot-num="p.num"
        @pointerdown="handlePointerDown(p.num, $event)"
        @pointermove="handlePointerMove"
        @pointerup="handlePointerUp"
        @pointercancel="handlePointerCancel"
        @click="handleCardClick(p)"
        style="touch-action: none;"
        :class="[
          'bg-white rounded-[20px] border transition-all cursor-pointer select-none',
          isDragSrc(p.num) ? 'opacity-25 scale-[0.97] border-[#EBEBEA]' :
          isDropTgt(p.num) ? 'border-[#FFBC00] border-2 scale-[1.02]' : 'border-[#EBEBEA]'
        ]"
      >
        <div className="p-4 flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl flex items-center justify-center flex-shrink-0" :style="{ background: p.color }">
            <span className="font-bold text-white" style="font-size: 22px;">{{ p.num }}</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="font-semibold text-[#111827]" style="font-size: 19px;">{{ p.label }}</p>
            <p className="text-[#6B7280] mt-0.5 truncate" style="font-size: 13px;">{{ p.sub }}</p>
            <span className="inline-flex mt-1 font-bold text-[#16A34A] bg-green-50 px-2 py-0.5 rounded-full border border-green-200" style="font-size: 11px;">사용 중</span>
          </div>
          <div className="flex items-center flex-shrink-0">
            <button
              @click.stop="sheetPat = p; sheet = true;"
              className="w-10 h-10 flex items-center justify-center text-[#9CA3AF] font-bold rounded-full"
              style="font-size: 22px;"
            >
              ⋮
            </button>
          </div>
        </div>
      </div>

      <button
        v-if="store.patterns.length < 12"
        @click="store.editingId = null; store.navigate('pattern-register');"
        className="w-full border-2 border-dashed border-[#FFBC00] rounded-[20px] font-bold text-[#92650A] flex items-center justify-center gap-2"
        style="min-height: 60px; font-size: 17px;"
      >
        <Ic name="Plus" />새로운 금융 패턴 만들기
      </button>

      <div v-else className="bg-[#FFF3CC] border border-[#FFBC00] rounded-[18px] p-4 text-center">
        <p className="font-bold text-[#92650A]" style="font-size: 14px;">12개 패턴이 모두 등록되어 있습니다.</p>
      </div>
    </div>

    <NavBar active="patterns" :onSelect="store.navTo" />

    <!-- Drag ghost -->
    <div
      v-if="drag && ghostPat"
      className="pointer-events-none z-[9999] rounded-[20px]"
      :style="{
        position: 'fixed',
        left: `${drag.ghostX - 155}px`,
        top: `${drag.ghostY - 32}px`,
        width: '310px',
        background: '#FFBC00',
        opacity: 0.93,
        transform: 'scale(1.03)',
        boxShadow: '0 8px 32px rgba(255,188,0,0.4)'
      }"
    >
      <div className="p-4 flex items-center gap-3">
        <div className="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0" style="background: rgba(0,0,0,0.1);">
          <span className="font-black text-[#111827]" style="font-size: 20px;">{{ ghostPat.num }}</span>
        </div>
        <span className="font-black text-[#111827]" style="font-size: 17px;">{{ ghostPat.label }}</span>
      </div>
    </div>

    <!-- Sheets -->
    <BottomSheet :open="sheet && !delConfirm" @close="sheet = false">
      <SheetRow label="패턴 수정" :sub="sheetPat?.label" @click="editSheetPattern" />
      <SheetRow label="패턴 삭제" color="#EF4444" :sub="sheetPat?.label" @click="delConfirm = true" />
    </BottomSheet>

    <BottomSheet :open="delConfirm" @close="delConfirm = false; sheet = false;" :title="`${sheetPat?.label} 패턴을 삭제할까요?`">
      <p className="text-[#6B7280] px-2 pb-3" style="font-size: 14px;">삭제하면 홈의 단축번호에서도 사용할 수 없습니다.</p>
      <Btn variant="secondary" className="mb-2" @click="delConfirm = false; sheet = false;">취소</Btn>
      <Btn variant="danger" @click="doDelete">삭제하기</Btn>
    </BottomSheet>
  </div>

  <!-- Pattern Detail -->
  <div v-else-if="viewMode === 'pattern-detail'" className="flex flex-col h-full" style="background: #FAFAF8;">
    <SafeArea />
    <TopBar title="패턴 상세" :onBack="store.goBack" />
    <div className="flex-1 overflow-y-auto px-4 pt-4 pb-4 space-y-4">
      <div className="flex items-center gap-4">
        <div className="w-18 h-18 rounded-2xl flex items-center justify-center flex-shrink-0" :style="{ width: '72px', height: '72px', background: pDetail.color || '#FFBC00' }">
          <span className="font-bold text-white" style="font-size: 30px;">{{ pDetail.num }}</span>
        </div>
        <div>
          <p className="font-bold text-[#111827]" style="font-size: 23px;">{{ pDetail.label }}</p>
          <span className="font-bold text-[#16A34A] bg-green-50 px-2 py-0.5 rounded-full border border-green-200" style="font-size: 12px;">사용 중</span>
        </div>
      </div>
      <Card className="overflow-hidden">
        <div
          v-for="(r, i) in detailRows"
          :key="i"
          :class="['flex items-center justify-between px-5 py-4', i < detailRows.length - 1 ? 'border-b border-[#F3F4F6]' : '']"
        >
          <span className="text-[#6B7280]" style="font-size: 15px;">{{ r.l }}</span>
          <span className="font-bold text-[#111827] text-right" style="font-size: 16px;">{{ r.v }}</span>
        </div>
      </Card>
      <Btn variant="secondary" @click="store.navigate('voice-edit')">패턴 설명 음성 수정</Btn>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue';
import { useAppStore } from '../stores/appStore';
import SafeArea from '../components/common/SafeArea.vue';
import TopBar from '../components/common/TopBar.vue';
import Card from '../components/common/Card.vue';
import Btn from '../components/common/Btn.vue';
import Ic from '../components/common/Ic.vue';
import NavBar from '../components/common/NavBar.vue';
import BottomSheet from '../components/common/BottomSheet.vue';
import SheetRow from '../components/common/SheetRow.vue';

const props = defineProps({
  viewMode: { type: String, required: true }
});

const store = useAppStore();

const sheet = ref(false);
const sheetPat = ref(null);
const delConfirm = ref(false);

const drag = ref(null);
const justDropped = ref(false);
let longPressTimer = null;
const startPos = ref({ x: 0, y: 0 });
const LONG_PRESS_MS = 500;
const MOVE_THRESHOLD = 8;

const sortedPatterns = computed(() => [...store.patterns].sort((a, b) => a.num - b.num));
const ghostPat = computed(() => (drag.value ? store.patterns.find((p) => p.num === drag.value.sourceNum) : null));

const pDetail = computed(() => store.activePattern || {});
const detailPerson = computed(() => (pDetail.value.taskType === 'transfer' ? store.people.find((x) => x.id === pDetail.value.personId) : null));

const detailRows = computed(() => [
  detailPerson.value ? { l: "받는 사람", v: `${detailPerson.value.emoji} ${detailPerson.value.name} (${detailPerson.value.relation})` } : null,
  { l: "업무 유형", v: pDetail.value.taskType },
  { l: "단축번호", v: `${pDetail.value.num}번` },
  { l: "최근 사용", v: "오늘" }
].filter(Boolean));

function isDragSrc(num) { return drag.value?.sourceNum === num; }
function isDropTgt(num) { return drag.value?.targetNum === num && drag.value.sourceNum !== num; }

function findNumAtPoint(x, y) {
  const els = document.elementsFromPoint(x, y);
  for (const el of els) {
    const s = el.dataset?.slotNum;
    if (s) {
      const n = parseInt(s);
      if (n >= 1 && n <= 12) return n;
    }
  }
  return null;
}

// Document-level drag handlers
function onDocPointerMove(e) {
  if (!drag.value) return;
  const x = e.clientX, y = e.clientY;
  drag.value = { ...drag.value, ghostX: x, ghostY: y, targetNum: findNumAtPoint(x, y) };
}

function onDocPointerUp() {
  removeDocListeners();
  if (drag.value) {
    const { sourceNum, targetNum } = drag.value;
    const didReorder = targetNum !== null && targetNum !== sourceNum;
    if (didReorder) store.reorder(sourceNum, targetNum);
    drag.value = null;
    justDropped.value = didReorder;
  }
}

function onDocPointerCancel() {
  removeDocListeners();
  drag.value = null;
  justDropped.value = false;
}

function removeDocListeners() {
  document.removeEventListener('pointermove', onDocPointerMove);
  document.removeEventListener('pointerup', onDocPointerUp);
  document.removeEventListener('pointercancel', onDocPointerCancel);
}

function handlePointerDown(num, e) {
  e.preventDefault();
  const x = e.clientX, y = e.clientY;
  startPos.value = { x, y };

  longPressTimer = setTimeout(() => {
    longPressTimer = null;
    drag.value = { sourceNum: num, ghostX: x, ghostY: y, targetNum: null };
    document.addEventListener('pointermove', onDocPointerMove, { passive: true });
    document.addEventListener('pointerup', onDocPointerUp);
    document.addEventListener('pointercancel', onDocPointerCancel);
  }, LONG_PRESS_MS);
}

function handlePointerMove(e) {
  if (drag.value || longPressTimer === null) return;
  const dx = e.clientX - startPos.value.x;
  const dy = e.clientY - startPos.value.y;
  if (Math.hypot(dx, dy) > MOVE_THRESHOLD) {
    clearTimeout(longPressTimer);
    longPressTimer = null;
  }
}

function handlePointerUp() {
  if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null; }
}

function handlePointerCancel() {
  if (longPressTimer) { clearTimeout(longPressTimer); longPressTimer = null; }
}

function handleCardClick(p) {
  if (justDropped.value) { justDropped.value = false; return; }
  if (drag.value) return;
  store.activePattern = p;
  store.navigate('pattern-detail');
}

function editSheetPattern() {
  if (sheetPat.value) {
    store.editingId = sheetPat.value.id;
    sheet.value = false;
    store.navigate('pattern-register');
  }
}

onUnmounted(() => {
  if (longPressTimer) clearTimeout(longPressTimer);
  removeDocListeners();
});

function doDelete() {
  if (!sheetPat.value) return;
  const targetId = sheetPat.value.id;
  const targetLabel = sheetPat.value.label;
  const prev = [...store.patterns];
  store.setPatterns((ps) => ps.filter((p) => p.id !== targetId));
  delConfirm.value = false;
  sheet.value = false;
  store.showToast(`${targetLabel} 패턴을 삭제했어요.`, "되돌리기", () => {
    store.patterns = prev;
  });
}
</script>
