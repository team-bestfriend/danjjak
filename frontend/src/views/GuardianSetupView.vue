<template>
  <div class="guardian-view flex h-full flex-col bg-[#FAFAF8] px-6">
    <main class="min-h-0 flex-1 overflow-y-auto pb-6 pt-5">
      <!-- 상단 이미지 -->
      <div class="flex justify-center">
        <div
          class="flex h-[180px] w-[180px] items-center justify-center overflow-hidden rounded-full border-[5px] border-[#FFC7CA] bg-[#FFF1F2]"
        >
          <img
            :src="warningImage"
            alt=""
            class="h-[150px] w-[150px] object-contain"
            aria-hidden="true"
          />
        </div>
      </div>

      <h1
        class="mt-9 text-[32px] font-extrabold leading-[1.45] tracking-[-0.03em] text-[#111827]"
      >
        가족과 함께<br />
        안전을 지켜요
      </h1>

      <!-- 안내 상자 -->
      <div
        class="mt-7 flex items-start gap-4 rounded-[20px] border border-[#FED7AA] bg-[#FFF7ED] px-5 py-5"
      >
        <span class="shrink-0 text-[24px]" aria-hidden="true"> 💡 </span>

        <p
          class="break-keep text-[16px] font-medium leading-relaxed text-[#9A3412]"
        >
          보호자는 이상 거래 알림을 받고 언제든 전화로 도와줄 수 있어요.
        </p>
      </div>

      <!-- 전화번호 입력 -->
      <div class="mt-8">
        <label
          for="guardian-phone"
          class="block text-[19px] font-bold text-[#111827]"
        >
          보호자 전화번호
        </label>

        <input
          id="guardian-phone"
          v-model="phoneNumber"
          type="tel"
          inputmode="numeric"
          autocomplete="tel"
          maxlength="13"
          placeholder="010-0000-0000"
          class="mt-4 h-[66px] w-full rounded-[20px] border-2 border-[#E1E5EB] bg-white px-5 text-[22px] font-bold text-[#111827] outline-none transition placeholder:font-medium placeholder:text-[#D1D5DB] focus:border-[#FFBC00]"
          :aria-invalid="Boolean(errorMessage)"
          @input="handlePhoneInput"
          @keyup.enter="saveGuardian"
        />

        <p
          v-if="errorMessage"
          class="mt-3 text-[15px] text-[#B91C1C]"
          role="alert"
        >
          {{ errorMessage }}
        </p>
      </div>
    </main>

    <!-- 하단 버튼 -->
    <div class="space-y-3 pb-9 pt-5">
      <button
        type="button"
        class="h-[66px] w-full rounded-[20px] text-[20px] font-bold transition active:scale-[0.98] disabled:cursor-not-allowed"
        :class="
          canSave && !store.guardianSaving
            ? 'bg-[#FFBC00] text-[#111827]'
            : 'bg-[#F0F1F3] text-[#A7ADBA]'
        "
        :disabled="!canSave || store.guardianSaving"
        @click="saveGuardian"
      >
        {{ store.guardianSaving ? "등록 중…" : "보호자 전화번호 등록하기" }}
      </button>

      <button
        type="button"
        class="h-[58px] w-full rounded-[18px] text-[18px] font-medium text-[#9CA3AF] transition active:bg-[#F3F4F6]"
        :disabled="store.guardianSaving"
        @click="skipGuardian"
      >
        나중에 등록할게요
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import warningImage from "../assets/icons/warning.png";
import { useAppStore } from "../stores/appStore";

const router = useRouter();
const store = useAppStore();

const phoneNumber = ref("");
const errorMessage = ref("");

const canSave = computed(() => {
  return /^010-\d{4}-\d{4}$/.test(phoneNumber.value);
});

function formatPhoneNumber(value) {
  const digits = value.replace(/\D/g, "").slice(0, 11);

  if (digits.length <= 3) {
    return digits;
  }

  if (digits.length <= 7) {
    return `${digits.slice(0, 3)}-${digits.slice(3)}`;
  }

  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
}

function handlePhoneInput(event) {
  phoneNumber.value = formatPhoneNumber(event.target.value);
  errorMessage.value = "";
}

function getNextRoute() {
  return store.currentUser?.accountReady ? "home" : "account-import";
}

function finishFirstStartFlow() {
  sessionStorage.removeItem("danjjakPrivacyConsent");
}

async function saveGuardian() {
  if (!canSave.value || store.guardianSaving) return;

  errorMessage.value = "";

  try {
    await store.saveGuardian(phoneNumber.value);

    finishFirstStartFlow();

    await router.replace({
      name: getNextRoute(),
    });
  } catch (error) {
    errorMessage.value =
      error?.message ??
      "보호자 전화번호를 등록하지 못했습니다. 다시 시도해 주세요.";
  }
}

async function skipGuardian() {
  if (store.guardianSaving) return;

  finishFirstStartFlow();

  await router.replace({
    name: getNextRoute(),
  });
}
</script>

<style scoped>
.guardian-view {
  box-sizing: border-box;
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
