<template>
  <div class="flex h-full min-h-0 flex-col bg-[#FAFAF8]">
    <main class="min-h-0 flex-1 overflow-y-auto px-6">
      <div class="flex min-h-full w-full flex-col items-center justify-center py-10 text-center">
        <div
          class="flex h-[112px] w-[112px] shrink-0 items-center justify-center rounded-full border-[3px] border-[#F9C13C] bg-[#FFF5CF] shadow-md"
          aria-hidden="true"
        >
          <img :src="bankImage" alt="" class="h-[80px] w-[80px] object-contain" />
        </div>

        <h1 class="mt-10 break-keep text-[28px] font-extrabold leading-snug tracking-[-0.03em] text-[#111827]">
          계좌를 연동할게요
        </h1>

        <button
          type="button"
          class="mt-10 flex min-h-[76px] w-full items-center justify-center rounded-[20px] bg-[#F9C13C] px-4 py-4 text-[24px] font-bold text-[#111827] transition active:scale-[0.98]"
          @click="startAccountImport"
        >
          계좌 불러오기
        </button>
      </div>
    </main>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import bankImage from "../assets/bank.png";

const router = useRouter();

function startAccountImport() {
  router.push({
    name: "account-import-loading",
  });
}
</script>
