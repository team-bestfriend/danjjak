<template>
  <div class="nickname-view flex h-full flex-col bg-[#FAFAF8] px-6">
    <main class="min-h-0 flex-1 pt-5">
      <!-- 할머니 프로필 이미지 -->
      <div
        class="flex h-[138px] w-[138px] items-center justify-center overflow-hidden rounded-full border-[5px] border-[#FFBC00] bg-[#FFF3CC]"
      >
        <img
          :src="oldAgeWomanImage"
          alt=""
          class="h-[118px] w-[118px] object-contain"
          aria-hidden="true"
        />
      </div>

      <h1
        class="mt-10 text-[32px] font-extrabold leading-[1.35] tracking-[-0.03em] text-[#111827]"
      >
        어떻게 불러드릴까요?
      </h1>

      <p class="mt-3 text-[17px] leading-relaxed text-[#6B7280]">
        편하게 부르실 이름을 입력해주세요.
      </p>

      <!-- 추천 이름 -->
      <button
        type="button"
        class="mt-8 flex min-h-[94px] w-full items-center rounded-[22px] border-2 px-7 text-left transition"
        :class="
          !customInputVisible
            ? 'border-[#FFBC00] bg-[#FFF9E8]'
            : 'border-[#E1E5EB] bg-white'
        "
        @click="selectSuggestedName"
      >
        <span class="text-[25px] font-bold text-[#111827]">
          {{ suggestedName }}님
        </span>
      </button>

      <!-- 직접 입력 영역 -->
      <div v-if="customInputVisible" class="mt-5">
        <label
          for="nickname"
          class="mb-2 block text-[16px] font-bold text-[#374151]"
        >
          사용할 이름
        </label>

        <input
          id="nickname"
          ref="nicknameInput"
          v-model="customName"
          type="text"
          maxlength="10"
          autocomplete="nickname"
          placeholder="예: 순자"
          class="h-[66px] w-full rounded-[18px] border-2 border-[#FFBC00] bg-white px-5 text-[21px] font-bold text-[#111827] outline-none placeholder:font-normal placeholder:text-[#C4C8CF]"
          @keyup.enter="saveNickname"
        />

        <p class="mt-2 text-right text-[14px] text-[#9CA3AF]">
          {{ customName.trim().length }} / 10
        </p>
      </div>

      <p
        v-if="errorMessage"
        class="mt-4 rounded-[14px] bg-[#FEF2F2] p-4 text-[15px] text-[#B91C1C]"
        role="alert"
      >
        {{ errorMessage }}
      </p>
    </main>

    <!-- 하단 버튼 -->
    <div class="space-y-3 pb-9">
      <button
        type="button"
        class="h-[64px] w-full rounded-[20px] border border-[#D1D5DB] bg-white text-[19px] font-medium text-[#374151] transition active:scale-[0.98]"
        @click="showCustomInput"
      >
        {{ customInputVisible ? "추천 이름 사용" : "다른 이름 입력" }}
      </button>

      <button
        type="button"
        class="h-[66px] w-full rounded-[20px] text-[21px] font-bold transition active:scale-[0.98] disabled:cursor-not-allowed"
        :class="
          canSave
            ? 'bg-[#FFBC00] text-[#111827]'
            : 'bg-[#F0F1F3] text-[#A7ADBA]'
        "
        :disabled="!canSave || saving"
        @click="saveNickname"
      >
        {{ saving ? "저장 중…" : "좋아요" }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from "vue";
import { useRouter } from "vue-router";
import oldAgeWomanImage from "../assets/icons/profile/old_age_woman.png";
import { useAppStore } from "../stores/appStore";

const router = useRouter();
const store = useAppStore();

const suggestedName = computed(() => {
  return store.currentUser?.name?.trim() || "순자";
});

const selectedName = ref(suggestedName.value);
const customName = ref("");
const customInputVisible = ref(false);
const nicknameInput = ref(null);
const saving = ref(false);
const errorMessage = ref("");

const finalName = computed(() => {
  return customInputVisible.value
    ? customName.value.trim()
    : selectedName.value.trim();
});

const canSave = computed(() => {
  const name = finalName.value;

  return name.length >= 1 && name.length <= 10;
});

function selectSuggestedName() {
  selectedName.value = suggestedName.value;
  customInputVisible.value = false;
  customName.value = "";
  errorMessage.value = "";
}

async function showCustomInput() {
  if (customInputVisible.value) {
    selectSuggestedName();
    return;
  }

  customInputVisible.value = true;
  customName.value = "";
  errorMessage.value = "";

  await nextTick();
  nicknameInput.value?.focus();
}

async function saveNickname() {
  if (!canSave.value || saving.value) return;

  saving.value = true;
  errorMessage.value = "";

  try {
    store.saveUserName(finalName.value);

    const firstStartFlow =
      sessionStorage.getItem("danjjakPrivacyConsent") !== null;

    /*
     * 처음 시작하기 흐름에서는 이전에 동의한 계정이라도
     * 기존 카카오 알림 동의 화면을 거치도록 합니다.
     */
    if (firstStartFlow) {
      await router.replace({
        name: "consent",
        query: {
          onboarding: "1",
        },
      });

      return;
    }

    /*
     * 다시 이용하기 흐름의 기본 이동 처리입니다.
     */
    const nextRoute = store.currentUser?.consents?.completed
      ? store.currentUser?.accountReady
        ? "home"
        : "account-import"
      : "consent";

    await router.replace({
      name: nextRoute,
    });
  } catch (error) {
    errorMessage.value =
      error?.message ?? "이름을 저장하지 못했습니다. 다시 시도해 주세요.";
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.nickname-view {
  box-sizing: border-box;
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
