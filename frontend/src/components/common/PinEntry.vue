<template>
  <div className="space-y-5">
    <div className="flex justify-center gap-5">
      <div
        v-for="(_, i) in 4"
        :key="i"
        :class="[
          'w-16 h-16 rounded-full border-2 flex items-center justify-center transition-all',
          i < digits.length ? 'bg-[#F5B800] border-[#F5B800]' : 'bg-white border-[#E5E7EB]'
        ]"
      >
        <div v-if="i < digits.length" className="w-4 h-4 rounded-full bg-white" />
      </div>
    </div>
    <div className="grid grid-cols-3 gap-2.5">
      <button
        v-for="(k, i) in keys"
        :key="i"
        @click="k === 'DEL' ? del() : k ? add(k) : undefined"
        :class="[
          'h-[62px] rounded-2xl text-[24px] font-bold transition-all active:scale-95 flex items-center justify-center',
          k === '' ? 'bg-transparent pointer-events-none' : k === 'DEL' ? 'bg-[#F9FAFB] text-[#6B7280] border border-[#EBEBEA]' : 'bg-white border border-[#EBEBEA] text-[#111827]'
        ]"
      >
        <Ic v-if="k === 'DEL'" name="Del" />
        <template v-else>{{ k }}</template>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import Ic from './Ic.vue';

const emit = defineEmits(['complete']);
const digits = ref([]);
const keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "DEL"];

function add(d) {
  if (digits.value.length < 4) {
    digits.value.push(d);
    if (digits.value.length === 4) {
      setTimeout(() => {
        digits.value = [];
        emit('complete');
      }, 400);
    }
  }
}

function del() {
  digits.value.pop();
}
</script>
