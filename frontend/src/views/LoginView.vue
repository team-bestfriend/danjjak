<template>
  <div class="login-view flex h-full flex-col bg-[#FAFAF8] px-6">
    <main class="flex flex-col items-center pt-[32dvh]">
      <!-- 실제 단짝 PNG 로고 -->
      <img
        :src="danjjakLogo"
        alt="단짝"
        class="block h-[112px] w-[112px] object-contain"
      />

      <h1
        class="mt-6 whitespace-nowrap text-center text-[34px] font-bold leading-tight text-[#111827]"
      >
        단짝 로그인하기
      </h1>

      <div
        v-if="store.sessionNotice"
        class="mt-7 w-full rounded-[16px] bg-[#FFF7ED] p-4 text-[15px] text-[#92400E]"
        role="status"
      >
        {{ store.sessionNotice }}
      </div>

      <div
        v-if="store.authError"
        class="mt-4 w-full rounded-[16px] bg-[#FEF2F2] p-4 text-[15px] text-[#B91C1C]"
        role="alert"
      >
        {{ store.authError }}
      </div>
    </main>

    <div class="mt-auto pb-[25dvh]">
      <button
        type="button"
        class="block w-full overflow-hidden rounded-[12px] transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        :disabled="store.loginStarting || store.authLoading"
        :aria-label="
          store.loginStarting
            ? '카카오 로그인 페이지로 이동 중'
            : '카카오 로그인'
        "
        @click="store.startKakaoLogin"
      >
        <img
          :src="kakaoLoginButton"
          alt=""
          class="block h-auto w-full"
          aria-hidden="true"
        />
      </button>

      <p
        v-if="store.loginStarting"
        class="mt-3 text-center text-[15px] font-medium text-[#6B7280]"
        role="status"
      >
        카카오 로그인 페이지로 이동하고 있어요…
      </p>
    </div>
  </div>
</template>

<script setup>
import danjjakLogo from "../assets/danjjak-logo.png";
import kakaoLoginButton from "../assets/kakao_login_large_narrow.png";
import { useAppStore } from "../stores/appStore";

const store = useAppStore();
</script>

<style scoped>
.login-view {
  box-sizing: border-box;
  padding-top: env(safe-area-inset-top);
  padding-bottom: env(safe-area-inset-bottom);
}
</style>
