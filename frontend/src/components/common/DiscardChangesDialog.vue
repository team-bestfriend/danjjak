<template>
  <dialog
    ref="dialog"
    :aria-labelledby="titleId"
    :aria-describedby="descriptionId"
    class="m-auto max-h-[calc(100%_-_32px)] w-[calc(100%_-_32px)] max-w-[360px] overflow-y-auto border-0 bg-transparent p-0"
    @cancel.prevent="emit('resolve', false)"
    @click.self="emit('resolve', false)"
    @keydown.tab.prevent="cycleFocus"
  >
    <div class="rounded-[24px] bg-white p-5 text-center shadow-xl">
      <h2 :id="titleId" class="break-keep text-[22px] font-bold text-[#111827]">{{ title }}</h2>
      <p :id="descriptionId" class="mt-3 break-keep text-[16px] leading-relaxed text-[#6B7280]">{{ description }}</p>
      <div class="mt-5 grid grid-cols-2 gap-3">
        <button ref="keepButton" type="button" class="min-h-[48px] rounded-[12px] border border-[#D1D5DB] bg-white px-3 py-3 text-[17px] font-bold text-[#374151]" @click="emit('resolve', false)">
          계속 작성
        </button>
        <button ref="discardButton" type="button" class="min-h-[48px] rounded-[12px] bg-[#DC2626] px-3 py-3 text-[17px] font-bold text-white" @click="emit('resolve', true)">
          {{ confirmLabel }}
        </button>
      </div>
    </div>
  </dialog>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, useId } from 'vue';

defineProps({
  title: { type: String, default: '작성을 그만둘까요?' },
  description: { type: String, default: '저장하지 않은 변경사항은 사라져요.' },
  confirmLabel: { type: String, default: '나가기' },
});
const emit = defineEmits(['resolve']);
const dialog = ref(null);
const keepButton = ref(null);
const discardButton = ref(null);
const titleId = useId();
const descriptionId = useId();

function cycleFocus() {
  (document.activeElement === keepButton.value ? discardButton.value : keepButton.value)?.focus();
}

onMounted(() => {
  // 모달 밖 조작과 초점 이동을 막고, 기본 초점은 초안 유지 버튼에 둔다.
  dialog.value.showModal();
  keepButton.value.focus();
});
onBeforeUnmount(() => dialog.value?.close());
</script>

<style scoped>
dialog::backdrop {
  background: rgba(0, 0, 0, 0.45);
}
</style>
