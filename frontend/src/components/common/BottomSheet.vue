<template>
  <div
    v-if="open"
    className="absolute inset-0 z-50 flex flex-col justify-end"
    style="background: rgba(0, 0, 0, 0.18)"
    @click="$emit('close')"
  >
    <div className="bg-white rounded-t-[28px] px-4 pt-3 pb-10" @click.stop>
      <div className="w-9 h-1 bg-[#E5E7EB] rounded-full mx-auto mb-3" />
      <p v-if="title" className="text-[20px] font-black text-[#111827] px-1 pb-2">{{ title }}</p>
      <slot />
    </div>
  </div>
</template>

<script setup>
defineProps({
  open: { type: Boolean, required: true },
  title: { type: String, default: null }
});

defineEmits(['close']);
</script>
