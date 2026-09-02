<template>
  <div class="space-y-5">
    <p class="text-[21px] font-black text-[#111827]">새로운 가족 등록</p>

    <!-- Emoji -->
    <div class="space-y-2">
      <p class="text-[13px] font-bold text-[#374151] px-1">이모지 선택</p>
      <div class="flex gap-2 flex-wrap">
        <button
          v-for="e in EMOJI_OPTIONS"
          :key="e"
          @click="emoji = e"
          :class="['w-12 h-12 rounded-full text-[24px] border-2 transition-all', emoji === e ? 'border-[#F5B800] bg-[#FFFBEB]' : 'border-[#E5E7EB]']"
        >{{ e }}</button>
      </div>
    </div>

    <!-- Name -->
    <div class="space-y-2">
      <p class="text-[13px] font-bold text-[#374151] px-1">이름</p>
      <input
        v-model="name"
        placeholder="예: 김민준"
        class="w-full min-h-[52px] rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[17px] font-bold text-[#111827] placeholder:text-[#D1D5DB]"
      />
    </div>

    <!-- Relation -->
    <div class="space-y-2">
      <p class="text-[13px] font-bold text-[#374151] px-1">관계</p>
      <input
        v-model="relation"
        placeholder="예: 아들, 딸, 사위, 며느리"
        class="w-full min-h-[52px] rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[17px] font-bold text-[#111827] placeholder:text-[#D1D5DB]"
      />
    </div>

    <!-- Accounts -->
    <div class="space-y-3">
      <p class="text-[14px] font-bold text-[#374151] px-1">계좌 정보</p>

      <div
        v-for="(acc, idx) in accounts"
        :key="idx"
        class="rounded-[16px] border-2 border-[#E5E7EB] p-4 space-y-3"
      >
        <div class="flex items-center justify-between">
          <span class="text-[13px] font-bold text-[#6B7280]">계좌 {{ idx + 1 }}</span>
          <button
            v-if="accounts.length > 1"
            @click="removeAccount(idx)"
            class="text-[#EF4444] text-[13px] font-bold"
          >삭제</button>
        </div>

        <!-- Bank -->
        <div class="space-y-1.5">
          <p class="text-[12px] font-bold text-[#374151] px-1">은행 선택</p>
          <button
            @click="acc.showBanks = !acc.showBanks"
            :class="[
              'w-full min-h-[48px] rounded-[12px] border-2 px-4 text-left text-[15px] font-bold flex items-center justify-between transition-all',
              acc.bank ? 'border-[#F5B800] text-[#111827]' : 'border-[#E5E7EB] text-[#9CA3AF]'
            ]"
          >
            <span>{{ acc.bank || '은행 선택' }}</span>
            <span :class="['transition-transform', acc.showBanks ? 'rotate-180' : '']">▾</span>
          </button>
          <div v-if="acc.showBanks" class="grid grid-cols-2 gap-2 pt-1">
            <button
              v-for="b in BANKS"
              :key="b"
              @click="acc.bank = b; acc.showBanks = false;"
              :class="[
                'h-[44px] rounded-[10px] border-2 text-[14px] font-bold transition-all',
                acc.bank === b ? 'border-[#F5B800] bg-[#FFFBEB] text-[#D97706]' : 'border-[#E5E7EB] text-[#374151]'
              ]"
            >{{ b }}</button>
          </div>
        </div>

        <!-- Account Number -->
        <div class="space-y-1.5">
          <p class="text-[12px] font-bold text-[#374151] px-1">계좌 번호</p>
          <input
            type="tel"
            v-model="acc.number"
            @input="acc.number = $event.target.value.replace(/[^0-9-]/g, '')"
            placeholder="000-00-000000"
            inputmode="numeric"
            class="w-full min-h-[48px] rounded-[12px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[16px] font-bold text-[#111827] placeholder:text-[#D1D5DB]"
          />
        </div>

        <!-- Alias (optional) -->
        <div class="space-y-1.5">
          <p class="text-[12px] font-bold text-[#374151] px-1">계좌 별칭 <span class="text-[#9CA3AF] font-medium">(선택)</span></p>
          <input
            v-model="acc.alias"
            placeholder="예: 주거래 계좌, 생활비 계좌"
            class="w-full min-h-[48px] rounded-[12px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[15px] font-bold text-[#111827] placeholder:text-[#D1D5DB]"
          />
        </div>
      </div>

      <button
        @click="addAccount"
        class="w-full min-h-[52px] rounded-[16px] border-2 border-dashed border-[#F5B800] text-[#D97706] text-[15px] font-bold flex items-center justify-center gap-2"
      >
        <span class="text-[18px]">+</span> 계좌 추가
      </button>
    </div>

    <!-- Buttons -->
    <div class="flex gap-3 pt-2">
      <button
        @click="emit('cancel')"
        class="flex-1 min-h-[56px] rounded-[18px] border-2 border-[#E5E7EB] text-[16px] font-bold text-[#6B7280] active:bg-[#F9FAFB] transition-all"
      >취소</button>
      <button
        :disabled="!canSave"
        @click="handleSave"
        :class="[
          'flex-1 min-h-[56px] rounded-[18px] text-[16px] font-bold transition-all',
          canSave ? 'bg-[#F5B800] text-[#111827] active:bg-[#D97706]' : 'bg-[#E5E7EB] text-[#9CA3AF] cursor-not-allowed'
        ]"
      >저장</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue';
import { useAppStore } from '../../stores/appStore';

const emit = defineEmits(['saved', 'cancel']);

const EMOJI_OPTIONS = ["👨", "👩", "🧑", "👴", "👵", "👦", "👧", "🧓"];
const BANKS = ["KB국민은행", "신한은행", "우리은행", "하나은행", "NH농협", "카카오뱅크", "케이뱅크", "토스뱅크"];
const BANK_COLORS = {
  "KB국민은행": "#F5B800",
  "신한은행": "#0077CC",
  "우리은행": "#006EAF",
  "하나은행": "#009B77",
  "NH농협": "#007B40",
  "카카오뱅크": "#3A1D1D",
  "케이뱅크": "#FFB800",
  "토스뱅크": "#0064FF",
};

const store = useAppStore();

const emoji = ref("👤");
const name = ref("");
const relation = ref("");
const accounts = ref([{ bank: "", number: "", alias: "", showBanks: false }]);

const canSave = computed(() => name.value.trim() && relation.value.trim());

function addAccount() {
  accounts.value.push({ bank: "", number: "", alias: "", showBanks: false });
}

function removeAccount(idx) {
  accounts.value.splice(idx, 1);
}

function maskNumber(raw) {
  const digits = raw.replace(/\D/g, "");
  if (digits.length < 6) return raw || "****";
  const visible = digits.slice(-2);
  const prefix = digits.slice(0, Math.min(3, digits.length - 6));
  return `${prefix}-**-****-${visible}`;
}

function handleSave() {
  if (!canSave.value) return;
  const personId = store.addPerson(name.value.trim(), relation.value.trim(), emoji.value);
  const filledAccounts = accounts.value.filter(a => a.bank && a.number);
  filledAccounts.forEach((acc, idx) => {
    store.addAccount(personId, {
      bank: acc.bank,
      color: BANK_COLORS[acc.bank] || "#9CA3AF",
      nickname: acc.alias || acc.bank,
      masked: maskNumber(acc.number),
      primary: idx === 0,
    });
  });
  emit("saved", personId);
}
</script>
