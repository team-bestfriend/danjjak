<template>
  <div className="flex bg-[#F3F4F6] p-1 rounded-[14px] gap-1" role="group" :aria-label="label">
    <button
      v-for="o in options"
      :key="o.key"
      type="button"
      :aria-pressed="value === o.key"
      :disabled="disabled"
      @click="$emit('update:value', o.key)"
      :class="[
        'flex-1 min-w-0 min-h-12 rounded-[11px] px-1 py-2 text-[15px] font-bold transition-all disabled:opacity-50',
        value === o.key ? 'bg-white text-[#111827] shadow-sm' : 'text-[#6B7280]'
      ]"
    >
      <span class="flex items-center justify-center gap-2">
        <img v-if="o.icon" :src="o.icon" alt="" class="h-10 w-10 shrink-0 object-contain" aria-hidden="true" />
        <span>{{ o.label }}</span>
      </span>
    </button>
  </div>
</template>

<script setup>
defineProps({
  options: { type: Array, required: true },
  value: { type: String, required: true },
  label: { type: String, default: '옵션 선택' },
  disabled: Boolean,
});

defineEmits(['update:value']);
</script>
