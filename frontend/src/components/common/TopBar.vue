<template>
  <div className="flex items-center justify-between px-4 bg-white border-b border-[#EEEEED] flex-shrink-0" style="min-height: 56px; padding-top: 10px; padding-bottom: 10px;">
    <button v-if="onBack" @click="onBack" className="w-11 h-11 flex items-center justify-center text-[#374151] rounded-full active:bg-[#F3F4F6]">
      <Ic name="Back" />
    </button>
    <div v-else className="w-11" />

    <span className="font-bold text-[#111827]" style="font-size: 19px;">{{ title }}</span>

    <button v-if="rightLabel" @click="onRight" className="font-medium text-[#6B7280] px-2 h-11 flex items-center" style="font-size: 16px;">
      {{ rightLabel }}
    </button>
    <div v-else className="w-11" />
  </div>
</template>

<script setup>
import Ic from './Ic.vue';

defineProps({
  title: { type: String, required: true },
  onBack: { type: Function, default: null },
  rightLabel: { type: String, default: null },
  onRight: { type: Function, default: null }
});
</script>
