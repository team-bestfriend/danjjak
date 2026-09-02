<template>
  <div style="display: grid; grid-template-columns: 1fr 1fr; grid-template-rows: 1fr 1fr; gap: 12px; height: 100%;">
    <div
      v-for="(num, idx) in pageNums"
      :key="num"
      :data-slot-num="num"
      @pointerdown="handlePointerDown(num, $event)"
      @pointermove="handlePointerMove"
      @pointerup="handlePointerUp"
      @pointercancel="handlePointerCancel"
      @click="handleCardClick(num)"
      className="select-none transition-all cursor-pointer"
      :style="{
        minHeight: '120px',
        borderRadius: '18px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '8px',
        padding: '16px 12px',
        touchAction: 'none',
        backgroundColor: getPattern(num) ? getPattern(num).color : '#F5F5F3',
        border: getPattern(num) ? 'none' : '1.5px dashed #D1D5DB',
        opacity: isDragSrc(num) ? 0.25 : 1,
        transform: isDragSrc(num) ? 'scale(0.95)' : isDropTgt(num) ? 'scale(1.04)' : 'scale(1)',
        boxShadow: isDropTgt(num) ? '0 0 0 3px rgba(0,0,0,0.20)' : 'none',
      }"
    >
      <span
        style="font-size: 54px; font-weight: 700; line-height: 1;"
        :style="{ color: getPattern(num) ? 'rgba(255,255,255,0.85)' : '#D1D5DB' }"
      >{{ num }}</span>
      <span
        style="font-size: 19px; font-weight: 600; word-break: keep-all; line-height: 1.3; text-align: center;"
        :style="{ color: getPattern(num) ? '#ffffff' : '#AEAEAE' }"
      >{{ getPattern(num) ? getPattern(num).label : '패턴 등록' }}</span>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  pageNums: { type: Array, required: true },
  patterns: { type: Array, required: true },
  dragState: { type: Object, default: null }
});

const emit = defineEmits(['pointer-down', 'pointer-move', 'pointer-up', 'pointer-cancel', 'card-click']);

function getPattern(num) {
  return props.patterns.find((x) => x.num === num);
}
function isDragSrc(num) { return props.dragState?.sourceNum === num; }
function isDropTgt(num) { return props.dragState?.targetNum === num && props.dragState.sourceNum !== num; }
function handlePointerDown(num, e) { emit('pointer-down', num, e); }
function handlePointerMove(e) { emit('pointer-move', e); }
function handlePointerUp(e) { emit('pointer-up', e); }
function handlePointerCancel() { emit('pointer-cancel'); }
function handleCardClick(num) { emit('card-click', num, getPattern(num)); }
</script>
