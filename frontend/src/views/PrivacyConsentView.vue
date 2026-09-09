<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />

    <main class="min-h-0 flex-1 overflow-y-auto px-6 pb-6 pt-8">
      <h1 class="text-[32px] font-extrabold leading-[1.35] text-[#111827]">
        서비스 이용을 위해<br />
        동의해주세요.
      </h1>

      <!-- 전체 동의 -->
      <button
        type="button"
        class="mt-8 flex min-h-[94px] w-full items-center gap-5 rounded-[22px] border-2 bg-white px-6 text-left transition"
        :class="
          allAgreed ? 'border-[#FFBC00] bg-[#FFF9E8]' : 'border-[#E1E5EB]'
        "
        :aria-pressed="allAgreed"
        @click="toggleAll"
      >
        <span
          class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full border-2 text-[24px] font-bold transition"
          :class="
            allAgreed
              ? 'border-[#FFBC00] bg-[#FFBC00] text-[#111827]'
              : 'border-[#D5DAE1] bg-white text-transparent'
          "
          aria-hidden="true"
        >
          ✓
        </span>

        <span class="text-[20px] font-bold text-[#111827]">
          전체 동의 (필수)
        </span>
      </button>

      <!-- 개별 약관 -->
      <div
        class="mt-7 overflow-hidden rounded-[22px] border border-[#E1E5EB] bg-white"
      >
        <div
          v-for="(term, index) in terms"
          :key="term.id"
          class="flex min-h-[90px] items-center gap-3 px-6"
          :class="{ 'border-t border-[#ECEEF1]': index > 0 }"
        >
          <button
            type="button"
            class="flex min-w-0 flex-1 items-center gap-4 text-left"
            :aria-pressed="term.agreed"
            :aria-label="`${term.label} ${term.agreed ? '동의함' : '동의하지 않음'}`"
            @click="toggleTerm(term.id)"
          >
            <span
              class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border-2 text-[20px] font-bold transition"
              :class="
                term.agreed
                  ? 'border-[#FFBC00] bg-[#FFBC00] text-[#111827]'
                  : 'border-[#D5DAE1] bg-white text-transparent'
              "
              aria-hidden="true"
            >
              ✓
            </span>

            <span class="break-keep text-[17px] font-medium text-[#374151]">
              {{ term.label }} (필수)
            </span>
          </button>

          <button
            type="button"
            class="min-h-12 shrink-0 px-1 text-[16px] font-medium text-[#9CA3AF] underline underline-offset-4"
            @click="openDetail(term)"
          >
            자세히
          </button>
        </div>
      </div>
    </main>

    <!-- 다음 버튼 -->
    <div class="border-t border-[#EEEEED] bg-white px-6 pb-9 pt-5">
      <button
        type="button"
        class="h-[66px] w-full rounded-[20px] text-[21px] font-bold transition active:scale-[0.98] disabled:cursor-not-allowed"
        :class="
          allAgreed
            ? 'bg-[#FFBC00] text-[#111827]'
            : 'bg-[#F0F1F3] text-[#A7ADBA]'
        "
        :disabled="!allAgreed"
        @click="goNext"
      >
        다음
      </button>
    </div>

    <!-- 약관 상세 팝업 -->
    <div
      v-if="selectedTerm"
      class="absolute inset-0 z-50 flex items-end bg-black/40"
      @click.self="closeDetail"
    >
      <section
        class="w-full rounded-t-[28px] bg-white px-6 pb-10 pt-6"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="`${selectedTerm.id}-title`"
      >
        <div class="flex items-center justify-between gap-4">
          <h2
            :id="`${selectedTerm.id}-title`"
            class="text-[22px] font-bold text-[#111827]"
          >
            {{ selectedTerm.label }}
          </h2>

          <button
            type="button"
            class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-[#F3F4F6] text-[24px] text-[#374151]"
            aria-label="약관 상세 닫기"
            @click="closeDetail"
          >
            ×
          </button>
        </div>

        <div
          class="mt-5 max-h-[300px] overflow-y-auto text-[16px] leading-relaxed text-[#4B5563]"
        >
          <p>
            {{ selectedTerm.description }}
          </p>
        </div>

        <button
          type="button"
          class="mt-7 h-[60px] w-full rounded-[18px] bg-[#FFBC00] text-[19px] font-bold text-[#111827]"
          @click="agreeSelectedTerm"
        >
          확인하고 동의하기
        </button>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import SafeArea from "../components/common/SafeArea.vue";

const router = useRouter();

const terms = ref([
  {
    id: "service",
    label: "서비스 이용약관",
    description:
      "서비스 이용약관의 실제 내용이 들어갈 영역입니다. 최종 약관이 준비되면 이 문구를 교체해 주세요.",
    agreed: false,
  },
  {
    id: "privacy",
    label: "개인정보 수집 및 이용",
    description:
      "개인정보 수집 및 이용에 관한 실제 내용이 들어갈 영역입니다. 수집 항목, 이용 목적, 보유 기간을 명확하게 작성해 주세요.",
    agreed: false,
  },
  {
    id: "uniqueIdentifier",
    label: "고유식별정보 처리",
    description:
      "고유식별정보 처리에 관한 실제 내용이 들어갈 영역입니다. 처리 목적과 보유 기간을 최종 정책에 맞게 작성해 주세요.",
    agreed: false,
  },
]);

const selectedTerm = ref(null);

const allAgreed = computed(() => {
  return terms.value.every((term) => term.agreed);
});

function toggleAll() {
  const nextValue = !allAgreed.value;

  terms.value.forEach((term) => {
    term.agreed = nextValue;
  });
}

function toggleTerm(termId) {
  const term = terms.value.find((item) => item.id === termId);

  if (term) {
    term.agreed = !term.agreed;
  }
}

function openDetail(term) {
  selectedTerm.value = term;
}

function closeDetail() {
  selectedTerm.value = null;
}

function agreeSelectedTerm() {
  if (selectedTerm.value) {
    selectedTerm.value.agreed = true;
  }

  closeDetail();
}

function goNext() {
  if (!allAgreed.value) return;

  /*
   * 로그인 완료 후 서버로 전달할 수 있도록
   * 현재 브라우저 세션에 동의 결과를 임시 저장합니다.
   */
  sessionStorage.setItem(
    "danjjakPrivacyConsent",
    JSON.stringify({
      serviceTermsAgreed: true,
      privacyCollectionAgreed: true,
      uniqueIdentifierAgreed: true,
      agreedAt: new Date().toISOString(),
    }),
  );

  router.push({ name: "login" });
}
</script>
