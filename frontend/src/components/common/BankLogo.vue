<template>
  <span
    class="inline-flex shrink-0 items-center justify-center overflow-hidden border border-[#E5E7EB] bg-white"
    :class="sizeClass"
  >
    <img
      v-if="logoSrc"
      :src="logoSrc"
      :alt="`${bankName} 로고`"
      class="block h-full w-full object-contain"
    />

    <span
      v-else
      class="flex h-full w-full items-center justify-center bg-[#F3F4F6] text-[11px] font-bold text-[#6B7280]"
      aria-hidden="true"
    >
      {{ fallbackText }}
    </span>
  </span>
</template>

<script setup>
import { computed } from "vue";

import kbBankLogo from "../../assets/kb-bank.png";
import shinhanBankLogo from "../../assets/shinhan-bank.png";
import wonBankLogo from "../../assets/won-bank.png";
import hanaBankLogo from "../../assets/hana-bank.png";

const props = defineProps({
  bankName: {
    type: String,
    default: "",
  },

  size: {
    type: String,
    default: "medium",
  },
});

const logoSrc = computed(() => {
  const normalizedName = props.bankName.replace(/\s/g, "").toLowerCase();

  if (normalizedName.includes("국민") || normalizedName.includes("kb")) {
    return kbBankLogo;
  }

  if (normalizedName.includes("신한")) {
    return shinhanBankLogo;
  }

  if (
    normalizedName.includes("우리") ||
    normalizedName.includes("woori") ||
    normalizedName.includes("won")
  ) {
    return wonBankLogo;
  }

  if (normalizedName.includes("하나") || normalizedName.includes("hana")) {
    return hanaBankLogo;
  }

  return null;
});

const fallbackText = computed(() => {
  const name = props.bankName.replace("은행", "").trim();

  return name.slice(0, 2) || "은행";
});

const sizeClass = computed(() => {
  const sizes = {
    small: "h-8 w-8 rounded-[9px]",
    medium: "h-11 w-11 rounded-[12px]",
    large: "h-14 w-14 rounded-[16px]",
  };

  return sizes[props.size] ?? sizes.medium;
});
</script>
