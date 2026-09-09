<template>
  <div class="px-4 py-3 flex items-center gap-3 bg-white border-b border-[#F0F0EE] flex-shrink-0">
    <span class="text-[14px] font-black text-[#6B7280] whitespace-nowrap">
      {{ prefix }} {{ current }}단계<template v-if="label"> · {{ label }}</template>
    </span>
    <div class="flex-1 h-[6px] bg-[#F3F4F6] rounded-full overflow-hidden">
      <div class="h-full rounded-full transition-all duration-500 bg-[#FFBC00]" :style="{ width: `${(current / total) * 100}%` }" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  current: { type: Number, required: true },
  total: { type: Number, required: true },
  label: { type: String, default: '' },
  prefix: { type: String, default: '송금' }
});
</script>
