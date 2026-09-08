<template>
  <span
    class="inline-flex shrink-0 items-center justify-center overflow-hidden border border-[#E5E7EB] bg-white"
    :class="sizeClass"
  >
    <img
      v-if="logoSrc"
      :src="logoSrc"
      alt=""
      class="block h-full w-full object-contain"
      aria-hidden="true"
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
