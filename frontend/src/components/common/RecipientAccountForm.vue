<template>
  <form class="space-y-3" @submit.prevent="handleSave">
    <p class="text-[24px] font-extrabold leading-[1.35] text-[#111827]">
      {{
        existingAccount
          ? "받는 계좌를 수정해주세요"
          : "받는 계좌를 추가해주세요"
      }}
    </p>
    <p class="text-[15px] text-[#6B7280]">
      {{ personName }} 님에게 연결할 계좌 정보를 입력해 주세요.
    </p>

    <div class="space-y-2">
      <span class="block pb-1 text-[18px] font-bold text-[#111827]">은행</span>
      <button
        type="button"
        :aria-invalid="Boolean(fieldErrors.bank)"
        aria-describedby="recipient-account-bank-error"
        @click="
          touched.bank = true;
          showBanks = !showBanks;
        "
        :class="[
          'w-full min-h-[58px] rounded-[14px] mb-6 border-2 px-4 text-left text-[18px] font-normal flex items-center justify-between',
          selectedBank
            ? 'border-[#F5B800] text-[#111827]'
            : 'border-[#E5E7EB] text-[#9CA3AF]',
        ]"
      >
        <span>{{ selectedBank?.name || "은행 선택" }}</span>
        <span>▾</span>
      </button>
      <p
        v-if="fieldErrors.bank"
        id="recipient-account-bank-error"
        class="text-[13px] text-[#B91C1C]"
        role="alert"
      >
        {{ fieldErrors.bank }}
      </p>
      <div v-if="showBanks" class="grid grid-cols-2 gap-2">
        <button
          v-for="bank in BANKS"
          :key="bank.code"
          type="button"
          @click="
            bankCode = bank.code;
            showBanks = false;
          "
          :class="[
            'min-h-[54px] rounded-[14px] mb-2 border-2 text-[17px] font-normal',
            bankCode === bank.code
              ? 'border-[#F5B800] bg-[#FFFBEB] text-[#92650A]'
              : 'border-[#E5E7EB] text-[#374151]',
          ]"
        >
          {{ bank.name }}
        </button>
      </div>
    </div>

    <label class="block space-y-2">
      <span class="block pb-1 text-[18px] font-bold text-[#111827]"
        >계좌 번호</span
      >
      <input
        id="recipient-account-number"
        type="tel"
        :value="accountNumber"
        :aria-invalid="Boolean(fieldErrors.account)"
        aria-describedby="recipient-account-number-help recipient-account-number-error"
        @input="accountNumber = $event.target.value.replace(/[^0-9-]/g, '')"
        @blur="touched.account = true"
        maxlength="50"
        placeholder="000-00-000000"
        inputmode="numeric"
        class="w-full min-h-[58px] mb-2 rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[18px] font-normal placeholder:text-[#9CA3AF] placeholder:font-normal"
      />
      <p
        id="recipient-account-number-help"
        class="text-[13px] mb-6 text-[#6B7280]"
      >
        숫자 8~20자와 숫자 사이의 하이픈만 입력할 수 있어요.
      </p>
      <p
        v-if="fieldErrors.account"
        id="recipient-account-number-error"
        class="text-[13px] text-[#B91C1C]"
        role="alert"
      >
        {{ fieldErrors.account }}
      </p>
    </label>

    <label class="block space-y-2">
      <span class="block pb-1 text-[18px] font-bold text-[#111827]">
        계좌 별칭
        <span class="text-[16px] font-normal text-[#9CA3AF]">(선택)</span>
      </span>
      <input
        v-model.trim="accountAlias"
        maxlength="50"
        placeholder="예: 민수 생활비"
        class="w-full min-h-[58px] rounded-[14px] border-2 border-[#E5E7EB] focus:border-[#F5B800] outline-none px-4 text-[18px] font-normal placeholder:text-[#9CA3AF] placeholder:font-normal"
      />
    </label>

    <p
      v-if="formError"
      class="rounded-xl bg-[#FEF2F2] p-3 text-[#991B1B]"
      role="alert"
    >
      {{ formError }}
    </p>

    <div class="flex gap-3 pt-2">
      <button
        type="button"
        :disabled="saving"
        @click="emit('cancel')"
        class="flex-1 min-h-[58px] rounded-[18px] border-2 border-[#E5E7EB] text-[17px] font-bold text-[#6B7280] disabled:opacity-50"
      >
        취소
      </button>
      <button
        type="submit"
        :disabled="!canSave || saving"
        :class="[
          'flex-1 min-h-[58px] rounded-[18px] text-[17px] font-bold',
          canSave && !saving
            ? 'bg-[#F5B800] text-[#111827]'
            : 'bg-[#E5E7EB] text-[#9CA3AF]',
        ]"
      >
        {{ saving ? "저장 중…" : "저장" }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { computed, ref } from "vue";
import { ApiError } from "../../api/httpClient";
import { BANKS, findBankByName } from "../../constants/banks";
import { useAppStore } from "../../stores/appStore";

const props = defineProps({
  registeredPersonId: { type: Number, required: true },
  personName: { type: String, required: true },
  existingAccount: { type: Object, default: null },
});
const emit = defineEmits(["saved", "cancel"]);
const store = useAppStore();
const matchedBank =
  BANKS.find((bank) => bank.code === props.existingAccount?.bankCode) ??
  findBankByName(props.existingAccount?.bankName);
const bankCode = ref(matchedBank?.code ?? "");
const accountNumber = ref(props.existingAccount?.accountNumber ?? "");
const accountAlias = ref(props.existingAccount?.accountAlias ?? "");
const showBanks = ref(false);
const saving = ref(false);
const formError = ref("");
const touched = ref({ bank: false, account: false });
const selectedBank = computed(
  () => BANKS.find((bank) => bank.code === bankCode.value) ?? null,
);
const validAccountNumber = computed(() =>
  /^(?=(?:[0-9]-?){8,20}$)[0-9]+(?:-[0-9]+)*$/.test(accountNumber.value),
);
const canSave = computed(
  () => Boolean(selectedBank.value) && validAccountNumber.value,
);
const fieldErrors = computed(() => ({
  bank:
    touched.value.bank && !selectedBank.value ? "은행을 선택해 주세요." : "",
  account:
    touched.value.account && !validAccountNumber.value
      ? "계좌번호는 숫자 8~20자와 숫자 사이의 하이픈만 입력해 주세요."
      : "",
}));

async function handleSave() {
  if (!canSave.value || saving.value) {
    formError.value = "은행과 계좌번호를 모두 확인해 주세요.";
    return;
  }
  saving.value = true;
  formError.value = "";
  try {
    const saved = await store.saveRecipientAccount(
      props.registeredPersonId,
      {
        bankCode: selectedBank.value.code,
        bankName: selectedBank.value.name,
        accountNumber: accountNumber.value,
        accountAlias: accountAlias.value || null,
      },
      props.existingAccount?.accountId ?? null,
    );
    emit("saved", saved.registeredPersonId);
  } catch (error) {
    formError.value =
      error instanceof ApiError
        ? error.message
        : "계좌 정보를 저장하지 못했습니다. 다시 시도해 주세요.";
  } finally {
    saving.value = false;
  }
}
</script>
