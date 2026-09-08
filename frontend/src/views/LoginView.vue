<template>
  <div class="flex h-full flex-col bg-[#FAFAF8]">
    <SafeArea />

    <main class="flex flex-1 flex-col items-center justify-center px-6">
      <DanjjakMark :size="72" />

      <h1 class="mt-5 text-center text-[30px] font-bold text-[#111827]">
        단짝에 로그인해요
      </h1>

      <p
        class="mt-3 break-keep text-center text-[17px] leading-relaxed text-[#6B7280]"
      >
        카카오 계정으로 본인을 확인하면<br />
        준비된 시연 계정과 안전하게 연결됩니다.
      </p>

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

    <div class="px-6 pb-12">
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

      <p class="mt-4 text-center text-[14px] leading-relaxed text-[#6B7280]">
        비밀번호는 단짝에 저장하지 않으며,<br />
        카카오 인증 결과만 사용합니다.
      </p>
    </div>
  </div>
</template>

<script setup>
import kakaoLoginButton from "../assets/kakao_login_large_narrow.png";
import DanjjakMark from "../components/common/DanjjakMark.vue";
import SafeArea from "../components/common/SafeArea.vue";
import { useAppStore } from "../stores/appStore";

const store = useAppStore();
</script>
