<template>
  <div className="space-y-4">
    <Card className="p-5 text-center border-2 border-[#F5B800]">
      <p className="break-keep text-[40px] font-black leading-tight text-[#111827]">{{ formatWon(val) }}</p>
      <p className="mt-2 min-h-6 break-keep text-[17px] font-bold text-[#6B7280]">
        {{ formatKoreanWon(val) || '금액을 입력해 주세요.' }}
      </p>
    </Card>
    <div className="flex gap-2">
      <button
        v-for="q in quickBtns"
        :key="q.l"
        @click="addN(q.n)"
        className="flex-1 min-h-12 rounded-xl bg-[#FFFBEB] border border-[#FDE68A] text-[14px] font-bold text-[#D97706]"
      >
        {{ q.l }}
      </button>
    </div>
    <div className="grid grid-cols-3 gap-2">
      <button
        v-for="(k, i) in keys"
        :key="i"
        @click="k === 'DEL' ? del() : k ? add(k) : undefined"
        :class="[
          'h-[54px] rounded-xl text-[22px] font-bold active:scale-95 flex items-center justify-center',
          k === '' ? 'bg-transparent pointer-events-none' : k === 'DEL' ? 'bg-[#F9FAFB] text-[#6B7280] border border-[#EBEBEA]' : 'bg-white border border-[#EBEBEA] text-[#111827]'
        ]"
      >
        <Ic v-if="k === 'DEL'" name="Del" />
        <template v-else>{{ k }}</template>
      </button>
    </div>
    <Btn :disabled="val === '0'" @click="$emit('complete', val)">다음</Btn>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import Card from './Card.vue';
import Btn from './Btn.vue';
import Ic from './Ic.vue';
import { formatKoreanWon, formatWon } from '../../utils/money.js';

defineEmits(['complete']);

const props = defineProps({
  initialValue: { type: [String, Number], default: '0' },
});

const initialAmount = Number(props.initialValue);
const val = ref(Number.isInteger(initialAmount) && initialAmount > 0
  ? String(Math.min(initialAmount, 999999999))
  : '0');
const quickBtns = [
  { l: "+1만원", n: 10000 },
  { l: "+5만원", n: 50000 },
  { l: "+10만원", n: 100000 }
];
const keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "DEL"];

function add(d) {
  val.value = (val.value === "0" ? d : val.value + d).slice(0, 9);
}

function del() {
  val.value = val.value.length > 1 ? val.value.slice(0, -1) : "0";
}

function addN(n) {
  val.value = String(Math.min(parseInt(val.value || "0") + n, 999999999));
}
</script>
