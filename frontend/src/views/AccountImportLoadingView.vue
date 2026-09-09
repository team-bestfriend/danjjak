<template>
  <div class="flex h-full min-h-0 flex-col bg-[#FAFAF8]">
    <main class="min-h-0 flex-1 overflow-y-auto px-6">
      <div
        class="flex min-h-full w-full flex-col items-center justify-center py-10 text-center"
        role="status"
        aria-live="polite"
      >
        <div
          class="flex h-[96px] w-[96px] shrink-0 items-center justify-center rounded-full bg-[#FFF5CF]"
          aria-hidden="true"
        >
          <img :src="bankImage" alt="" class="h-[68px] w-[68px] object-contain" />
        </div>

        <h1 class="mt-8 break-keep text-[28px] font-extrabold leading-snug tracking-[-0.03em] text-[#111827]">
          계좌를 불러오고 있어요
        </h1>
        <p class="mt-3 text-[20px] font-medium leading-relaxed text-[#6B7280]">
          잠시만 기다려 주세요...
        </p>

        <div class="mt-8 flex items-center gap-2" aria-hidden="true">
          <span v-for="dot in 3" :key="dot" class="loading-dot h-3 w-3 rounded-full bg-[#F9C13C]" />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import bankImage from "../assets/bank.png";

const router = useRouter();
let redirectTimer;

onMounted(() => {
  redirectTimer = setTimeout(() => {
    router.replace({ name: "guardian-setup" });
  }, 2000);
});

// 화면을 먼저 떠나면 예약된 이동이 다른 화면을 방해하지 않도록 취소한다.
onUnmounted(() => {
  clearTimeout(redirectTimer);
});
</script>

<style scoped>
.loading-dot {
  animation: loading-bounce 0.8s ease-in-out infinite;
}

.loading-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.loading-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes loading-bounce {
  0%, 80%, 100% {
    transform: translateY(0);
    opacity: 0.45;
  }
  40% {
    transform: translateY(-7px);
    opacity: 1;
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-dot {
    animation-name: loading-fade;
  }

  @keyframes loading-fade {
    0%, 80%, 100% { opacity: 0.45; }
    40% { opacity: 1; }
  }
}
</style>
